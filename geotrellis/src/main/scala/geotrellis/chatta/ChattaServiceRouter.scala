package geotrellis.chatta

import geotrellis.proj4.{LatLng, WebMercator}
import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.services._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.vector.io.json.Implicits._
import geotrellis.vector.Polygon
import geotrellis.vector.reproject._

import org.apache.spark.{SparkConf, SparkContext}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ChattaServiceRouter extends Directives with AkkaSystem.LoggerExecutor with LazyLogging {
  // val conf: SparkConf
  // implicit val sc: SparkContext

  val reader: CollectionLayerReader[LayerId]
  val tileReader: ValueReader[LayerId]
  val attributeStore: AttributeStore

  val staticPath: String
  val baseZoomLevel = 9

  def layerId(layer: String): LayerId = LayerId(layer, baseZoomLevel)

  def getMetaData(id: LayerId): TileLayerMetadata[SpatialKey] =
    attributeStore.readMetadata[TileLayerMetadata[SpatialKey]](id)

  def routes = get {
    pathPrefix("gt") {
      pathPrefix("tms")(tms) ~
        path("colors")(colors) ~
        path("breaks")(breaks) ~
        path("sum")(sum)
    } ~
      pathEndOrSingleSlash {
        getFromFile(staticPath + "/index.html")
      } ~
      pathPrefix("") {
        getFromDirectory(staticPath)
      }
  }

  def colors = complete(Future(ColorRampMap.getJson))

  def breaks =
    parameters(
      'layers,
      'weights,
      'numBreaks.as[Int]
    ) { (layersParam, weightsParam, numBreaks) =>
      import DefaultJsonProtocol._

      val layers = layersParam.split(",")
      val weights = weightsParam.split(",").map(_.toInt)

      complete {
        Future {
          val breaksSeq =
            timedCreate(
              "breaks",
              "ChattaServiceActor(91)::breaksSeq start",
              "ChattaServiceActor(91)::breaksSeq end") {
              layers.zip(weights)
                .map { case (layer, weight) =>
                  reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId(layer)).convert(ShortConstantNoDataCellType) * weight
                }.toSeq
            }

          val breaksAdd =
            timedCreate(
              "breaks",
              "ChattaServiceActor(102)::breaksAdd start",
              "ChattaServiceActor(102)::breaksAdd end") {
              breaksSeq.localAdd
            }

          val breaksArray =
            timedCreate(
              "breaks",
              "ChattaServiceActor(110)::breaksArray start",
              "ChattaServiceActor(110)::breaksArray end") {
              breaksAdd.histogramExactInt.quantileBreaks(numBreaks)
            }

          printBuffer("breaks")
          JsObject(
            "classBreaks" -> breaksArray.toJson
          )
        }
      }
    }

  def tms = pathPrefix(IntNumber / IntNumber / IntNumber) { (zoom, x, y) =>
    parameters(
      'layers,
      'weights,
      'breaks,
      'bbox.?,
      'colors.as[Int] ? 4,
      'colorRamp ? "blue-to-red",
      'mask ? ""
    ) { (layersParam, weightsParam, breaksString, bbox, colors, colorRamp, maskz) =>

      val layers = layersParam.split(",")
      val weights = weightsParam.split(",").map(_.toInt)
      val breaks = breaksString.split(",").map(_.toInt)
      val key = SpatialKey(x, y)

      complete {
        Future {
          val maskTile =
            timedCreate(
              "tms",
              "ChattaServiceActor(142)::maskTile start",
              "ChattaServiceActor(142)::maskTile end") {
              tileReader.reader[SpatialKey, Tile](LayerId("mask", zoom)).read(key).convert(ShortConstantNoDataCellType).mutable
            }

          val (extSeq, tileSeq) =
            timedCreate(
              "tms",
              "ChattaServiceActor(150)::(extSeq, tileSeq) start",
              "ChattaServiceActor(150)::(extSeq, tileSeq) end") {
              layers.zip(weights)
                .map { case (l, weight) =>
                  getMetaData(LayerId(l, zoom)).mapTransform(key) ->
                    tileReader.reader[SpatialKey, Tile](LayerId(l, zoom)).read(key).convert(ShortConstantNoDataCellType) * weight
                }.toSeq.unzip
            }

          val extent =
            timedCreate(
              "tms",
              "ChattaServiceActor(162)::extent start",
              "ChattaServiceActor(162)::extent end") {
              extSeq.reduce(_ combine _)
            }

          val tileAdd =
            timedCreate(
              "tms",
              "ChattaServiceActor(170)::tileAdd start",
              "ChattaServiceActor(170)::tileAdd end") {
              tileSeq.localAdd
            }

          val tileMap =
            timedCreate(
              "tms",
              "ChattaServiceActor(178)::tileMap start",
              "ChattaServiceActor(178)::tileMap end") {
              tileAdd.map(i => if (i == 0) NODATA else i)
            }

          val tile = timedCreate(
            "tms",
            "ChattaServiceActor(186)::tile start",
            "ChattaServiceActor(186)::tile end") {
            tileMap.localMask(maskTile, NODATA, NODATA)
          }

          val maskedTile =
            if (maskz.isEmpty) tile
            else {
              val poly =
                maskz
                  .parseGeoJson[Polygon]
                  .reproject(LatLng, WebMercator)

              tile.mask(extent, poly.geom)
            }

          val ramp = ColorRampMap.getOrElse(colorRamp, ColorRamps.BlueToRed).toColorMap(breaks)

          val bytes =
            timedCreate(
              "tms",
              "ChattaServiceActor(211)::result start",
              "ChattaServiceActor(211)::result end") {
              maskedTile.renderPng(ramp).bytes
            }

          printBuffer("tms")
          HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`image/png`), bytes))
        }
      }
    }
  }

  def sum =
    parameters(
      'polygon,
      'layers,
      'weights) { (polygonJson, layersString, weightsString) =>
      import DefaultJsonProtocol._

      complete {
        Future {
          val start = System.currentTimeMillis()

          val poly = {
            val parsed = polygonJson.parseGeoJson[Polygon]
            Reproject(parsed, LatLng, WebMercator)
          }
          val layers = layersString.split(",")
          val weights = weightsString.split(",").map(_.toInt)

          val summary =
            timedCreate(
              "sum",
              "ChattaServiceActor(241)::summary start",
              "ChattaServiceActor(241)::summary end") {
              Model.summary(layers, weights, baseZoomLevel, poly)(reader)
            }
          val elapsedTotal = System.currentTimeMillis - start

          val layerSummaries = summary.layerSummaries.map { ls =>
            JsObject(
              "layer" -> ls.name.toJson,
              "total" -> "%.2f".format(ls.score * 100).toJson
            )
          }

          printBuffer("sum")
          JsObject(
            "layerSummaries" -> layerSummaries.toJson,
            "total" -> "%.2f".format(summary.score * 100).toJson,
            "elapsed" -> elapsedTotal.toJson
          )
        }
      }
    }

  /*path("wo") {
   parameters('service, 'request, 'version, 'format, 'bbox, 'height.as[Int], 'width.as[Int], 'layers, 'weights,
     'palette ? "ff0000,ffff00,00ff00,0000ff", 'colors.as[Int] ? 4, 'breaks, 'colorRamp ? "blue-to-red", 'mask ? "") {
     (_, _, _, _, bbox, cols, rows, layersString, weightsString, palette, colors, breaksString, colorRamp, mask) => {
       val extent = Extent.fromString(bbox)
       val re = RasterExtent(extent, cols, rows)
       val zoomLevel = ???
       catalog.reader[SpatialKey](LayerId(layer, zoomLevel), FilterSet[
       val layers = layersString.split(",")
       val weights = weightsString.split(",").map(_.toInt)
       val model = Model.weightedOverlay(layers,weights,re)
       val overlay =
         if (mask.isEmpty) model
         else {
           GeoJsonReader.parse(mask) match {
             case Some(geomArray) if geomArray.length == 1 =>
               val transformed =
                 geomArray.head.mapGeom { g =>
                   Transformer.transform(g, Projections.LatLong, Projections.WebMercator)
                 }
               model.mask(transformed)
             case _ =>
               throw new Exception(s"Invalid GeoJSON: $mask")
           }
         }
       val breaks = breaksString.split(",").map(_.toInt)
       val ramp = {
         val cr = ColorRampMap.getOrElse(colorRamp, ColorRamps.BlueToRed)
         if (cr.toArray.length < breaks.length) cr.interpolate(breaks.length)
         else cr
       }
       val png: ValueSource[Png] = overlay.renderPng(ramp, breaks)
       png.run match {
         case process.Complete(img, h) =>
           respondWithMediaType(MediaTypes.`image/png`) {
             complete(img)
           }
         case process.Error(message,trace) =>
           println(message)
           println(trace)
           println(re)
           failWith(new RuntimeException(message))
       }
     }
   }
 } ~*/

}

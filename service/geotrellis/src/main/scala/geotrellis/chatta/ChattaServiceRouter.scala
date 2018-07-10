package geotrellis.chatta

import geotrellis.proj4.util.UTM
import geotrellis.proj4.{LatLng, WebMercator}
import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.services._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.tiling.ZoomedLayoutScheme.EARTH_CIRCUMFERENCE
import geotrellis.spark.tiling._
import geotrellis.util.Haversine
import geotrellis.vector._
import geotrellis.vector.io.json.Implicits._
import geotrellis.vector.Polygon
import geotrellis.vector.reproject._

// import org.apache.spark.{SparkConf, SparkContext}
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
        path("sum")(sum) ~
        path("wo")(wo)
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
              "ChattaServiceRouter(71)::breaksSeq start",
              "ChattaServiceRouter(71)::breaksSeq end") {
              layers.zip(weights)
                .map { case (layer, weight) =>
                  reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId(layer)).convert(ShortConstantNoDataCellType) * weight
                }.toSeq
            }

          val breaksAdd =
            timedCreate(
              "breaks",
              "ChattaServiceRouter(82)::breaksAdd start",
              "ChattaServiceRouter(82)::breaksAdd end") {
              breaksSeq.localAdd
            }

          val breaksArray =
            timedCreate(
              "breaks",
              "ChattaServiceRouter(90)::breaksArray start",
              "ChattaServiceRouter(90)::breaksArray end") {
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
              "ChattaServiceRouter(124)::maskTile start",
              "ChattaServiceRouter(124)::maskTile end") {
              tileReader.reader[SpatialKey, Tile](LayerId("mask", zoom)).read(key).convert(ShortConstantNoDataCellType).mutable
            }

          val (extSeq, tileSeq) =
            timedCreate(
              "tms",
              "ChattaServiceRouter(132)::(extSeq, tileSeq) start",
              "ChattaServiceRouter(132)::(extSeq, tileSeq) end") {
              layers.zip(weights)
                .map { case (l, weight) =>
                  getMetaData(LayerId(l, zoom)).mapTransform(key) ->
                    tileReader.reader[SpatialKey, Tile](LayerId(l, zoom)).read(key).convert(ShortConstantNoDataCellType) * weight
                }.toSeq.unzip
            }

          val extent =
            timedCreate(
              "tms",
              "ChattaServiceRouter(144)::extent start",
              "ChattaServiceRouter(145)::extent end") {
              extSeq.reduce(_ combine _)
            }

          val tileAdd =
            timedCreate(
              "tms",
              "ChattaServiceRouter(152)::tileAdd start",
              "ChattaServiceRouter(152)::tileAdd end") {
              tileSeq.localAdd
            }

          val tileMap =
            timedCreate(
              "tms",
              "ChattaServiceRouter(160)::tileMap start",
              "ChattaServiceRouter(160)::tileMap end") {
              tileAdd.map(i => if (i == 0) NODATA else i)
            }

          val tile = timedCreate(
            "tms",
            "ChattaServiceRouter(167)::tile start",
            "ChattaServiceRouter(167)::tile end") {
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
              "ChattaServiceRouter(188)::result start",
              "ChattaServiceRouter(188)::result end") {
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
              "ChattaServiceRouter(221)::summary start",
              "ChattaServiceRouter(221)::summary end") {
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

  def wo =
    parameters('service, 'request, 'version, 'format, 'bbox, 'height.as[Int], 'width.as[Int], 'layers, 'weights,
     'palette ? "ff0000,ffff00,00ff00,0000ff", 'colors.as[Int] ? 4, 'breaks, 'colorRamp ? "blue-to-red", 'mask ? "") {
      (_, _, _, _, bbox, cols, rows, layersParam, weightsParam, palette, colors, breaksString, colorRamp, maskz) => {
        val extent = Extent.fromString(bbox)
        val re = RasterExtent(extent, cols, rows)
        val scheme = ZoomedLayoutScheme(crs = WebMercator, tileSize = math.max(cols, rows), resolutionThreshold = 0.5)

        // wondering why there is a need to zoom out (?)
        // val LayoutLevel(zoom, layout) = scheme.zoomOut(scheme.levelFor(re.extent, re.cellSize))
        val LayoutLevel(zoom, layout) = scheme.levelFor(re.extent, re.cellSize)
        val layers = layersParam.split(",")
        val weights = weightsParam.split(",").map(_.toInt)
        val breaks = breaksString.split(",").map(_.toInt)
        val key = layout.mapTransform(extent.center)

        complete {
          Future {
            val maskTile =
              timedCreate(
                "tms",
                "ChattaServiceRouter(263)::maskTile start",
                "ChattaServiceRouter(264)::maskTile end") {
                tileReader.reader[SpatialKey, Tile](LayerId("mask", zoom)).read(key).convert(ShortConstantNoDataCellType).mutable
              }

            val (extSeq, tileSeq) =
              timedCreate(
                "tms",
                "ChattaServiceRouter(271)::(extSeq, tileSeq) start",
                "ChattaServiceRouter(272)::(extSeq, tileSeq) end") {
                layers.zip(weights)
                  .map { case (l, weight) =>
                    getMetaData(LayerId(l, zoom)).mapTransform(key) ->
                      tileReader.reader[SpatialKey, Tile](LayerId(l, zoom)).read(key).convert(ShortConstantNoDataCellType) * weight
                  }.toSeq.unzip
              }

            val extent =
              timedCreate(
                "tms",
                "ChattaServiceRouter(283)::extent start",
                "ChattaServiceRouter(284)::extent end") {
                extSeq.reduce(_ combine _)
              }

            val tileAdd =
              timedCreate(
                "tms",
                "ChattaServiceRouter(291)::tileAdd start",
                "ChattaServiceRouter(292)::tileAdd end") {
                tileSeq.localAdd
              }

            val tileMap =
              timedCreate(
                "tms",
                "ChattaServiceRouter(299)::tileMap start",
                "ChattaServiceRouter(300)::tileMap end") {
                tileAdd.map(i => if (i == 0) NODATA else i)
              }

            val tile = timedCreate(
              "tms",
              "ChattaServiceRouter(306)::tile start",
              "ChattaServiceRouter(307)::tile end") {
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
                "ChattaServiceRouter(327)::result start",
                "ChattaServiceRouter(328)::result end") {
                maskedTile.renderPng(ramp).bytes
              }

            printBuffer("wo")
            HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`image/png`), bytes))
          }
        }
      }
    }
}

package geotrellis.chatta

import geotrellis.proj4.{LatLng, WebMercator}
import geotrellis.raster.{TypeInt, Tile}
import geotrellis.raster.op.local._
import geotrellis.services._
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.op.local._
import geotrellis.spark.io.json._
import geotrellis.spark.op.stats._
import geotrellis.vector.io.json._
import geotrellis.vector.reproject._
import geotrellis.vector.{Extent, Polygon}
import geotrellis.spark.io.avro.codecs._
import geotrellis.raster.render._

import akka.actor._
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import com.typesafe.config.Config
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.routing._

class ChattaServiceActor(override val staticPath: String, config: Config) extends Actor with ChattaService {

  override def actorRefFactory = context
  override def receive = runRoute(serviceRoute)

  override val accumulo = AccumuloInstance(
    config.getString("accumulo.instance"),
    config.getString("zookeeper.address"),
    config.getString("accumulo.user"),
    new PasswordToken(config.getString("accumulo.password"))
  )
}

trait ChattaService extends HttpService {

  implicit val sparkContext = SparkUtils.createLocalSparkContext("local[*]", "ChattaDemo")
  implicit val executionContext = actorRefFactory.dispatcher
  val accumulo: AccumuloInstance
  lazy val reader = AccumuloLayerReader[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]](accumulo)
  lazy val tileReader = AccumuloTileReader[SpatialKey, Tile](accumulo)
  lazy val attributeStore = AccumuloAttributeStore(accumulo.connector)

  val staticPath: String
  val baseZoomLevel = 9

  def layerId(layer: String): LayerId =
    LayerId(layer, baseZoomLevel)

  def getMetaData(id: LayerId): RasterMetaData = {
    import DefaultJsonProtocol._
    attributeStore.readLayerAttributes[
      Unit, RasterMetaData, Unit, Unit, Unit](id)._2
  }

  def serviceRoute = get {
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

  def colors = complete(ColorRampMap.getJson)

  def breaks =
    parameters(
      'layers,
      'weights,
      'numBreaks.as[Int]
    ) { (layersParam, weightsParam, numBreaks) =>
      import DefaultJsonProtocol._

      val layers = layersParam.split(",")
      val weights = weightsParam.split(",").map(_.toInt)

      val breaksArray =
        layers.zip(weights)
          .map { case (layer, weight) =>
            reader.read(layerId(layer)) * weight
          }
          .toSeq
          .localAdd
          .classBreaks(numBreaks)

      complete(JsObject(
        "classBreaks" -> breaksArray.toJson
      ))
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
    ) { (layersParam, weightsParam, breaksString, bbox, colors, colorRamp, mask) =>

      import geotrellis.raster._

      val layers = layersParam.split(",")
      val weights = weightsParam.split(",").map(_.toInt)
      val breaks = breaksString.split(",").map(_.toInt)
      val key = SpatialKey(x, y)

      val (extSeq, tileSeq) =
        layers.zip(weights)
          .map { case (l, weight) =>
            getMetaData(LayerId(l, zoom)).mapTransform(key).extent ->
              tileReader.read(LayerId(l, zoom)).read(key) * weight
          }.toSeq.unzip

      val tile   = tileSeq.localAdd().convert(TypeInt).map(i => if(i == 0) Int.MinValue else i)
      val extent = extSeq.reduce(_ combine _)

      val maskedTile =
        if (mask.isEmpty) tile
        else {
          val poly =
            mask
              .parseGeoJson[Polygon]
              .reproject(LatLng, WebMercator)

          tile.mask(extent, poly.geom)
        }

      val ramp = {
        val cr = ColorRampMap.getOrElse(colorRamp, ColorRamps.BlueToRed)
        if (cr.toArray.length < breaks.length) cr.interpolate(breaks.length)
        else cr
      }

      respondWithMediaType(MediaTypes.`image/png`) {
        complete(maskedTile.renderPng(ramp, breaks).bytes)
      }
    }
  }

  def sum =
    parameters(
      'polygon,
      'layers,
      'weights) { (polygonJson, layersString, weightsString) =>
      import DefaultJsonProtocol._

      val start = System.currentTimeMillis()

      val poly = {
        val parsed = polygonJson.parseGeoJson[Polygon]
        Reproject(parsed, LatLng, WebMercator)
      }
      val layers = layersString.split(",")
      val weights = weightsString.split(",").map(_.toInt)

      println(s"weigths: ${weights.toList}")

      println(s"poly: ${poly}")

      val summary = ModelSpark.summary(layers, weights, baseZoomLevel, poly)(reader)
      val elapsedTotal = System.currentTimeMillis - start

      val layerSummaries = summary.layerSummaries.map { ls =>
        JsObject(
          "layer" -> ls.name.toJson,
          "total" -> "%.2f".format(ls.score * 100).toJson
        )
      }

      complete(JsObject(
        "layerSummaries" -> layerSummaries.toJson,
        "total"          -> "%.2f".format(summary.score * 100).toJson,
        "elapsed"        -> elapsedTotal.toJson
      ))
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

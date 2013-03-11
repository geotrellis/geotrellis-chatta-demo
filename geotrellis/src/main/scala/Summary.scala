package chatta

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, POST, Path, Consumes, DefaultValue, QueryParam}
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context, MediaType, MultivaluedMap}
import geotrellis._
import geotrellis.feature.Polygon
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.feature.op.geometry.AsPolygonSet
import geotrellis.rest.op._

import scala.collection.JavaConversions._

/**
 * Sum values under polygon provided via geojson
 */
@Path("/gt/sum")
class Sum {
  @POST
  def sumPost(
    polygonJson : String,
    @DefaultValue("ForestedLands") @QueryParam("layers") layers:String,
    @DefaultValue("1") @QueryParam("weights") weights:String,
    @DefaultValue("") @QueryParam("mask") mask:String,
    @Context req:HttpServletRequest
  ) = {
    if (polygonJson == null) {
      Response.serverError()
              .entity("{ \"error\" => 'GeoTrellis has received an empty request.' }")
              .`type`("application/json")
              .build() 
    } else {
      println("received json: " + polygonJson)
      println("received POST request.")
      sumGet(polygonJson, layers, weights, mask, req)
    }
  }

  @GET
  def sumGet(
    @QueryParam("polygon")
    polygonJson:String,
    @DefaultValue("ForestedLands") @QueryParam("layers") layers:String,
    @DefaultValue("1") @QueryParam("weights") weights:String,
    @DefaultValue("") @QueryParam("mask") mask:String,
    @Context req:HttpServletRequest
  ):Any = {
    val start = System.currentTimeMillis()
    val polygon = io.LoadGeoJsonFeature(polygonJson)

    val reOp = Main.getRasterExtent(polygon)

    val layerOps = string.SplitOnComma(layers)
    val weightOps = 
      logic.ForEach(string.SplitOnComma(weights))(string.ParseInt(_))

    val overlayOp = Model(layerOps,weightOps,reOp
                          ,"albers")

    val reprojected = polygon.map {
      p =>
        Transformer.transform(p,Projections.LatLong,Projections.ChattaAlbers)
    }
    val p = AsPolygon(reprojected)
    val summary = zonal.summary.Sum(overlayOp,p,Map[RasterExtent,Long]())

    Main.server.getResult(summary) match {
      case process.Complete(result,h) =>
        val elapsedTotal = System.currentTimeMillis - start
        val data = "{ \"sum\": %d, \"elapsed\": %d }".format(result,elapsedTotal)
        Response.ok(data).`type`("application/json").build()
      case process.Error(message,trace) =>
        Response.serverError()
                .entity(message + " " + trace)
                .`type`("text/plain")
                .build()
    }
  }
}

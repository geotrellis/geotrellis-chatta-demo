package chatta

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.{GET, POST, Path, Consumes, DefaultValue, QueryParam}
import javax.ws.rs._
import geotrellis._
import javax.ws.rs.core.Context
import geotrellis.rest._
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
@Path("/sum")
class Sum {
  @POST
  def sumPost(
    polygonJson : String,
    @DefaultValue("ForestedLands") @QueryParam("layers") layers:String,
    @DefaultValue("1") @QueryParam("weights") weights:String,
    @DefaultValue("") @QueryParam("mask") mask:String,
    @Context req:HttpServletRequest
  ):core.Response = {
    if (polygonJson == null) {
      ERROR("{ \"error\" => 'GeoTrellis has received an empty request.' }")
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
  ):core.Response = {
    val start = System.currentTimeMillis()
    val polyOp = io.LoadGeoJsonFeature(polygonJson)
    val feature = Main.server.run(polyOp)
    val reproj = Transformer.transform(feature,Projections.LatLong,Projections.ChattaAlbers)
    val polygon = Polygon(reproj.geom,0)

    val reOp = Main.getRasterExtent(polygon)

    val layerOps = string.SplitOnComma(layers)
    val weightOps = 
      logic.ForEach(string.SplitOnComma(weights))(string.ParseInt(_))

    val summary = Model.summary(layerOps,weightOps,polygon)

    Main.server.getResult(summary) match {
      case process.Complete(result,h) =>
        val elapsedTotal = System.currentTimeMillis - start

        val layerSummaries = 
          "[" + result.layerSummaries.map {
            ls => 
              val v = "%.2f".format(ls.score * 100)
              s"""{ "layer": "${ls.name}", "total": "${v}" }"""
          }.mkString(",") + "]"

        val totalVal = "%.2f".format(result.score * 100)
        val data = s"""{ 
          "layerSummaries": $layerSummaries,
          "total": "${totalVal}", 
          "elapsed": "$elapsedTotal"
        }"""
        Response.ok(ResponseType.Json).data(data).allowCORS()
      case process.Error(message,trace) =>
        ERROR(message + " " + trace)
    }
  }
}

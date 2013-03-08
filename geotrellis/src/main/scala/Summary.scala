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
import geotrellis.raster._
import geotrellis.feature.op.geometry.AsPolygonSet

import scala.collection.JavaConversions._

/**
 * Sum values under polygon provided via geojson
 */
@Path("/gt/sum")
class Sum {
  @POST
  def sumPost(
    polygonJson : String
  ) = {
    if (polygonJson == null) {
      Response.serverError()
              .entity("{ \"error\" => 'GeoTrellis has received an empty request.' }")
              .`type`("application/json")
              .build() 
    } else {
      println("received json: " + polygonJson)
      println("received POST request.")
      sumGet(polygonJson, "true")
    }
  }

  @GET
  def sumGet(
    @QueryParam("polygon")
    polygonJson:String,
    
    @DefaultValue("true")
    @QueryParam("cached")
    cached:String

  ):Any = {
    val start = System.currentTimeMillis()
    val server = Main.server

//    val raster = if (cached == "true") Main.tiled_raster else Main.uncachedRaster

//    var preCount = System.currentTimeMillis

    try {
      val area = io.LoadGeoJson.parse(polygonJson)
                 .get
                 .map( geo => geo.geom.getArea )
                 .foldLeft( 0.0 ) ( (s, d) => s + d)
      // val polygonSetOp = logic.ForEach(AsPolygonSet(featureOp))
      // val plist = Main.server.run(polygonSetOp)
      // val count = plist.foldLeft( 0L ) ( (sum:Long, p) => sum + zonalSum(p, raster))

       val elapsedTotal = System.currentTimeMillis - start
      // println ("Request duration: " + elapsedTotal)

      val data = "{ \"area\": %f, \"elapsed\": %d }".format(area,elapsedTotal)
      Response.ok(data).`type`("application/json").build()
    } catch {
      case e: Exception => { 
        Response.serverError()
                .entity("{ \"error\" => 'Polygon request was invalid.' }")
                .`type`("application/json")
                .build()
      }
    } 
  }

  // def zonalSum(p:Polygon[_], raster:Raster) = {
  //   val sumOp = zonal.summary.Sum(raster, p, Main.tileSums) 
  //   Main.server.run(sumOp)
  // }
}

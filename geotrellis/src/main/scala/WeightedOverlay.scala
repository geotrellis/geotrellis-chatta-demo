package chatta

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context, MediaType, MultivaluedMap}
import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.rest.op._
import geotrellis.raster._
import geotrellis.feature._
import geotrellis.feature.op.geometry.AsPolygonSet
import geotrellis.feature.rasterize.{Rasterizer, Callback}
import geotrellis.data.ColorRamps._

import scala.collection.JavaConversions._

/**
 * Create a weighted overlay of the Chattanooga model.
 */
@Path("/gt/wo")
class WeightedOverlay {
  final val defaultBox = "-9634947.090,4030964.877,-9359277.090,4300664.877"
  final val defaultColors = "ff0000,ffff00,00ff00,0000ff"

  @GET
  def get(
    @DefaultValue(defaultBox) @QueryParam("bbox") bbox:String,
    @DefaultValue("256") @QueryParam("cols") cols:String,
    @DefaultValue("256") @QueryParam("rows") rows:String,
    @DefaultValue("wm_ForestedLands") @QueryParam("layers") layers:String,
    @DefaultValue("1") @QueryParam("weights") weights:String,
    @DefaultValue("") @QueryParam("mask") mask:String,
    @DefaultValue(defaultColors) @QueryParam("palette") palette:String,
    @DefaultValue("4") @QueryParam("colors") numColors:String,
    @DefaultValue("image/png") @QueryParam("format") format:String,
    @DefaultValue("") @QueryParam("breaks") breaks:String,
    @Context req:HttpServletRequest
  ) = {
    val extentOp = string.ParseExtent(bbox)

    val colsOp = string.ParseInt(cols)
    val rowsOp = string.ParseInt(rows)

    val reOp = extent.GetRasterExtent(extentOp, colsOp, rowsOp)

    val layerOps = string.SplitOnComma(layers)
    val weightOps = 
      logic.ForEach(string.SplitOnComma(weights))(string.ParseInt(_))

    val modelOp = Model(layerOps,weightOps,reOp)

    val overlayOp = if(mask == "") { 
      modelOp
    } else {
      val polyOp = io.LoadGeoJsonFeature(mask)
      val feature = Main.server.run(polyOp)
      val re = Main.server.run(reOp)
      println(s"$re")
      val reproj = Transformer.transform(feature,Projections.LatLong,Projections.WebMercator)
      val polygon = Polygon(reproj.geom,0)
      println(s"${polygon.geom}")
      val maskRaster = Rasterizer.rasterizeWithValue(polygon,re) { x => 1 }
      local.Mask(modelOp,maskRaster,NODATA,-1000)
    }
 
    val breaksOp = 
      logic.ForEach(string.SplitOnComma(breaks))(string.ParseInt(_))
    
    val png = Render.operation(overlayOp,BlueToRed,breaksOp)
    

    Main.server.getResult(png) match {
      case process.Complete(img,h) =>
        format match {
          case "info" => 
            val histo = Main.server.run(stat.GetHistogram(overlayOp))
            val t = Main.server.run(overlayOp).data.getType.toString
            val ms = h.elapsedTime
            val query = req.getQueryString
            val url = "/gt/wo?format=image/png&" + query
            val html = InfoPage.infoPage(cols,rows,ms,url,
s"""
<p>$h</p>
<p>${histo.toJSON}</p>
<p>${t}</p>
""")
            Response.ok(html)

                    .build()
          case _ => 
            Response.ok(img)
                    .`type`("image/png")
                    .build()        
        }
      case process.Error(message,trace) =>
        Response.serverError()
                .entity(message + " " + trace)
                .`type`("text/plain")
                .build()
    }
  }
}















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
  //final val defaultBox = "-9470853.552646479,4128822.5198520804,-9451285.673405472,4148390.3990930878"
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
    @Context req:HttpServletRequest
  ) = {
    val colsOp = string.ParseInt(cols)
    val rowsOp = string.ParseInt(rows)
    val extentOp = string.ParseExtent(bbox)
    val reOp = extent.GetRasterExtent(extentOp, colsOp, rowsOp)

    val layerOps = 
      logic.ForEach(string.SplitOnComma(layers))(io.LoadRaster(_, reOp))
    val weightOps = 
      logic.ForEach(string.SplitOnComma(weights))(string.ParseInt(_))

    // Do the actual weighted overlay operation
    val overlayOp = Model.run(reOp,"wm")
    //val overlayOp = WeightedOverlayArray(layerOps, weightOps)

    val png = io.SimpleRenderPng(overlayOp, BlueToRed)

    Main.server.getResult(png) match {
      case process.Complete(img,h) =>
        format match {
          case "info" => 
            val histo = Main.server.run(stat.GetHistogram(overlayOp))
            val t = Main.server.run(overlayOp).data.getType.toString
            val ms = h.elapsedTime
            val query = req.getQueryString
            val url = "/gt/wo?format=image/png&" + query
            println(url)
            val html = InfoPage.infoPage(cols,rows,ms,url,
s"""
<p>$h</p>
<p>${histo.toJSON}</p>
<p>${t}</p>
""")
            Response.ok(html)
                    .`type`("text/html")
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

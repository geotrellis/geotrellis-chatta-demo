package chatta

import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.rest._
import geotrellis.rest.op._
import geotrellis.raster._
import geotrellis.feature.op.geometry.AsPolygonSet
import geotrellis.feature.rasterize.{Rasterizer, Callback}
import geotrellis.data.ColorRamp

import javax.ws.rs.core.Context

import scala.collection.JavaConversions._
import scala.language.implicitConversions

case class ClassBreaksToJson(b:Op[Array[Int]]) extends Op1(b)({
  breaks => 
    val breaksArray = breaks.mkString("[", ",", "]")
    Result(s"""{ "classBreaks" : $breaksArray }""")
})

@Path("/breaks") 
class GetBreaks {
  final val defaultBox = "-9634947.090,4030964.877,-9359277.090,4300664.877"

  @GET
  def get(
    @DefaultValue(defaultBox) @QueryParam("bbox") bbox:String,
    @DefaultValue("256") @QueryParam("cols") cols:String,
    @DefaultValue("256") @QueryParam("rows") rows:String,
    @DefaultValue("wm_ForestedLands") @QueryParam("layers") layers:String,
    @DefaultValue("1") @QueryParam("weights") weights:String,
    @DefaultValue("") @QueryParam("mask") mask:String,
    @DefaultValue("10") @QueryParam("numBreaks") numBreaks:String,
    @Context req:HttpServletRequest
  ):core.Response = {
    println(s"$weights\n$layers")
    val extentOp = string.ParseExtent(bbox)
    
    val colsOp = string.ParseInt(cols)
    val rowsOp = string.ParseInt(rows)

    val reOp = extent.GetRasterExtent(extentOp, colsOp, rowsOp)

    val layerOps = string.SplitOnComma(layers)
    val weightOps = 
      logic.ForEach(string.SplitOnComma(weights))(string.ParseInt(_))

    val overlay = Model(layerOps,weightOps,reOp)

    val numBreaksOp = string.ParseInt(numBreaks)
    val histo = stat.GetHistogram(overlay)
    val classBreaks = stat.GetClassBreaks(histo, numBreaksOp)
    val op = ClassBreaksToJson(classBreaks)
    
    Main.server.getResult(op) match {
      case process.Complete(json,h) =>
        OK.json(json)
          .allowCORS()
      case process.Error(message,trace) =>
        ERROR(message + " " + trace)
    }
  }
}

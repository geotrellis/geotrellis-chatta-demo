package chatta

import geotrellis._
import geotrellis.raster.op._
import geotrellis.feature._
import geotrellis.Implicits._

case class LayerSummary(name:String,total:Int)
case class SummaryResult(layerSummaries:List[LayerSummary],total:Int)

object Model {
  def applyDefault(rasterExtent:Op[RasterExtent]) = {
    val ops = for((name,weight) <- Main.weights) yield {
      val rast = io.LoadRaster(s"wm_${name}",rasterExtent)
      val converted = Force(rast.map { r => r.convert(TypeByte) })
      local.Multiply(weight, converted)
    }
    local.IfCell(local.Add(ops.toSeq:_*), (x:Int) => x == 0, NODATA)
  }

  def apply(layers:Op[Array[String]], weights:Op[Array[Int]],rasterExtent:Op[RasterExtent], prefix:String = "wm") = {
    var weighted = logic.ForEach(layers,weights) {
      (layer,weight) =>
        val r = io.LoadRaster(s"${prefix}_${layer}",rasterExtent).map { r => r.convert(TypeByte) }
        val fR = Force(r)
        local.Multiply(weight,fR)
    }
    val mask = io.LoadRaster("mask",rasterExtent)
    local.Mask(local.AddArray(weighted),mask,NODATA,NODATA)
  }
  
  private def makeSummary(layer:String,polygon:Op[Polygon[Int]]) = {
    val tileLayer = Main.getTileLayer(layer)
    zonal.summary.Sum(tileLayer.raster,polygon,tileLayer.tileSums)
                 .map { l => l.toInt }
  }

  def summary(layers:Op[Array[String]], weights:Op[Array[Int]], polygon:Op[Polygon[Int]]) = {
    val sums = logic.ForEach(layers,weights) {
      (layer,weight) =>
        val tileLayer = Main.getTileLayer(layer)
        zonal.summary.Sum(tileLayer.raster,polygon,tileLayer.tileSums)
                     .map { l => l.toInt }
        local.Multiply(weight,makeSummary(layer,polygon)).map { total =>
          LayerSummary(layer,total)
        }
    }
    sums.map {
      s => s.foldLeft(SummaryResult(List[LayerSummary](),0)) {
        (result,layerSummary) => 
          SummaryResult((layerSummary :: result.layerSummaries).reverse, 
                        result.total + layerSummary.total )
      }
    }
  }
}

object WeightedOverlayArray {
  def apply(rasters:Op[Array[Raster]], weights:Op[Array[Int]]) = {

    val rs:Op[Array[Raster]] = logic.ForEach(rasters, weights)(_ * _)

    val weightSum:Op[Int] = logic.Do(weights)(_.sum)

    local.AddArray(rs) / weightSum
  }
}

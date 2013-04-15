package chatta

import geotrellis._
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.raster.op.zonal.summary._
import geotrellis.feature._
import geotrellis.feature.rasterize.Callback
import geotrellis.Implicits._

case class LayerSummary(name:String,score:Double)
case class SummaryResult(layerSummaries:List[LayerSummary],score:Double)

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
    RatioOfOnes(tileLayer.raster,polygon,tileLayer.tileRatios)
  }

  def summary(layers:Op[Array[String]], weights:Op[Array[Int]], polygon:Op[Polygon[Int]]) = {
    val sums = logic.ForEach(layers,weights) {
      (layer,weight) =>
        val tileLayer = Main.getTileLayer(layer)
        local.Multiply(weight.toDouble, makeSummary(layer,polygon)).map { score =>
          LayerSummary(layer,score)
        }
    }

    sums.map {
      s => s.foldLeft(SummaryResult(List[LayerSummary](),0.0)) {
        (result,layerSummary) => 
          SummaryResult(
            (layerSummary :: result.layerSummaries), 
            result.score + layerSummary.score
            )
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

object RatioOfOnes {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    val tiles = trd.getTiles(re)
    tiles map { r => (r.rasterExtent, rasterResult(r))} toMap
  }

  def rasterResult (r:Raster):LayerRatio = {
    var sum = 0
    var count = 0
    r.foreach { x => 
      if (x != NODATA) { sum = sum + x }
      count += 1
    }
    LayerRatio(sum,count)
  }
}

case class LayerRatio(sum:Int,count:Int) {
  def value = { sum / count.toDouble }
  def combine(other:LayerRatio) = { 
    LayerRatio(sum + other.sum, count + other.count)
  }
}

case class RatioOfOnes[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,LayerRatio]) 
  (implicit val mB: Manifest[LayerRatio], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Double] {

  type B = LayerRatio
  type D = DD
  
  def handlePartialTileIntersection(rOp: Op[Raster], gOp: Op[Geometry[D]]) = 
    rOp.flatMap ( r => gOp.flatMap ( g => {
      var sum: Int = 0
      var total: Int = 0
      val f = new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            total += 1
            val z = r.get(col,row)
            if (z != NODATA) { sum = sum + z }
          }
        }

      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent)(f)
      LayerRatio(sum, total)
    }))

  def handleFullTile(rOp:Op[Raster]) = 
    rOp.map (r =>
      tileResults.get(r.rasterExtent).getOrElse({
        var s = 0
        r.force.foreach((x:Int) => if (s != NODATA) s = s + x)
        LayerRatio(s, r.cols * r.rows)
    }))

  def handleNoDataTile = LayerRatio(0,0) // Should not be any NODATA constant tiles.

  def reducer(mapResults: List[LayerRatio]):Double = {
    mapResults.foldLeft(LayerRatio(0,0)) { (l1,l2) =>
      l1.combine(l2)
    }.value
  }
}

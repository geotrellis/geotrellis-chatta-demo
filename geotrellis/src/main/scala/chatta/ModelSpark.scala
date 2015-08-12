package chatta

import geotrellis._
import geotrellis.engine.RasterSource
import geotrellis.raster._
import geotrellis.spark.op._
import geotrellis.spark.op.local._
import geotrellis.spark.op.local.spatial._
import geotrellis.spark.io.accumulo.AccumuloRasterCatalog
import geotrellis.spark.{LayerId, Intersects, SpatialKey, RasterRDD}
import geotrellis.vector._
import geotrellis.raster.rasterize.{Callback, Rasterizer}
import geotrellis.raster.op.zonal.summary._

case class LayerSummary(name: String, score: Double)
case class SummaryResult(layerSummaries: List[LayerSummary], score: Double)

case class LayerRatio(sum: Int, count: Int) {
  def value = sum / count.toDouble
  def combine(other: LayerRatio) =
    LayerRatio(sum + other.sum, count + other.count)
}

object LayerRatio {
  def rasterResult(r: RasterRDD[SpatialKey]): LayerRatio = {
    val mapTransform = r.metaData.mapTransform
    val rasterExtent = r.metaData.rasterExtent
    val sum =
      r.map { case (k, tile) =>
        val extent = mapTransform(k)
        tile.zonalSumDouble(extent, extent.toPolygon())
      }
      .sum()
      .toInt
    LayerRatio(sum, rasterExtent.rows * rasterExtent.cols)
  }
}

object ModelSpark {

  def weightedOverlay(layers: Iterable[String], weights: Iterable[Int], zoom: Int, rasterExtent: RasterExtent)
                     (implicit catalog: AccumuloRasterCatalog): RasterRDD[SpatialKey] = {

    val layerIds = layers.map(l => LayerId(s"albers_$l", zoom))
    val maskId = LayerId("mask", zoom)
    val bounds = rasterExtent.gridBoundsFor(rasterExtent.extent)

    val mask = catalog.query[SpatialKey](maskId).where(Intersects(bounds)).toRDD
    val weighted =
      layerIds.zip(weights)
      .map { case (layer, weight) =>
        catalog.query[SpatialKey](layer).where(Intersects(bounds)).toRDD * weight
      }
      .toSeq
      .localAdd

    weighted.localMask(mask, NODATA, NODATA)
  }

  def summary(layers: Iterable[String], weights: Iterable[Int], zoom: Int, polygon: Polygon)
             (implicit catalog: AccumuloRasterCatalog): SummaryResult = {

    val layerIds = layers.map(l => LayerId(s"albers_$l", zoom))

    val layerRatios =
      layerIds.zip(weights)
      .map { case (layer, weight) =>

        //val raster = catalog.query[SpatialKey](layer).where(Intersects(bounds)).toRDD * weight
        val raster = catalog.read[SpatialKey](layer) * weight
        val masked = raster.mask(polygon)
        val ratio = LayerRatio.rasterResult(masked)

        LayerSummary(layer.name, ratio.value * weight)
      }

    layerRatios.foldLeft(SummaryResult(List[LayerSummary](), 0.0)) { (result, layerSummary) =>
        SummaryResult(layerSummary :: result.layerSummaries, result.score + layerSummary.score)
    }
  }
}


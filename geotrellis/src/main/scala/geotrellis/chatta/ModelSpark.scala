package geotrellis.chatta

import geotrellis.raster._
import geotrellis.spark.io.{RDDQuery, Intersects}
import geotrellis.spark.io.accumulo.AccumuloLayerReader
import geotrellis.spark.op.local.spatial._
import geotrellis.spark._
import geotrellis.vector._
import geotrellis.raster.op.zonal.summary._
import geotrellis.spark.op.local._

case class LayerSummary(name: String, score: Double)
case class SummaryResult(layerSummaries: List[LayerSummary], score: Double)

case class LayerRatio(sum: Long, count: Long) {
  def value = sum / count.toDouble
  def combine(other: LayerRatio) =
    LayerRatio(sum + other.sum, count + other.count)
}

object LayerRatio {
  def rasterResult(r: RasterRDD[SpatialKey]): LayerRatio = {
    val mapTransform = r.metaData.mapTransform
    val (sum, count) =
      r map { case (k, tile) =>
        val extent = mapTransform(k)
        (tile.zonalSum(extent, extent.toPolygon()), tile.size)
      } reduce { case ((sl, cl), (sr, cr)) => (sl + sr, cl + cr) }

    LayerRatio(sum, count)
  }
}

object ModelSpark {

  def weightedOverlay(layers: Iterable[String], weights: Iterable[Int], zoom: Int, rasterExtent: RasterExtent)
                     (reader: AccumuloLayerReader[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]]): RasterRDD[SpatialKey] = {

    val layerIds = layers.map(LayerId(_, zoom))
    val maskId = LayerId("mask", zoom)
    val bounds = rasterExtent.gridBoundsFor(rasterExtent.extent)

    val mask = reader.query(maskId).where(Intersects(bounds)).toRDD
    val weighted =
      layerIds.zip(weights)
      .map { case (layer, weight) =>
        reader.read(layer, new RDDQuery[SpatialKey, RasterMetaData].where(Intersects(bounds))) * weight
      }
      .toSeq
      .localAdd

    weighted.localMask(mask, NODATA, NODATA)
  }

  def summary(layers: Iterable[String], weights: Iterable[Int], zoom: Int, polygon: Polygon)
             (reader: AccumuloLayerReader[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]]): SummaryResult = {

    val layerIds = layers.map(LayerId(_, zoom))
    val layerRatios =
      layerIds.zip(weights)
      .map { case (layer, weight) =>
        val raster = reader.read(layer)
        val masked = raster.mask(polygon)
        val ratio = LayerRatio.rasterResult(masked)

        LayerSummary(layer.name, ratio.value * weight)
      }

    layerRatios.foldLeft(SummaryResult(List[LayerSummary](), 0.0)) { (result, layerSummary) =>
        SummaryResult(layerSummary :: result.layerSummaries, result.score + layerSummary.score)
    }
  }
}

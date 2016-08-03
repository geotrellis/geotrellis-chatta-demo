package geotrellis.chatta

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.vector._

import org.apache.spark.rdd.RDD

case class LayerSummary(name: String, score: Double)
case class SummaryResult(layerSummaries: List[LayerSummary], score: Double)

case class LayerRatio(sum: Long, count: Long) {
  def value = sum / count.toDouble
  def combine(other: LayerRatio) =
    LayerRatio(sum + other.sum, count + other.count)
}

object LayerRatio {
  def rasterResult(r: TileLayerRDD[SpatialKey]): LayerRatio = {
    val mapTransform = r.metadata.mapTransform
    val (sum, count) =
      r map { case (k, tile) =>
        val extent = mapTransform(k)
        (tile.polygonalSum(extent, extent.toPolygon()), tile.size)
      } reduce { case ((sl, cl), (sr, cr)) => (sl + sr, cl + cr) }

    LayerRatio(sum, count)
  }
}

object ModelSpark {

  def weightedOverlay(layers: Iterable[String], weights: Iterable[Int], zoom: Int, rasterExtent: RasterExtent)
                     (reader: FilteringLayerReader[LayerId]): RDD[(SpatialKey, Tile)] = {

    val layerIds = layers.map(LayerId(_, zoom))
    val maskId = LayerId("mask", zoom)
    val bounds = rasterExtent.gridBoundsFor(rasterExtent.extent)

    val mask = reader.query[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](maskId).where(Intersects(bounds)).result
    val weighted =
      layerIds.zip(weights)
      .map { case (layer, weight) =>
        reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layer, new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(bounds))) * weight
      }
      .toSeq
      .localAdd

    weighted.localMask(mask, NODATA, NODATA)
  }

  def summary(layers: Iterable[String], weights: Iterable[Int], zoom: Int, polygon: Polygon)
             (reader: FilteringLayerReader[LayerId]): SummaryResult = {

    val layerIds = layers.map(LayerId(_, zoom))
    val layerRatios =
      layerIds.zip(weights)
      .map { case (layer, weight) =>
        val raster = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layer)
        val masked = raster.mask(polygon)
        val ratio = LayerRatio.rasterResult(masked)

        LayerSummary(layer.name, ratio.value * weight)
      }

    layerRatios.foldLeft(SummaryResult(List[LayerSummary](), 0.0)) { (result, layerSummary) =>
        SummaryResult(layerSummary :: result.layerSummaries, result.score + layerSummary.score)
    }
  }
}

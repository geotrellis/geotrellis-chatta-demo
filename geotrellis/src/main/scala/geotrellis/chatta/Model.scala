package geotrellis.chatta

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.vector._

object CollectionLayerRatio {
  def rasterResult(r: TileLayerCollection[SpatialKey]): LayerRatio = {
    val mapTransform = r.metadata.mapTransform
    val (sum, count) =
      r map { case (k, tile) =>
        val extent = mapTransform(k)
        (tile.polygonalSum(extent, extent.toPolygon()), tile.size)
      } reduce[(Long, Int)] { case (t1, t2) => (t1._1 + t2._1, t1._2 + t2._2) }

    LayerRatio(sum, count)
  }
}

object Model {
  def weightedOverlay(layers: Iterable[String], weights: Iterable[Int], zoom: Int, rasterExtent: RasterExtent)
                     (reader: CollectionLayerReader[LayerId]): Seq[(SpatialKey, Tile)] = {

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
             (reader: CollectionLayerReader[LayerId]): SummaryResult = {

    val layerIds = layers.map(LayerId(_, zoom))
    val layerRatios =
      layerIds.zip(weights)
        .map { case (layer, weight) =>
          val raster = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layer)
          val masked = raster.mask(polygon)
          val ratio = CollectionLayerRatio.rasterResult(masked)

          LayerSummary(layer.name, ratio.value * weight)
        }

    layerRatios.foldLeft(SummaryResult(List[LayerSummary](), 0.0)) { (result, layerSummary) =>
      SummaryResult(layerSummary :: result.layerSummaries, result.score + layerSummary.score)
    }
  }
}

package chatta

import com.vividsolutions.jts.{geom => jts}
import geotrellis._
import geotrellis.vector._
import geotrellis.raster.rasterize.{Callback, Rasterizer}
import geotrellis.raster.op.zonal.summary._

/*case class LayerSummary(name: String, score: Double)
case class SummaryResult(layerSummaries: List[LayerSummary], score: Double)

case class LayerRatio(sum: Int, count: Int) {
	def value = sum / count.toDouble
	def combine(other: LayerRatio) =
		LayerRatio(sum + other.sum, count + other.count)
}

object LayerRatio {
	def rasterResult(r: Raster): LayerRatio = {
		var sum = 0
		var count = 0
		r.foreach { x =>
			if (x != NODATA) { sum = sum + x }
			count += 1
		}
		// TODO : perf aspect of this ?
		val sum = r.toArray().filter(_ != NODATA).sum
		LayerRatio(sum, r.rows * r.cols)

	}
}

object Model {

	def weightedOverlay(layers: Iterable[String], weights: Iterable[Int], rasterExtent: RasterExtent): RasterSource =
		layers.zip(weights).map {
			case (layer, weight) =>
				RasterSource(s"wm_$layer", rasterExtent).convert(TypeByte).localMultiply(weight)
		}.localAdd().localMask(RasterSource("mask", rasterExtent), NODATA, NODATA)

	def summary(layers: Iterable[String], weights: Iterable[Int], polygon: jts.Polygon): ValueSource[SummaryResult] = {

		val layerRatios: SeqSource[LayerSummary] =
			layers.zip(weights).map {
				case (layer, weight) =>

					val tileCache = Main.getCachedRatios(layer)
					RasterSource(s"albers_$layer").mapIntersecting(Polygon(polygon, 0), tileCache) {

						case FullTileIntersection(tile) =>
							LayerRatio.rasterResult(tile)

						case PartialTileIntersection(tile, intersections) =>
							var sum: Int = 0
							var total: Int = 0
							val f = new Callback[Geometry, Any] {
								def apply(col: Int, row: Int, g: Geometry[Any]) {
									total += 1
									val z = tile.get(col, row)
									if (isData(z)) {
										sum += z
									}
								}
							}

							intersections.foreach { g =>
								Rasterizer.foreachCellByFeature(g, tile.rasterExtent)(f)
							}
							LayerRatio(sum, total)

					}.foldLeft(LayerRatio(0,0)) {
						(l1,l2) =>
							l1.combine(l2)
					}.map {
						ratio =>
							LayerSummary(layer, ratio.value * weight)
					}
			}

		layerRatios.foldLeft(SummaryResult(List[LayerSummary](), 0.0)) {
			(result, layerSummary) =>
				SummaryResult(layerSummary :: result.layerSummaries, result.score + layerSummary.score)
		}
	}
}
*/

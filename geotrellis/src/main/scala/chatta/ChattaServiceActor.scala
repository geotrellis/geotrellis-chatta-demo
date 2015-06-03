package chatta

import akka.actor._
import com.vividsolutions.jts.{geom => jts}
import geotrellis._
import geotrellis.data.geojson._
import geotrellis.render._
import geotrellis.services._
import geotrellis.source._
import spray.http._
import spray.routing.HttpService

class ChattaServiceActor(val staticPath: String) extends Actor with ChattaService {
	override def actorRefFactory = context
	override def receive = runRoute(serviceRoute)
}

trait ChattaService extends HttpService {

	implicit def executionContext = actorRefFactory.dispatcher
	val staticPath: String

	def serviceRoute = get { home ~ api }

	private def home = (pathSingleSlash | pathPrefix("")) {
		getFromDirectory(staticPath)
	}

	private def api = pathPrefix("gt") {
		path("colors") {
			complete(ColorRampMap.getJson)
		} ~
		path("breaks") {
			parameters('layers, 'weights, 'numBreaks.as[Int], 'mask ? "") {
				(layersParam, weightsParam, numBreaks, mask) => {
					val extent = Extent(-9634947.090, 4030964.877, -9359277.090, 4300664.877)
					val re = RasterExtent(extent, 256,256)
					val layers = layersParam.split(",")
					val weights = weightsParam.split(",").map(_.toInt)

					Model.weightedOverlay(layers, weights, re).classBreaks(numBreaks).run match {

						case process.Complete(breaks, _) =>
							val breaksArray = breaks.mkString("[", ",", "]")
							val json = s"""{ "classBreaks" : $breaksArray }"""
							complete(json)

						case process.Error(message, trace) =>
							failWith(new RuntimeException(message))
					}
				}
			}
		} ~
		path("wo") {
			parameters('service, 'request, 'version, 'format, 'bbox, 'height.as[Int], 'width.as[Int], 'layers, 'weights,
				'palette ? "ff0000,ffff00,00ff00,0000ff", 'colors.as[Int] ? 4, 'breaks, 'colorRamp ? "blue-to-red", 'mask ? "") {
				(_, _, _, _, bbox, cols, rows, layersString, weightsString, palette, colors, breaksString, colorRamp, mask) => {

					val extent = Extent.fromString(bbox)
					val re = RasterExtent(extent, cols, rows)

					val layers = layersString.split(",")
					val weights = weightsString.split(",").map(_.toInt)
					val model = Model.weightedOverlay(layers,weights,re)

					val overlay =
						if (mask.isEmpty) model
						else {
							GeoJsonReader.parse(mask) match {

								case Some(geomArray) if geomArray.length == 1 =>
									val transformed =
										geomArray.head.mapGeom { g =>
											Transformer.transform(g, Projections.LatLong, Projections.WebMercator)
										}
									model.mask(transformed)

								case _ =>
									throw new Exception(s"Invalid GeoJSON: $mask")
							}
						}

					val breaks = breaksString.split(",").map(_.toInt)

					val ramp = {
						val cr = ColorRampMap.getOrElse(colorRamp, ColorRamps.BlueToRed)
						if (cr.toArray.length < breaks.length) cr.interpolate(breaks.length)
						else cr
					}

					val png: ValueSource[Png] = overlay.renderPng(ramp, breaks)
					png.run match {

						case process.Complete(img, h) =>
							respondWithMediaType(MediaTypes.`image/png`) {
								complete(img)
							}

						case process.Error(message,trace) =>
							println(message)
							println(trace)
							println(re)
							failWith(new RuntimeException(message))
					}
				}
			}
		} ~
		path("sum") {
			parameters('polygon, 'layers, 'weights) {
				(polygonJson, layersString, weightsString) => {
					val start = System.currentTimeMillis()

					val poly =
						GeoJsonReader.parse(polygonJson) match {

							case Some(geomArray) if geomArray.length == 1 =>
								geomArray.head.geom match {

									case p: jts.Polygon =>
										Transformer.transform(p, Projections.LatLong, Projections.ChattaAlbers).asInstanceOf[jts.Polygon]

									case _ =>
										throw new Exception(s"Invalid GeoJSON: $polygonJson")
								}

							case _ =>
								throw new Exception(s"Invalid GeoJSON: $polygonJson")
						}

					val re = Main.getRasterExtent(poly)

					val layers = layersString.split(",")
					val weights = weightsString.split(",").map(_.toInt)

					val summary = Model.summary(layers, weights, poly)

					summary.run match {

						case process.Complete(result,h) =>
							val elapsedTotal = System.currentTimeMillis - start

							val layerSummaries =
								"[" + result.layerSummaries.map {
									ls =>
										val v = "%.2f".format(ls.score * 100)
										s"""{ "layer": "${ls.name}", "total": "$v" }"""
								}.mkString(",") + "]"

							val totalVal = "%.2f".format(result.score * 100)
							val data =
								s"""{
                                    "layerSummaries": $layerSummaries,
                                    "total": "$totalVal",
									"elapsed": "$elapsedTotal"
                                }"""
							complete(data)

						case process.Error(message,trace) =>
							failWith(new RuntimeException(message))
					}
				}
			}
		}
	}

}

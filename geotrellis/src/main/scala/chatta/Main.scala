package chatta

import akka.actor.Props
import akka.io.IO
import com.typesafe.config.ConfigFactory
import com.vividsolutions.jts.{geom => jts}
import geotrellis._
import geotrellis.source._
import spray.can.Http

object Main {

	private var cachedRatios: Map[String, SeqSource[LayerRatio]] = _
	private lazy val albersRasterExtent = RasterSource("albers_Wetlands").rasterExtent.get

	val weights = Map(
		"ImperviousSurfaces_Barren Lands_Open Water" -> 1,
		"DevelopedLand" -> 2,
		"Wetlands" -> 3,
		"ForestedLands" -> 4,
		"Non-workingProtectedOrPublicLands" -> 5,
		"PrimeAgriculturalSoilsNotForestedOrFarmland" -> 6,
		"PublicallyOwnedWorkingLands" -> 7,
		"PrivatelyOwnedWorkingLandsWithEasements" -> 8,
		"FarmlandWithoutPrimeAgriculturalSoils" -> 9,
		"FarmlandOrForestedLandsWithPrimeAgriculturalSoils" -> 10
	)

	def initCache(): Boolean = {
		try {
			cachedRatios = (for (layer <- weights.keys) yield {
					println(s"CACHING TILE RESULT FOR RASTER $layer")
					(layer, RasterSource(s"albers_$layer").map(LayerRatio.rasterResult).cached)
				}).toMap
			true
		} catch {
			case e: Exception =>
				GeoTrellis.shutdown()
				println(s"Could not load tile set: $e.message")
				e.printStackTrace()
				false
		}
	}

	def main(args: Array[String]): Unit = {
		if (initCache()) {
			implicit val system = server.system

			val config = ConfigFactory.load()
			val staticPath = config.getString("geotrellis.server.static-path")
			val port = config.getInt("geotrellis.port")
			val host = config.getString("geotrellis.hostname")

			// create and start our service actor
			val service = system.actorOf(Props(classOf[ChattaServiceActor], staticPath), "chatta-service")

			// start a new HTTP server on port 8080 with our service actor as the handler
			IO(Http) ! Http.Bind(service, host, port = port)
		}
	}

	def getRasterExtent(polygon:jts.Geometry): Op[RasterExtent] = {
		val env = polygon.getEnvelopeInternal
		val e = Extent(env.getMinX, env.getMinY, env.getMaxX, env.getMaxY )
		albersRasterExtent.createAligned(e)
	}

	def getCachedRatios(layer: String): SeqSource[LayerRatio] = {
		cachedRatios(layer)
	}
}

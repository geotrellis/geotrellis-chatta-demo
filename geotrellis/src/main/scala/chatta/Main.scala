package chatta

import akka.actor.Props
import akka.io.IO
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import spray.can.Http

object Main {

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

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("chatta-demo")

    val config = ConfigFactory.load()
    val staticPath = config.getString("geotrellis.server.static-path")
    val port = config.getInt("geotrellis.port")
    val host = config.getString("geotrellis.hostname")

    // create and start our service actor
    val service = system.actorOf(Props(classOf[ChattaServiceActor], staticPath, config), "chatta-service")

    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ! Http.Bind(service, host, port)
  }

}


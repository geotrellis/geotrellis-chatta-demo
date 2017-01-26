package geotrellis.chatta

import geotrellis.kryo.AvroRegistrator

import akka.actor.Props
import akka.io.IO
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import org.apache.spark.{SparkConf, SparkContext}

object AkkaSystem {
  implicit val system = ActorSystem("chatta-demo")
  implicit val materializer = ActorMaterializer()

  trait LoggerExecutor {
    protected implicit val log = Logging(system, "app")
  }
}

object Main extends ChattaServiceRouter {
  import AkkaSystem._

  val weights = Map(
    "ImperviousSurfacesBarrenLandsOpenWater" -> 1,
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

  val config = ConfigFactory.load()
  val staticPath = config.getString("geotrellis.server.static-path")
  val port = config.getInt("geotrellis.port")
  val host = config.getString("geotrellis.hostname")
  val withTimings = config.getBoolean("geotrellis.with-timings")

  /*val conf = AvroRegistrator(
    new SparkConf()
      .setAppName("ChattaDemo")
      .set("spark.serializer", classOf[org.apache.spark.serializer.KryoSerializer].getName)
      .set("spark.kryo.registrator", classOf[geotrellis.spark.io.kryo.KryoRegistrator].getName)
  )

  implicit val sc = new SparkContext(conf)*/

  lazy val (reader, tileReader, attributeStore) = initCollectionBackend(config)

  def main(args: Array[String]): Unit = {
    Http().bindAndHandle(routes, host, port)
  }
}

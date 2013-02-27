package chatta

import geotrellis.rest.WebRunner
import geotrellis.process.{Server,Catalog}

object Main {
  val rasterProjection = 

  val server = Server("tutorial-server",
                      Catalog.fromPath("data/catalog.json"))

  def main(args: Array[String]) = WebRunner.main(args)
}

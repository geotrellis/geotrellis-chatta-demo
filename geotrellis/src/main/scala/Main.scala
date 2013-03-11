package chatta

import geotrellis.rest.WebRunner
import geotrellis.process.{Server,Catalog}
import geotrellis._
import geotrellis.raster.op._
import geotrellis.feature._

object Main {
  val server = Server("tutorial-server",
                      Catalog.fromPath("data/catalog.json"))

  def main(args: Array[String]) = {
    WebRunner.main(args)
  }

  def getRasterExtent(polygon:Op[Geometry[_]]):Op[RasterExtent] = {
    val e = GetFeatureExtent(polygon)
    val rasterExtent = io.LoadRasterExtent("albers_Wetlands")
    extent.CropRasterExtent(rasterExtent,e)
  }
}


case class GetFeatureExtent(f:Op[Geometry[_]]) extends Op1(f)({
  (f) => {
    val env = f.geom.getEnvelopeInternal
    Result(Extent( env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY() ))
  }
})

case class AsPolygon[D](g:Op[Geometry[D]]) extends Op1(g) ({
  g =>
    Result(g.asInstanceOf[Polygon[D]])
})



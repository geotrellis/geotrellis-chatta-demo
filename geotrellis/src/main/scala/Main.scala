package chatta

import geotrellis.rest.WebRunner
import geotrellis.process.{Server,Catalog}
import geotrellis._
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.feature._

case class TiledLayer(raster:Raster,tileRatios:Map[RasterExtent,LayerRatio])

object Main {
  val server = Server("tutorial-server",
                      Catalog.fromPath("data/catalog.json"))

  private var tiledLayers:Map[String,TiledLayer] = null

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

  def main(args: Array[String]):Unit = {
    try {
      tiledLayers = { for(layer <- weights.keys) yield { 
        val tilePath = s"data/albers_tiled/albers_$layer"
        println(s"LOADING TILE RASTER $tilePath")
        val r = Raster.loadTileSet(tilePath, server)

        val tileSetRD = r.data.asInstanceOf[TileArrayRasterData]
        val tileSums = RatioOfOnes.createTileResults(tileSetRD, r.rasterExtent)
        (layer,TiledLayer(r,tileSums))
      } }.toMap
    } catch {
      case e:Exception => 
        server.shutdown()
        println(s"Could not load tile set: $e.message")
        e.printStackTrace()
       return
    }

    WebRunner.main(args)
  }

  def getRasterExtent(polygon:Op[Geometry[_]]):Op[RasterExtent] = {
    val e = GetFeatureExtent(polygon)
    val rasterExtent = io.LoadRasterExtent("albers_Wetlands")
    extent.CropRasterExtent(rasterExtent,e)
  }

  def getTileLayer(layer:String) = {
    tiledLayers(layer)
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
    Result(Polygon[Int](g.asInstanceOf[Polygon[D]].geom,0))
})



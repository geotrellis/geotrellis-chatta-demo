package chatta

import geotrellis._
import geotrellis.raster.op._
import geotrellis.Implicits._

object Model {
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

  def run(rasterExtent:Op[RasterExtent], crs:String = "wm") = {
    val ops = for((name,weight) <- weights) yield {
      val rast = io.LoadRaster(s"${crs}_${name}",rasterExtent)
      val converted = Force(rast.map { r => r.convert(TypeShort) })
      local.Multiply(weight, converted)
    }
    local.IfCell(local.Add(ops.toSeq:_*),_ == 0, NODATA)
  }
}

object WeightedOverlayArray {
  def apply(rasters:Op[Array[Raster]], weights:Op[Array[Int]]) = {

    val rs:Op[Array[Raster]] = logic.ForEach(rasters, weights)(_ * _)

    val weightSum:Op[Int] = logic.Do(weights)(_.sum)

    local.AddArray(rs) / weightSum
  }
}

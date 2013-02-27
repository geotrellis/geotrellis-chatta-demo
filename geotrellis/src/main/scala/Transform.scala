package chatta

import geotrellis.feature._

import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.opengis.referencing.operation.MathTransform

import com.vividsolutions.jts.{ geom => jts }

object Transformer {
  private var mathTransformTo:MathTransform = null
  private var mathTransformFrom:MathTransform = null

  private def init() = {
    // Well known text of raster projection
    val wkt = """
PROJCS["Albers_Conical_Equal_Area",
    GEOGCS["NAD83",
        DATUM["North_American_Datum_1983",
            SPHEROID["GRS 1980",6378137,298.2572221010002,
                AUTHORITY["EPSG","7019"]],
            AUTHORITY["EPSG","6269"]],
        PRIMEM["Greenwich",0],
        UNIT["degree",0.0174532925199433],
        AUTHORITY["EPSG","4269"]],
    PROJECTION["Albers_Conic_Equal_Area"],
    PARAMETER["standard_parallel_1",29.5],
    PARAMETER["standard_parallel_2",45.5],
    PARAMETER["latitude_of_center",23],
    PARAMETER["longitude_of_center",-96],
    PARAMETER["false_easting",0],
    PARAMETER["false_northing",0],
    UNIT["metre",1,
        AUTHORITY["EPSG","9001"]]]
"""

    val rasterCRS = CRS.parseWKT(wkt)
    val latlongCRS = CRS.decode("ESPG:4326")

    mathTransformTo = CRS.findMathTransform(latlongCRS,rasterCRS,true)
    mathTransformFrom = CRS.findMathTransform(rasterCRS,latlongCRS,true)
  }

  init()

  def transformTo[G <: jts.Geometry,D](feature:SingleGeometry[G,D]) = {
    JTS.transform(feature.geom, mathTransformTo)
    feature
  }

  def transformFrom[G <: jts.Geometry,D](feature:SingleGeometry[G,D]) = {
    JTS.transform(feature.geom, mathTransformFrom)
    feature
  }
}

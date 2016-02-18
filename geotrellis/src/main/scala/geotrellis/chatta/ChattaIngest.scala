package geotrellis.chatta

import geotrellis.raster.Tile
import geotrellis.spark.SpatialKey
import geotrellis.spark.etl.Etl
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark.ingest._
import geotrellis.vector.ProjectedExtent
import org.apache.spark.SparkConf

object ChattaIngest extends App {
  implicit val sc = SparkUtils.createSparkContext("GeoTrellis ETL SinglebandIngest", new SparkConf(true))
  Etl.ingest[ProjectedExtent, SpatialKey, Tile](args, ZCurveKeyIndexMethod)
  sc.stop()
}

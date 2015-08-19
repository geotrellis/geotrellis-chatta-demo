package geotrellis.chatta

import geotrellis.spark.SpatialKey
import geotrellis.spark.etl.Etl
import geotrellis.spark.etl.accumulo.AccumuloModule
import geotrellis.spark.io.index.HilbertKeyIndexMethod
import geotrellis.spark.utils.SparkUtils
import org.apache.spark.SparkConf

object ChattaIngest extends App {

  val etl = Etl[SpatialKey](args)
  implicit val sc = SparkUtils.createSparkContext("GeoTrellis ETL", new SparkConf(true))

  val (id, rdd) = etl.load()
  etl.save(id, rdd, HilbertKeyIndexMethod)

  sc.stop()

}
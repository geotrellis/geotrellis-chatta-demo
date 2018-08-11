package geotrellis

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.hbase._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.file._
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import com.typesafe.config.Config
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkContext

import scala.collection.JavaConversions._

package object chatta {
  def initBackend(config: Config)(implicit cs: SparkContext): (FilteringLayerReader[LayerId], ValueReader[LayerId], AttributeStore)  = {
    config.getString("geotrellis.backend") match {
      case "s3" => {
        val (bucket, prefix) = config.getString("s3.bucket") -> config.getString("s3.prefix")
        val attributeStore = S3AttributeStore(bucket, prefix)

        (S3LayerReader(attributeStore), S3ValueReader(bucket, prefix), attributeStore)
      }
      case "accumulo" => {
        val instance = AccumuloInstance(
          config.getString("accumulo.instance"),
          config.getString("accumulo.zookeepers"),
          config.getString("accumulo.user"),
          new PasswordToken(config.getString("accumulo.password"))
        )

        (AccumuloLayerReader(instance), AccumuloValueReader(instance), AccumuloAttributeStore(instance))
      }
      case "hbase" => {
        val instance = HBaseInstance(
          config.getString("hbase.zookeepers").split(","),
          config.getString("hbase.master")
        )

        (HBaseLayerReader(instance), HBaseValueReader(instance), HBaseAttributeStore(instance))
      }
      case "cassandra" => {
        val instance = BaseCassandraInstance(
          config.getStringList("cassandra.hosts").toList,
          config.getString("cassandra.user"),
          config.getString("cassandra.password"),
          config.getString("cassandra.replicationStrategy"),
          config.getInt("cassandra.replicationFactor")
        )

        (CassandraLayerReader(instance), CassandraValueReader(instance), CassandraAttributeStore(instance))
      }
      case "hadoop" => {
        val path = config.getString("hadoop.path")
        (HadoopLayerReader(path), HadoopValueReader(path), HadoopAttributeStore(path))
      }
      case "file" => {
        val path = config.getString("file.path")
        (FileLayerReader(path), FileValueReader(path), FileAttributeStore(path))
      }
      case s => throw new Exception(s"not supported backend: $s")
    }
  }

  def initCollectionBackend(config: Config): (CollectionLayerReader[LayerId], ValueReader[LayerId], AttributeStore)  = {
    config.getString("geotrellis.backend") match {
      case "s3" => {
        val (bucket, prefix) = config.getString("s3.bucket") -> config.getString("s3.prefix")
        val attributeStore = S3AttributeStore(bucket, prefix)

        (S3CollectionLayerReader(attributeStore), S3ValueReader(bucket, prefix), attributeStore)
      }
      case "accumulo" => {
        val instance = AccumuloInstance(
          config.getString("accumulo.instance"),
          config.getString("accumulo.zookeepers"),
          config.getString("accumulo.user"),
          new PasswordToken(config.getString("accumulo.password"))
        )

        (AccumuloCollectionLayerReader(instance), AccumuloValueReader(instance), AccumuloAttributeStore(instance))
      }
      case "hbase" => {
        val instance = HBaseInstance(
          config.getString("hbase.zookeepers").split(","),
          config.getString("hbase.master")
        )

        (HBaseCollectionLayerReader(instance), HBaseValueReader(instance), HBaseAttributeStore(instance))
      }
      case "cassandra" => {
        val instance = BaseCassandraInstance(
          config.getStringList("cassandra.hosts").toList,
          config.getString("cassandra.user"),
          config.getString("cassandra.password"),
          config.getString("cassandra.replicationStrategy"),
          config.getInt("cassandra.replicationFactor")
        )

        (CassandraCollectionLayerReader(instance), CassandraValueReader(instance), CassandraAttributeStore(instance))
      }
      case "hadoop" => {
        val attributeStore = HadoopAttributeStore(config.getString("hadoop.path"), {
          val c = new Configuration
          // c.set("fs.default.name", "hdfs://" + host + ":" + fsPort)
          // c.set("mapred.job.tracker", host + ":" + jtPort)
          c
        })

        (HadoopCollectionLayerReader(attributeStore), HadoopValueReader(attributeStore), attributeStore)
      }
      case "file" => {
        val path = config.getString("file.path")
        (FileCollectionLayerReader(path), FileValueReader(path), FileAttributeStore(path))
      }
      case s => throw new Exception(s"not supported backend: $s")
    }
  }

}

package geotrellis.chatta

import org.apache.log4j.{PatternLayout, WriterAppender, Logger}

import scala.collection.mutable
import scala.collection.JavaConversions._
import java.io.StringWriter
import java.util

/**
  * LazyLogging dirty trait
  */
trait LazyLogging { self =>
  @transient private lazy val logger: Logger = Logger.getLogger(self.getClass)
  val withTimings: Boolean

  private val logBuffer = new util.concurrent.ConcurrentHashMap[String, mutable.ListBuffer[String]]()

  def timedCreate[T](id: String, startMsg: String, endMsg: String)(f: => T): T = {
    if(withTimings) {
      val writer = new StringWriter()
      val appender = new WriterAppender(new PatternLayout(), writer)

      logger.info(startMsg)
      logger.addAppender(appender)
      val s = System.currentTimeMillis
      val result = f
      val e = System.currentTimeMillis
      val t = "%,d".format(e - s)
      logger.info(s"\t$endMsg (in $t ms)")
      writer.flush()
      logger.removeAppender(appender)
      Option(logBuffer.get(id))
        .fold(logBuffer.put(id, mutable.ListBuffer(writer.toString)))(_ += writer.toString)

      result
    } else f
  }

  def printBuffer(id: String) =
    if(withTimings) {
      println
      println("=================================")
      println(s"$id summary:")
      println("=================================")
      println
      logBuffer(id) foreach println
      println("=================================")
      println
      logBuffer(id) clear()
    }
}
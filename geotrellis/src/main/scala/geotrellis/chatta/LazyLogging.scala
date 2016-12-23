package geotrellis.chatta

import org.apache.log4j.{PatternLayout, WriterAppender, Logger}

import scala.collection.mutable
import java.io.StringWriter

/**
  * LazyLogging dirty trait
  */
trait LazyLogging { self =>
  @transient private lazy val logger: Logger = Logger.getLogger(self.getClass)

  private val logBuffer = mutable.Map[String, mutable.ListBuffer[String]]()

  def timedCreate[T](id: String, startMsg: String, endMsg: String)(f: => T): T = {
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
    logBuffer.get(id).fold(
      logBuffer.update(id, mutable.ListBuffer(writer.toString))
    )(_ += writer.toString)

    result
  }

  def printBuffer(id: String) = {
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
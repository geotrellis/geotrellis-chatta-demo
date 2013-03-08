package chatta

import org.codehaus.jackson._
import org.codehaus.jackson.JsonToken._
import org.codehaus.jackson.map._

import geotrellis._
import geotrellis.io.LoadGeoJson
import geotrellis.feature._

import scala.language.existentials

case class WORequest(bbox:Extent,cols:Int,rows:Int,polygon:Polygon[_]) {
  def rasterExtent = {
    val cellWidth = (bbox.xmax - bbox.xmin) / cols.toDouble
    val cellHeight = (bbox.ymax - bbox.ymin) / rows.toDouble
    RasterExtent(bbox, cellWidth, cellHeight, cols, rows)
  }
}

object JsonParser {
  val parserFactory = new MappingJsonFactory()

  def parseWORequest(json:String):WORequest = {
    var bbox:Extent = null
    var polygon:Polygon[_] = null
    var cols = 0
    var rows = 0

    val parser = parserFactory.createJsonParser(json)

    if(parser.nextToken() != START_OBJECT) 
      sys.error("Json does not start as object.")
    else { 
      var token = parser.nextToken()
      while (token != null) {
        token match {
          case FIELD_NAME => 
            parser.getCurrentName() match {
              case "bbox" =>
                parser.nextToken()
                bbox = parseExtent(parser.getText())
              case "rows" =>
                parser.nextToken()
                rows = parser.getText().toInt
              case "cols" =>
                parser.nextToken()
                cols = parser.getText().toInt
              case "polygon" =>
                parser.nextToken()
                val txt = parser.getText()
                LoadGeoJson.parse(txt) match {
                  case Some(f) =>
                    polygon = f.asInstanceOf[Polygon[_]]
                  case None =>
                    sys.error(s"Could not parse GeoJson: $txt")
                }
              case f => sys.error(s"Unknown field name $f.")
            }
          case END_OBJECT => // Done.
          case _ => 
            sys.error("Expecting a field name.")
        }
        token = parser.nextToken()    
      }
    }

    WORequest(bbox,cols,rows,polygon)
  }

  def parseExtent(extText:String) = { 
    try {
      val Array(x1, y1, x2, y2) = extText.split(",").map(_.toDouble)
      Extent(x1, y1, x2, y2)
    } catch {
      case _:Exception => sys.error(s"could not parse parse extent string: $extText")
    }
  }
}

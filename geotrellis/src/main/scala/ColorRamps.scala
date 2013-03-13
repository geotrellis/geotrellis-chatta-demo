package chatta

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, POST, Path, Consumes, DefaultValue, QueryParam}
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context, MediaType, MultivaluedMap}
import geotrellis._
import geotrellis.data.ColorRamps

import scala.collection.JavaConversions._

object Colors {
  val rampMap = Map(
    "blue-to-orange" -> ColorRamps.BlueToOrange,
    "green-to-orange" -> ColorRamps.LightYellowToOrange,
    "blue-to-red" -> ColorRamps.BlueToRed,
    "green-to-red-orange" -> ColorRamps.GreenToRedOrange,
    "light-to-dark-sunset" -> ColorRamps.LightToDarkSunset,
    "light-to-dark-green" -> ColorRamps.LightToDarkGreen,
    "yellow-to-red-heatmap" -> ColorRamps.HeatmapYellowToRed,
    "blue-to-yellow-to-red-heatmap" -> ColorRamps.HeatmapBlueToYellowToRedSpectrum,
    "dark-red-to-yellow-heatmap" -> ColorRamps.HeatmapDarkRedToYellowWhite,
    "purple-to-dark-purple-to-white-heatmap" -> ColorRamps.HeatmapLightPurpleToDarkPurpleToWhite,
    "bold-land-use-qualitative" -> ColorRamps.ClassificationBoldLandUse,
    "muted-terrain-qualitative" -> ColorRamps.ClassificationMutedTerrain)
}

/**
 * Sum values under polygon provided via geojson
 */
@Path("/gt/colors")
class ColorRampsService {
  @GET
  def get(
    @DefaultValue("") @QueryParam("img") image:String,
    @Context req:HttpServletRequest
  ):Any = {
    // Return JSON with information on color ramps.
    val c = for(key <- Colors.rampMap.keys) yield {
      s"""{ "key": "$key", "image": "/img/ramps/${key}.png" }"""
    }
    val arr = "[" + c.mkString(",") + "]"
    Response.ok(s"""{ "colors": $arr }""")
            .`type`("application/json")
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Credentials", "true")
            .build()
  }
}

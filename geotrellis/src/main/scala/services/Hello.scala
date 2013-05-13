package chatta

import geotrellis.rest._

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Context
import javax.ws.rs._

@Path("/")
class Hello {
  @GET
  def get(@Context req:HttpServletRequest):core.Response = {
    val message = "GeoTrellis is running."
    OK(message)
  }
}

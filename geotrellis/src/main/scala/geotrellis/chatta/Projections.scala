package geotrellis.chatta

import geotrellis.proj4.CRS

object Projections {

  val ChattaAlbers = CRS.fromString(
    """|+proj=aea +lat_1=29.5 +lat_2=45.5 +lat_0=23 +lon_0=-96 +x_0=0 +y_0=0 +datum=NAD83 +units=m +no_defs""")

  val WebMercator = CRS.fromName("EPSG:3857")
  val LatLong = CRS.fromName("EPSG:4326")

}

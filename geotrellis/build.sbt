name := "GeoTrellis Tutorial Project"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "com.azavea.geotrellis" %% "geotrellis" % "0.8.0",
  "org.geotools" % "gt-main" % "8.0-M4",
  "org.geotools" % "gt-jdbc" % "8.0-M4",
  "org.geotools.jdbc" % "gt-jdbc-postgis" % "8.0-M4",
  "org.geotools" % "gt-coverage" % "8.0-M4",
  "org.geotools" % "gt-coveragetools" % "8.0-M4"
)



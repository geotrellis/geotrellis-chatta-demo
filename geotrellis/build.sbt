name := "GeoTrellis Tutorial Project"

scalaVersion := "2.10.0"

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers +=       "Geotools" at "http://download.osgeo.org/webdav/geotools/"

libraryDependencies ++= Seq(
  "com.azavea.geotrellis" %% "geotrellis" % "0.9.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-server" % "0.9.0-SNAPSHOT",
  "org.geotools" % "gt-main" % "8.0-M4",
  "org.geotools" % "gt-jdbc" % "8.0-M4",
  "org.geotools.jdbc" % "gt-jdbc-postgis" % "8.0-M4",
  "org.geotools" % "gt-coverage" % "8.0-M4",
  "org.geotools" % "gt-coveragetools" % "8.0-M4"
)

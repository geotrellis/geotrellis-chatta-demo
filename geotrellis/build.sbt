import AssemblyKeys._

name := "GeoTrellis-Tutorial-Project"

scalaVersion := "2.10.5"

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += "Geotools" at "http://download.osgeo.org/webdav/geotools/"

resolvers += "Local Ivy" at "file://"+Path.userHome.absolutePath+"/.ivy2/local/"

libraryDependencies ++= Seq(
  "com.azavea.geotrellis" %% "geotrellis-services" % "0.10.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-spark" % "0.10.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-spark-etl" % "0.10.0-SNAPSHOT",
  "org.apache.spark" %% "spark-core" % "1.4.0",
  "io.spray" %% "spray-routing" % "1.3.3",
  "io.spray" %% "spray-can" % "1.3.3",
  "org.geotools" % "gt-main" % "13.1",
  "org.geotools" % "gt-coveragetools" % "13.1",
  "org.apache.hadoop" % "hadoop-client" % "2.7.0"
)

Revolver.settings

assemblySettings

mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case "reference.conf" => MergeStrategy.concat
    case "application.conf" => MergeStrategy.concat
    case "META-INF/MANIFEST.MF" => MergeStrategy.discard
    case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
}

cancelable in Global := true

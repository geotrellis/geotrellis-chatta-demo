import scala.util.Properties

name := "GeoTrellis-Tutorial-Project"
scalaVersion := Properties.propOrElse("scala.version", "2.10.6")
crossScalaVersions := Seq("2.11.8", "2.10.6")
organization := "com.azavea"
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Yinline-warnings",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:existentials",
  "-feature")
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

resolvers += Resolver.bintrayRepo("azavea", "geotrellis")

val gtVersion = "1.0.0-SNAPSHOT"

val geotrellis = Seq(
  "com.azavea.geotrellis" %% "geotrellis-accumulo"  % gtVersion,
  "com.azavea.geotrellis" %% "geotrellis-cassandra" % gtVersion,
  "com.azavea.geotrellis" %% "geotrellis-s3"        % gtVersion,
  "com.azavea.geotrellis" %% "geotrellis-spark"     % gtVersion,
  "com.azavea.geotrellis" %% "geotrellis-spark-etl" % gtVersion
)

libraryDependencies ++= (((Seq(
  "org.apache.spark"  %% "spark-core"    % "1.5.2",
  "io.spray"          %% "spray-routing" % "1.3.3",
  "io.spray"          %% "spray-can"     % "1.3.3",
  "org.apache.hadoop"  % "hadoop-client" % "2.7.1"
) ++ geotrellis) map (_ exclude("com.google.guava", "guava"))) ++ Seq("com.google.guava" % "guava" % "16.0.1"))

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}

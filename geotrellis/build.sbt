name := "geotrellis-chatta-demo"
scalaVersion := "2.11.8"
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

val gtVersion   = "1.0.0"

val geotrellis = Seq(
  "org.locationtech.geotrellis" %% "geotrellis-accumulo"  % gtVersion,
  "org.locationtech.geotrellis" %% "geotrellis-hbase"     % gtVersion,
  "org.locationtech.geotrellis" %% "geotrellis-cassandra" % gtVersion,
  "org.locationtech.geotrellis" %% "geotrellis-s3"        % gtVersion,
  "org.locationtech.geotrellis" %% "geotrellis-spark"     % gtVersion,
  "org.locationtech.geotrellis" %% "geotrellis-spark-etl" % gtVersion
)

val akka = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe.akka" %% "akka-http-core" % "2.4.11",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.11",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.11"
)

libraryDependencies ++= (((Seq(
  "org.apache.spark"  %% "spark-core"    % "2.0.2",
  "org.apache.hadoop"  % "hadoop-client" % "2.7.3"
) ++ geotrellis ++ akka) map (_ exclude("com.google.guava", "guava"))) ++ Seq("com.google.guava" % "guava" % "16.0.1"))

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case "reference.conf" | "application.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" | "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" | "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}

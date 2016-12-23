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

val gtVersion        = "1.0.0-SNAPSHOT"
val akkaActorVersion = "2.4.16"
val akkaHttpVersion  = "2.4.11"

libraryDependencies ++= ((Seq(
  "org.locationtech.geotrellis" %% "geotrellis-spark-etl" % gtVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaActorVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpVersion,
  "org.apache.spark"  %% "spark-core"    % "2.0.2",
  "org.apache.hadoop"  % "hadoop-client" % "2.7.3"
) map (_ exclude("com.google.guava", "guava"))) ++ Seq("com.google.guava" % "guava" % "16.0.1"))

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case "reference.conf" | "application.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" | "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" | "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}

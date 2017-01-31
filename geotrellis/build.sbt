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
val akkaHttpVersion  = "10.0.3"

libraryDependencies ++= Seq(
  "org.locationtech.geotrellis" %% "geotrellis-spark-etl" % gtVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaActorVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "org.apache.spark"  %% "spark-core"    % "2.0.2",
  "org.apache.hadoop"  % "hadoop-client" % "2.7.3"
)

assemblyShadeRules in assembly := {
  val shadePackage = "com.azavea.shaded.demo"
  Seq(
    ShadeRule.rename("com.google.common.**" -> s"$shadePackage.google.common.@1")
      .inLibrary(
      "com.azavea.geotrellis" %% "geotrellis-cassandra" % gtVersion,
        "com.github.fge" % "json-schema-validator" % "2.2.6"
    ).inAll
  )
}

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
  case "reference.conf" | "application.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

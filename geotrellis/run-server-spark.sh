#!/usr/bin/env bash

# GeoTrellis ingest jar
export JAR="target/scala-2.11/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar"

echo "--class geotrellis.chatta.Main --driver-memory=2G $JAR"

spark-submit --class geotrellis.chatta.Main --driver-memory=2G $JAR

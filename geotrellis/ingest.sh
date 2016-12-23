#!/usr/bin/env bash

# GeoTrellis ingest jar
export JAR="target/scala-2.11/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar"

echo "--class geotrellis.chatta.ChattaIngest --driver-memory=2G $JAR \
  --input file:///${PWD}/conf/input.json \
  --output file://${PWD}/conf/output.json \
  --backend-profiles file://${PWD}/conf/backend-profiles.json"

spark-submit \
  --class geotrellis.chatta.ChattaIngest --driver-memory=2G $JAR \
  --input "file:///${PWD}/conf/input.json" \
  --output "file://${PWD}/conf/output-accumulo.json" \
  --backend-profiles "file://${PWD}/conf/backend-profiles.json"

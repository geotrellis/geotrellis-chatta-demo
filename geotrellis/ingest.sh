#!/usr/bin/env bash

# GeoTrellis ingest jar
export JAR="target/scala-2.10/GeoTrellis-Tutorial-Project-assembly-0.1-SNAPSHOT.jar"

# Remove some bad signatures from the assembled JAR
zip -d $JAR META-INF/ECLIPSEF.RSA > /dev/null
zip -d $JAR META-INF/ECLIPSEF.SF > /dev/null

echo "--class geotrellis.chatta.ChattaIngest --driver-memory=2G target/scala-2.10/GeoTrellis-Tutorial-Project-assembly-0.1-SNAPSHOT.jar \
  --input file:///${PWD}/conf/input.json \
  --output file://${PWD}/conf/output.json \
  --backend-profiles file://${PWD}/conf/backend-profiles.json"

spark-submit \
  --class geotrellis.chatta.ChattaIngest --driver-memory=2G target/scala-2.10/GeoTrellis-Tutorial-Project-assembly-0.1-SNAPSHOT.jar \
  --input "file:///${PWD}/conf/input.json" \
  --output "file://${PWD}/conf/output.json" \
  --backend-profiles "file://${PWD}/conf/backend-profiles.json"

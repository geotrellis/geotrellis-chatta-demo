#!/usr/bin/env bash
# Ingest tiled GeoTiff into Accumulo

# Geotrellis (gt-admin) ingest jar
export JAR="./target/scala-2.10/GeoTrellis-Tutorial-Project-assembly-0.1-SNAPSHOT.jar"

# Directory with the input tiled GeoTiff's
LAYERS="./data/arg_wm"

# Table to store tiles
TABLE="chattanooga"

# Destination spatial reference system
CRS="EPSG:3857"

# Accumulo conf
INSTANCE="GIS"
USER="root"
PASSWORD="secret"
ZOOKEEPER="localhost"

# Remove some bad signatures from the assembled JAR
zip -d $JAR META-INF/ECLIPSEF.RSA > /dev/null
zip -d $JAR META-INF/ECLIPSEF.SF > /dev/null

# Go through all layers and run the spark submit job
for LAYER in $(ls $LAYERS)
do

  LAYERNAME=${LAYER%.*}
  INPUT=file:$(realpath $LAYERS/$LAYER)

  echo "spark-submit \
  --class geotrellis.chatta.ChattaIngest --driver-memory=2G $JAR \
  --input hadoop --format geotiff --cache NONE -I path=$INPUT \
  --output accumulo -O instance=$INSTANCE table=$table user=$USER password=$PASSWORD zookeeper=$ZOOKEEPER \
  --layer $LAYERNAME --pyramid --crs $CRS"

  spark-submit \
  --class geotrellis.chatta.ChattaIngest --driver-memory=2G $JAR \
  --input hadoop --format geotiff --cache NONE -I path=$INPUT \
  --output accumulo -O instance=$INSTANCE table=$table user=$USER password=$PASSWORD zookeeper=$ZOOKEEPER \
  --layer $LAYERNAME --pyramid --crs $CRS

  break

done
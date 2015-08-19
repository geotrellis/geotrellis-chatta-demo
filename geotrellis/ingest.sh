#!/usr/bin/env bash
# Ingest tiled GeoTiff into Accumulo

# Geotrellis (gt-admin) ingest jar
JAR=~/Documents/geotrellis/gt-admin/ingest/target/scala-2.10/gt-admin-ingest-assembly-0.1.0-SNAPSHOT.jar

# Directory with the input tiled GeoTiff's
LAYERS=data/arg_wm

# Table to store tiles
TABLE=chattanooga

# Destination spatial reference system
CRS=EPSG:3857

# Pyramid the raster up to larger zoom levels
PYRAMID=true

# Delete the HDFS data for the layer if it already exists
CLOBBER=true

# Accumulo conf
INSTANCE=GIS
USER=root
PASSWORD=secret
ZOOKEEPER=localhost

# Go through all layers and run the spark submit job
for LAYER in $(ls $LAYERS)
do

  LAYERNAME=${LAYER%.*}
  INPUT=file:$(realpath $LAYERS/$LAYER)

  echo "spark-submit \
  --class geotrellis.admin.ingest.AccumuloIngestCommand $JAR \
  --instance $INSTANCE --user $USER --password $PASSWORD --zookeeper $ZOOKEEPER \
  --pyramid $PYRAMID --clobber $CLOBBER \
  --crs $CRS --table $TABLE \
  --layerName $LAYERNAME --input $INPUT
  "

done
#!/usr/bin/env bash

realpath ()
{
    f=$@;
    if [ -d "$f" ]; then
        base="";
        dir="$f";
    else
        base="/$(basename "$f")";
        dir=$(dirname "$f");
    fi;
    dir=$(cd "$dir" && /bin/pwd);
    echo "$dir$base"
}

# Ingest tiled GeoTiff into Accumulo

# Geotrellis (gt-admin) ingest jar
export JAR="target/scala-2.10/GeoTrellis-Tutorial-Project-assembly-0.1-SNAPSHOT.jar"

# Directory with the input tiled GeoTiff's
LAYERS="./data/arg_wm"

# Table to store tiles
TABLE="chattanooga"

# Destination spatial reference system
CRS="EPSG:3857"

LAYOUT_SCHEME="tms"

# Cassandra conf
HOST="localhost"
USER=""
PASSWORD=""
KEYSPACE="geotrellis"

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
  --output cassandra -O host=$HOST table=$TABLE user=$USER password=$PASSWORD keyspace=$KEYSPACE \
  --layer $LAYERNAME --pyramid --crs $CRS --layoutScheme $LAYOUT_SCHEME"

  spark-submit \
  --class geotrellis.chatta.ChattaIngest --driver-memory=2G $JAR \
  --input hadoop --format geotiff --cache NONE -I path=$INPUT \
  --output cassandra -O host=$HOST table=$TABLE user=$USER password=$PASSWORD keyspace=$KEYSPACE \
  --layer $LAYERNAME --pyramid --crs $CRS --layoutScheme $LAYOUT_SCHEME

done

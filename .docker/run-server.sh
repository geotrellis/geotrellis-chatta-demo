#!/bin/bash

docker exec -it geotrellischattademo_geodocker-spark-master_1 bash -c ". ~/.bashrc; cd /data/geotrellis-chatta-demo/geotrellis && java -cp target/scala-2.10/GeoTrellis-Tutorial-Project-assembly-0.1-SNAPSHOT.jar geotrellis.chatta.Main"

#!/bin/bash

docker exec -it geotrellischattademo_geodocker-spark-master_1 bash -c ". ~/.bashrc; cd /data/geotrellis-chatta-demo/geotrellis && mv data/arg_wm /data/arg_wm && ./ingest.sh"

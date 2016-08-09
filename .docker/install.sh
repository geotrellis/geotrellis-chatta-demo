#!/bin/bash

docker exec -it geotrellischattademo_geodocker-spark-master_1 bash -c ". ~/.bashrc && yum install -y git && \
                                  cd /data/geotrellis-chatta-demo; rm -rf geotrellis/src/main/resources && mv .docker/fs/resources geotrellis/src/main/resources && \
                                  cd /data; rm -rf geotrellis; git clone https://github.com/pomadchin/geotrellis.git && cd geotrellis && git checkout feature/etl-json && \
                                  cd /data/geotrellis; ./scripts/publish-local.sh && cd ../ && \
                                  cd /data/geotrellis-chatta-demo/geotrellis; ./sbt assembly"

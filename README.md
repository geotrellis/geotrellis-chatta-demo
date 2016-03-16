# GeoTrellis Chattanooga model demo 

This is a demo of GeoTrellis functionality. Demo consists of two parts: ingest process and demo server to query ingested data.

To run ingest, use `./ingest.sh`, to run server, use `./sbt run`. Then navigate to `http://locahost:8777/` to view the web map. 

Spray server application has two main services:

## Weighted Overlay:

The service at 'gt/wo' is a WMS layer service that gets passed a series of layer names and weights, and returns a tile PNG of the weighted overlay. It also takes the breaks that were computed using the 'gt/breaks' service. If the 'mask' option is set to a polygon, only the area under the polygon will be painted in the PNGs.

## Zonal summary:

This service at 'gt/sum' takes the same layers and weights, but also a polygon. It will compute a weighted summary of the area under the polygon.

## Runing demo using [GeoDocker cluster](https://github.com/geotrellis/geodocker-cluster)

To compile and run this demo, we prepared a development environment. 

* Clone GeoDocker cluster repository: 
  ```bash
    git clone https://github.com/geotrellis/geodocker-cluster ./
  ```

* To run cluster:
  ```bash
    cd ./geodocker-cluster/nodes; ./start-cluster.sh -n=2 # n >= 1, nodes amount
  ```
  
  To check that cluster is operating normally check pages availability: 
  * Hadoop http://localhost:50070/
  * Accumulo http://localhost:50095/
  * Spark http://localhost:8080/
  
  To check containers status is possible using following command:

  ```bash
  docker ps -a | grep geodocker 
  ```
  Runing containers have names `master1`, `slave1`, ..., `slaveN`, `N = n - 1`.
  
* Install and run this demo on cluster
  ```bash
    cd ./geodocker-cluster/install/geotrellis
    ./install.sh
    ./ingest.sh # to ingest
    ./run.sh    # to run server on a cluster
  ```

  This demo would be installed into `/data` directory, inside the container.

# GeoTrellis Chattanooga model demo 

This is a demo of GeoTrellis functionality. Demo consists of two parts: tile ingest process and demo server to query ingested data.

To run ingest, use `./ingest.sh`, to run server, use `./sbt run`. Web map would be available here `http://locahost:8777/`. 

## Short description

Demo covers [Chattanooga](https://goo.gl/S2qPCO) with different `Byte` tiles. In fact it's a `Bit` type (each tile has values `{0, 1}`). 
Each tile is ingests into it's own layer and the result map consists of layers combination with different weights (it is called weighted overlay).  

### API routes:

* `gt/colors`             - [Color Ramps](#color-ramps)
* `gt/breaks`             - [Color Breaks](#color-breaks)
* `gt/tms/{zoom}/{x}/{y}` - [Weighted Overlay](#weighted-overlay)
* `gt/sum`                - [Zonal Summary](#zonal-summary)

### Color Ramps

List of available color ramps to color weighted overlay: 
 
* `blue-to-orange`
* `green-to-orange`
* `blue-to-red`
* `green-to-red-orange`
* `light-to-dark-sunset`
* `light-to-dark-green`
* `yellow-to-red-heatmap`
* `blue-to-yellow-to-red-heatmap`
* `dark-red-to-yellow-heatmap`
* `purple-to-dark-purple-to-white-heatmap`
* `bold-land-use-qualitative`
* `muted-terrain-qualitative`

### Color Breaks

*Get Parameters:* `layers`, `weights`, `numBreaks`.

Calculates breaks for combined layers by weights with specified breaks amount.

### Weighted Overlay:

*Get Parameters:* `layers`, `weights`, `breaks`, `bbox`, `colors: [default: 4]`, `colorRamp: [default: "blue-to-red"]`, `mask`.

It is a TMS layer service that gets `{zoom}/{x}/{y}`, passed a series of layer names and weights, and returns PNG tms tiles of the weighted overlay. 
It also takes the breaks that were computed using the `gt/breaks` service. 
If the `mask` option is set to a polygon, `{zoom}/{x}/{y}` tiles masked by polygon would be returned.

### Zonal Summary:

*Get Parameters:* `polygon`, `layers`, `weights`.

This service takes layers, weights and a polygon. 
It will compute a weighted summary of the area under the polygon.

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
  * Hadoop [http://localhost:50070/](http://localhost:50070/)
  * Accumulo [http://localhost:50095/](http://localhost:50095/)
  * Spark [http://localhost:8080/](http://localhost:8080/)
  
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

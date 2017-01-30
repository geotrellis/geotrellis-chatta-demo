# GeoTrellis Chattanooga model demo

This is a demo of GeoTrellis functionality. The demo consists of two parts:
the tile ingest process and demo server to query ingested data.

Usage
-----

See the `Makefile` for full details.

Command | Action
------- | -------
`make build` | Build ingest/server code
`make ingest` | Ingest data for use by server
`make server` | Start a test server at localhost:8777
`make image` | Generate a Docker image for deployment

## Details

The demo covers [Chattanooga](https://goo.gl/S2qPCO) with different `Byte`
tiles. (In fact each tile is essentially of type `Bit` because they only
contain the  values `{0, 1}`). Each tile is ingests into it's own layer, and
the resulting map consists of layers which consist of combinations of
differently-weighted source layers (a weighted overlay).

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

It is a TMS layer service that gets `{zoom}/{x}/{y}`, passed a series of
layer names and weights, and returns PNG TMS tiles of the weighted overlay.
It also takes the breaks that were computed using the `gt/breaks` service.
If the `mask` option is set to a polygon, `{zoom}/{x}/{y}` tiles masked by
that polygon would be returned.

### Zonal Summary:

*Get Parameters:* `polygon`, `layers`, `weights`.

This service takes layers, weights and a polygon.
It will compute a weighted summary of the area under the polygon.

## Running Demo with [GeoDocker Cluster](https://github.com/geodocker/geodocker)

Quick clarification:

* Ingest requires Spark usage.
* Server works without Spark (uses GeoTrellis Collections API).

This description is a bit more generic, and describes dependent Spark server run.

To compile and run this demo, we prepared an
[environment](https://github.com/geodocker/geodocker). To run cluster we
have a slightly-modified [docker-compose.yml](docker-compose.yml) file:

* To run cluster:
  ```bash
    docker-compose up
  ```

  To check that cluster is operating normally check the availability of these pages:
  * Hadoop [http://localhost:50070/](http://localhost:50070/)
  * Accumulo [http://localhost:50095/](http://localhost:50095/)
  * Spark [http://localhost:8080/](http://localhost:8080/)

  To check containers status is possible using following command:

  ```bash
  docker ps -a | grep geodocker
  ```

 More information avaible in a [GeoDocker cluster](https://github.com/geodocker/geodocker) repo

* Install and run this demo using [GeoDocker cluster](https://github.com/geodocker/geodocker)

  * Modify [application.conf](geotrellis/src/main/resource/application.conf) (working conf example for GeoDocker cluster):

    ```conf
      geotrellis {
        port = 8777
        server.static-path = "../static"
        hostname = "spark-master"
        backend  = "accumulo"
      }

      accumulo {
        instance   = "accumulo"
        user       = "root"
        password   = "GisPwd"
        zookeepers = "zookeeper"
      }
    ```

  * Modify [backend-profiles.json](geotrellis/conf/backend-profiles.json) (working conf example for GeoDocker cluster):

    ```json
      {
        "name": "accumulo-local",
        "type": "accumulo",
        "zookeepers": "zookeeper",
        "instance": "accumulo",
        "user": "root",
        "password": "GisPwd"
      }
    ```

  * Copy everything into spark master container:

    ```bash
      cd ./geotrellis
      ./sbt assembly
      docker exec geotrellischattademo_spark-master_1 mkdir -p /data/target/scala-2.10/
      docker cp target/scala-2.11/GeoTrellis-Tutorial-Project-assembly-0.1-SNAPSHOT.jar geotrellischattademo_spark-master_1:/data/target/scala-2.10/GeoTrellis-Tutorial-Project-assembly-0.1-SNAPSHOT.jar
      docker cp  ../static geotrellischattademo_spark-master_1:/static
      docker cp data/arg_wm/ geotrellischattademo_spark-master_1:/data/
      docker cp conf geotrellischattademo_spark-master_1:/data/
      docker cp ingest.sh geotrellischattademo_spark-master_1:/data/
      docker cp run-server.sh geotrellischattademo_spark-master_1:/data/
    ```

    ```bash
      docker exec -it geotrellischattademo_spark-master_1 bash
      cd /data/; make ingest  # to ingest data into accumulo
      cd /data/; make server  # to run the server
    ```

  This demo would be installed into `/data` directory, inside spark master container.

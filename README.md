# GeoTrellis Chattanooga model demo

This is a demo of GeoTrellis functionality. The demo consists of two parts:
the tile ingest process and demo server to query ingested data.

## Dependencies


- Vagrant 1.9.5
- VirtualBox 5.1+
- AWS CLI 1.11+
- AWS Account (to access S3)

## Getting Started

To provision a VM and fetch our pre-ingested demo data:

```bash
$ ./scripts/setup
$ vagrant ssh
```

This will download data into `./service/geotrellis/data/chatta-demo`. See the [ingest](#ingesting-data) sections for information about ingesting data manually using either the [local filesystem](#local-ingest) or [geodocker](geodocker-ingest).

## Scripts

Helper and development scripts are located in the `./scripts` directory at the root of this project. These scripts are designed to encapsulate and perform commonly used actions such as starting a development server, accessing a development console, or running tests.

| Script Name             | Purpose                                                      |
|-------------------------|--------------------------------------------------------------|
| `update`                | Pulls/builds necessary containers                            |
| `setup`                 | Provisions the VM, fetch ingest data.                        |
| `server`                | Starts a development server that listens at http://localhost:8777. Use the `--geodocker` flag to run the server with an accumulo backend.                       |
| `console`               | Gives access to a running container via `docker-compose run`. Use the `--geodocker` flag to run with an accumulo backend |
| `test`                  | Runs tests for project                                 |
| `cibuild`               | Invoked by CI server and makes use of `test`.                |
| `cipublish`             | Build JAR and publish container images to container image repositories.    |

## Testing

Run all the tests:

```bash
$ ./scripts/test
```

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

## Ingesting Data

Quick clarification:

* Ingest requires Spark usage.
* Server works without Spark (uses GeoTrellis Collections API).

This section includes instructions on how to do local filesystem and Geodocker ingests to either the local filesystem, or to Accumulo (using [Geodocker](https://github.com/geodocker/geodocker)).

### Local ingest
In the event that you need to run a local ingest, the `gt-chatta-ingest` container will run a `spark-submit` job that writes ingest data to the local filesystem. Make sure the Chatta Demo JAR has been built, then run the container:

```bash
$ docker-compose run --rm gt-chatta assembly
$ docker-compose build gt-chatta-ingest
$ docker-compose run --rm gt-chatta-ingest
```

Data will be installed into `./service/geotrellis/data/chatta-demo`, which is mounted at `/data/chatta-demo` inside of the `gt-chatta` container.

### Geodocker Ingests
To simulate running this demo in a distibuted environment, we prepared a Geodocker cluster including Hadoop, Accumulo and Spark. The application is configured for the geodocker setup using the `application.conf` file in the `geodocker/` folder. ***Make sure you build an accumulo-configured JAR with `make build-geodocker` before attempting a Geodocker ingest.*** 

* To run cluster:
  ```bash
    docker-compose up
  ```

  To check that cluster is operating normally check the availability of these pages:
  * Hadoop [http://localhost:50070/](http://localhost:50070/)
  * Accumulo [http://localhost:50095/](http://localhost:50095/)
  * Spark [http://localhost:8080/](http://localhost:8080/)

To check containers status, use the following command:

```bash
docker-compose -f docker-compose.geodocker.yml ps
```


* Install and run this demo using [GeoDocker cluster](https://github.com/geodocker/geodocker)
  
  * Running a Geodocker ingest will require more memory for the Vagrant VM. Before running an ingest, set `GT_CHATTA_VM_MEMORY=6144` (or a higher value) and run `vagrant reload`.

  * Build the accumulo-configured geotrellis JAR, run an ingest, then start the server.
    ```bash
      make build-geodocker
      make ingest-geodocker
      ./scripts/server --geodocker
    ```

The demo catalog will be available through the accumulo backend. More information avaible is available in the [GeoDocker cluster](https://github.com/geodocker/geodocker) repo.

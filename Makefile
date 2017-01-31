# Makefile to simplify project interaction.
TAG := $(shell git rev-parse --short HEAD)
JAR := target/scala-2.11/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar

clean:
	cd geotrellis && ./sbt clean

build:
	cd geotrellis && ./sbt assembly

# Local ingest, requires a valid Spark installation.
ingest: geotrellis/${JAR}
	spark-submit \
		--class geotrellis.chatta.ChattaIngest --driver-memory=4G geotrellis/${JAR} \
		--input "file:///${PWD}/geotrellis/conf/input.json" \
		--output "file://${PWD}/geotrellis/conf/output.json" \
		--backend-profiles "file://${PWD}/geotrellis/conf/backend-profiles.json"

# Perform a reliable ingest via Docker.
ingest-docker: geotrellis/${JAR}
	cd geotrellis && docker build -t chatta-ingest .
	docker run --rm -v ${PWD}/geotrellis/data:/data chatta-ingest:latest

# `-noverify` is to get around some strange Guava dependency problems.
server: geotrellis/${JAR}
	cd geotrellis && java -noverify -cp ${JAR} geotrellis.chatta.Main

image: geotrellis/${JAR}
	docker build -t geotrellis-chatta-demo:${TAG} .

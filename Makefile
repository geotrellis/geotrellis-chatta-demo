# Makefile to simplify the creation of the Docker image
TAG := $(shell git rev-parse --short HEAD)
JAR := target/scala-2.11/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar

clean:
	cd geotrellis && ./sbt clean

build:
	cd geotrellis && ./sbt assembly

ingest: geotrellis/${JAR}
	cd geotrellis && docker build -t chatta-ingest .
	docker run --rm -v ${PWD}/geotrellis/data:/data chatta-ingest:latest

# `-noverify` is to get around some strange Guava dependency problems.
server: geotrellis/${JAR}
	cd geotrellis && java -noverify -cp ${JAR} geotrellis.chatta.Main

image: geotrellis/${JAR}
	docker build -t geotrellis-chatta-demo:${TAG} .

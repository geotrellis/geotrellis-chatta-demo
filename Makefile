# Makefile to simplify project interaction.
TAG := $(shell git rev-parse --short HEAD)
JAR := target/scala-2.11/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar

build-geodocker:
	docker-compose -f docker-compose.geodocker.yml run --rm \
		--no-deps spark-master ./sbt assembly

# Perform a reliable ingest via Geodocker.
ingest-geodocker: service/geotrellis/${JAR}
	docker-compose -f docker-compose.geodocker.yml up -d
	docker-compose -f docker-compose.geodocker.yml exec spark-master \
		spark-submit --class geotrellis.chatta.ChattaIngest \
		${JAR} \
		--input "file:///data/geotrellis/conf/input.json" \
		--output "file:///data/geotrellis/conf/output-accumulo.json" \
		--backend-profiles "file:///data/geotrellis/conf/backend-profiles.json"

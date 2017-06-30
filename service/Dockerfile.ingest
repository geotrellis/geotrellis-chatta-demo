FROM quay.io/azavea/spark:2.0.2

# Must be `root` or else writes to mounted volume will fail.
USER root

VOLUME /data

COPY geotrellis/target/scala-2.11/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar /
COPY geotrellis/conf/input.json /
COPY geotrellis/conf/output.json /
COPY geotrellis/conf/backend-profiles.json /

CMD spark-submit \
    --class geotrellis.chatta.ChattaIngest --driver-memory=4G /geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar \
    --input "file:///input.json" \
    --output "file:///output.json" \
    --backend-profiles "file:///backend-profiles.json"

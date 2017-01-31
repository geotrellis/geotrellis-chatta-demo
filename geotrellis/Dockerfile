FROM quay.io/azavea/spark:2.0.2

USER root

VOLUME /data

ADD target/scala-2.11/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar /
ADD conf/input.json /
ADD conf/output.json /
ADD conf/backend-profiles.json /

CMD spark-submit \
    --class geotrellis.chatta.ChattaIngest --driver-memory=4G /geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar \
    --input "file:///input.json" \
    --output "file:///output.json" \
    --backend-profiles "file:///backend-profiles.json"

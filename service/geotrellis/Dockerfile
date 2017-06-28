FROM openjdk:8-jre

COPY service/geotrellis/target/scala-2.11/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar /opt/
COPY service/geotrellis/data/chatta-demo /data/chatta-demo
COPY service/static/ /static

EXPOSE 8777

CMD java -cp /opt/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar geotrellis.chatta.Main

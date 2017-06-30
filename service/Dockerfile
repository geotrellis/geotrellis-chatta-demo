FROM openjdk:8-jre

COPY geotrellis/target/scala-2.11/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar /opt/
COPY geotrellis/data/chatta-demo /data/chatta-demo
COPY static/ /static

EXPOSE 8777

ENTRYPOINT ["java"] 
CMD ["-cp", "/opt/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar", "geotrellis.chatta.Main"]

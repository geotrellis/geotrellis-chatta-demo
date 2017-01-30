FROM openjdk:8-jre

ADD geotrellis/target/scala-2.11/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar /opt/
ADD geotrellis/data/chatta-demo /data/chatta-demo
ADD static/ /static

EXPOSE 8777

CMD java -noverify -Dgeotrellis.hostname=chatta -cp /opt/geotrellis-chatta-demo-assembly-0.1-SNAPSHOT.jar geotrellis.chatta.Main

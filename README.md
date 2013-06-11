geotrellis chattanooga model demo 
=================================

Starting up multiple instances on the same machine (for testing)
./sbt -Dgeotrellis.port=8081 -Dgeotrellis.akka-port=2552
./sbt -Dgeotrellis.port=8082 -Dgeotrellis.akka-port=2553


Starting up multiple instances of different machines

There must be one server running on a known IP address so new machines can
join the cluster -- this gateway to the cluster is known as the cluster seed.

./sbt -Dgeotrellis.cluster_seed_ip=xxxx  


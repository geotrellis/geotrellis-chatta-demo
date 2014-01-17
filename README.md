geotrellis chattanooga model demo 
=================================

This is a demo of GeoTrellis functionality. To run, use `./sbt run`. Then navigate to `http://locahost:8777/` to view the web map. 

It's a spray server application that has two main services:

Weighted Overlay:
-----------------

The service at 'gt/wo' is a WMS layer service that gets passed a series of layer names and weights, and returns a tile PNG of the weighted overlay. It also takes the breaks that were computed using the 'gt/breaks' service. If the 'mask' option is set to a polygon, only the area under the polygon will be painted in the PNGs.

Zonal summary:
--------------

This service at 'gt/sum' takes the same layers and weights, but also a polygon. It will compute a weighted summary of the area under the polygon.

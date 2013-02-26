// Map
var map = L.map('map').setView([39.9522, -75.1642], 13);

L.tileLayer('http://{s}.tiles.mapbox.com/v3/azavea.map-zbompf85/{z}/{x}/{y}.png', {
	maxZoom: 18,
	attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery &copy; <a href="http://mapbox.com">MapBox</a>'
}).addTo(map);

// Drawing

var drawnItems = new L.FeatureGroup();
map.addLayer(drawnItems);

var drawControl = new L.Control.Draw({
    draw: {
	position: 'topleft',
        marker: false,
        polyline: false,
        rectangle: false,
//        circle: ,
	polygon: {
	    title: 'Draw a sexy polygon!',
	    allowIntersection: false,
	    drawError: {
		color: '#b00b00',
		timeout: 1000
	    },
	    shapeOptions: {
		color: '#bada55'
	    }
	},
    },
    edit: {
	featureGroup: drawnItems
    }
});
map.addControl(drawControl);

map.on('draw:created', function (e) {
    var type = e.layerType,
    layer = e.layer;

    if (type === 'polygon') {
        alert(GJ.fromPolygon(layer))
    }

    drawnItems.addLayer(layer);
});
	
// Sliders
  $(function() {
    $( ".slider" ).slider({
      value:0,
      min: 0,
      max: 10,
      step: 1,
      slide: function( event, ui ) {
        $( this ).prev('.weight').text( "+" + ui.value );
      }
    });
    $( '.weight' ).text( "+0" );
  });
  
 // Reset
$('.reset').on("click", function() {
	$( ".slider" ).slider( "value", 0 );
	$( '.weight' ).text( "+0" );
});

// Majuro Drawing option
var lat = 39.938435;
var lng = -75.136528;

// add a sample neighborhood area and make it editable
var wll = [ new L.LatLng(lat - 0.002896, lng - 0.02983), new L.LatLng(lat - 0.003817, lng + 0.022447), new L.LatLng(lat + 0.003508, lng - 0.026852) ];
footprint = new L.Polygon( wll, { color: "#00f", fillOpacity: 0.3, opacity: 0.65 } );
footprint.editing.enable();
map.addLayer(footprint);

// add a draggable marker to translate the shape
lat -= 0.001;
lng -= 0.004;

var ctrIcon = new L.Icon({
    iconSize: [40, 40],
    iconAnchor: [20, 20],
    shadowSize: [0, 0],
    iconUrl: "img/4-way-arrow.png"
});
var ctrmrk = new L.marker(new L.LatLng(lat,lng), { draggable: true, icon: ctrIcon });
ctrmrk.on('dragend', function(e){
    var latdiff = ctrmrk.getLatLng().lat - lat;
    var lngdiff = ctrmrk.getLatLng().lng - lng;
    var latlngs = footprint.getLatLngs();
    for(var pt=0;pt<latlngs.length;pt++){
        latlngs[pt] = new L.LatLng( latlngs[pt].lat + latdiff, latlngs[pt].lng + lngdiff );
    }
    map.removeLayer(footprint);
    footprint = new L.Polygon( latlngs, { color: "#00f", fillOpacity: 0.3, opacity: 0.65 } );
    footprint.editing.enable();
    map.addLayer(footprint);

    lat = ctrmrk.getLatLng().lat;
    lng = ctrmrk.getLatLng().lng;
    
    // re-center the center marker when editing occurs
    footprint.on('edit', function(e){
        var latlngs = footprint.getLatLngs();
        var avglat = 0;
        var avglng = 0;
        for(var pt=0;pt<latlngs.length;pt++){
            avglat += latlngs[pt].lat;
            avglng += latlngs[pt].lng;
        }
        lat = avglat / latlngs.length;
        lng = avglng / latlngs.length;
        ctrmrk.setLatLng(new L.LatLng(lat,lng));
    });
});
// re-center the center marker when editing occurs
footprint.on('edit', function(e){
    var latlngs = footprint.getLatLngs();
    var avglat = 0;
    var avglng = 0;
    for(var pt=0;pt<latlngs.length;pt++){
        avglat += latlngs[pt].lat;
        avglng += latlngs[pt].lng;
    }
    lat = avglat / latlngs.length;
    lng = avglng / latlngs.length;
    ctrmrk.setLatLng(new L.LatLng(lat,lng));
});
map.addLayer(ctrmrk);

function llserial(latlngs){
  var llstr = [ ];
  for(var i=0;i<latlngs.length;i++){
    llstr.push(latlngs[i].lat.toFixed(6) + "," + latlngs[i].lng.toFixed(6));
  }
  return llstr.join("|");
}

function postGeo(format){
  var poly = llserial(footprint.getLatLngs());
  $.post("/customgeo", { pts: poly }, function(data){
    if(format == "html"){
      if(src){
        window.location = "/build/" + src + "/" + data.id;
      }
      else{
        window.location = "/build/" + data.id;      
      }
    }
    else if(format == "time"){
      if(src){
        window.location = "/timeline/" + src + "/" + data.id;
      }
      else{
        window.location = "/timeline/" + data.id;      
      }
    }
    else if(format == "clusters"){
      if(src){
        window.location = "/timeclusters/" + src + "/" + data.id;
      }
      else{
        window.location = "/timeclusters/" + data.id;
      }
    }
    else if(format == "3d"){
      var minlat = 90;
      var centerlng = 0;
      var pts = footprint.getLatLngs();
      for(var p=0;p<pts.length;p++){
        minlat = Math.min( minlat, pts[p].lat );
        centerlng += pts[p].lng;
      }
      centerlng /= pts.length;
      if(src){
        window.location = "/explore3d/" + src + "/" + data.id;
      }
      else{
        window.location = "/explore3d/" + data.id;
      }
    }
    else if(format == "geojson"){
      if(src){
        window.location = "/timeline-at/" + src + "/" + data.id + ".geojson";
      }
      else{
        window.location = "/timeline-at/" + data.id + ".geojson";      
      }
    }
    else if(format == "kml"){
      if(src){
        window.location = "/timeline-at/" + src + "/" + data.id + ".kml";
      }
      else{
        window.location = "/timeline-at/" + data.id + ".kml";
      }
    }
  });
}

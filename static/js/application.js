// Map
var map = L.map('map').setView([34.76192255039478,-85.35140991210938], 9);

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
        circle: false,
	polygon: {
	    title: 'Draw a polygon.',
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
        // Perform call to GeoTrellis
        var geoJson = GJ.fromPolygon(layer);
        $.post('gt/sum', geoJson,
               function(data) {
                   alert("Data loaded:" + JSON.stringify(data));
               }, "json");
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

var d = null;
function updateWMS() {
    if (d) {
        map.removeLayer(d);
    }

    d = new L.TileLayer.WMS("gt/wo", {
        layers: 'default',
        format: 'image/png',
        breaks: breaks,
        transparent: true,
        attribution: 'Azavea',
    })

    d.setOpacity(.5);

    d.addTo(map);
}

var breaks = null;
$.getJSON('gt/breaks?', function(data) {
    breaks = data.classBreaks;
    updateWMS();
});

//var button = document.getElementById('update')
//button.addEventListener('click', updateWMS);
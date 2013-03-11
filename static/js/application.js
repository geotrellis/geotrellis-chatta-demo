var demo = {};

demo.layers = [ { name : "ImperviousSurfaces_Barren Lands_Open Water", 
                  weight : 1,
                  display : "Impervious Surfaces" },
                { name: "DevelopedLand",
                  weight: "2",  
                  display: "Developed Land"},
                { name: "Wetlands",
                  weight: "3",  
                  display: "Wetlands"},
                { name: "ForestedLands",
                  weight: "4",  
                  display: "Forested Lands"},
                { name: "Non-workingProtectedOrPublicLands",
                  weight: "5",  
                  display: "Non-working Protected or Public Lands"},
                { name: "PrimeAgriculturalSoilsNotForestedOrFarmland",
                  weight: "6",  
                  display: "Prime Agricultural Soils, Not Forested or Farmland"},
                { name: "PublicallyOwnedWorkingLands",
                  weight: "7",  
                  display: "Publically Owned Working Lands"},
                { name: "PrivatelyOwnedWorkingLandsWithEasements", 
                  weight: "8",  
                  display: "Privately Owned Working Lands, with Easements"},
                { name: "FarmlandWithoutPrimeAgriculturalSoils",  
                  weight: "9",  
                  display: "Farmland, without prime Agricultural Soils"},
                { name: "FarmlandOrForestedLandsWithPrimeAgriculturalSoils",
                  weight: "10",  
                  display: "Farmland or Forested Lands, with prime Agricultural Soils"}
              ];

demo.getLayers   = function() {
    var notZeros = _.filter(demo.layers, function(l) { return l.weight != 0 });
    return _.map(notZeros, function(l) { return l.name; }).join(",");
};

demo.getWeights   = function() {
    var notZeros = _.filter(demo.layers, function(l) { return l.weight != 0 });
    return _.map(notZeros, function(l) { return l.weight; }).join(",");
};

demo.breaks = null;
demo.WOLayer = null;

demo.updateWO = function() {
    $.ajax({
        url: 'gt/breaks',
        data: { 'layers' : demo.getLayers(), 'weights' : demo.getWeights() },
        dataType: "json",
        success: function(r) {
            demo.breaks = r.classBreaks;
            demo.updateWMS();
        }
    });
};

demo.updateWMS = function() {
    if (demo.WOLayer) {
        map.removeLayer(demo.WOLayer);
    }

    var layers = demo.getLayers();
    if(layers == "") return;

    demo.WOLayer = new L.TileLayer.WMS("gt/wo", {
        layers: 'default',
        format: 'image/png',
        breaks: demo.breaks,
        transparent: true,
        layers: layers,
        weights: demo.getWeights(),
        attribution: 'Azavea',
    })

    demo.WOLayer.setOpacity(.5);

    demo.WOLayer.addTo(map);
}

// On page load
$(document).ready(function() {
    var pList = $("#parameters");
    pList.empty();
    
    _.map(demo.layers, function(l) {
        var p = $("#parameterSlider").clone();
        p.find(".slider-label").text(l.display);
        p.show();
        pList.append(p);
        makeSlider(p,l);
    });

    demo.updateWO();
});

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
        $.ajax({        
            url: 'gt/sum',
            data: { polygon : geoJson, 
                    layers  : demo.getLayers(), 
                    weights : demo.getWeights() 
                  },
            dataType: "json",
            success : function(data) {
                alert("Data loaded:" + JSON.stringify(data));
            }});
    }

    drawnItems.addLayer(layer);
});
	
// Sliders
var makeSlider = function(div,layer) {
    div.find( ".slider" ).slider({
        value:layer.weight,
        min: 0,
        max: 10,
        step: 1,
        change: function( event, ui ) {
            $( this ).prev('.weight').text( "+" + ui.value );
            layer.weight = ui.value;
            demo.updateWO();
        }
    });
    div.find( '.weight' ).text( "+" + layer.weight );
};

// Reset
$('.reset').on("click", function() {
    $( ".slider" ).slider( "value", 0 );
    $( '.weight' ).text( "+0" );
});


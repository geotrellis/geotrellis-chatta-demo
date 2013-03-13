var map = (function() {
    var m = L.map('map').setView([34.76192255039478,-85.35140991210938], 9);

    L.tileLayer('http://{s}.tiles.mapbox.com/v3/azavea.map-zbompf85/{z}/{x}/{y}.png', {
	maxZoom: 18,
	attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery &copy; <a href="http://mapbox.com">MapBox</a>'
}).addTo(m);
    return m;
})()

var weightedOverlay = (function() {
    layers = [ { name : "ImperviousSurfaces_Barren Lands_Open Water", 
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

    breaks = null;
    WOLayer = null;
    opacity = 0.5;

    getLayers   = function() {
        var notZeros = _.filter(layers, function(l) { return l.weight != 0 });
        return _.map(notZeros, function(l) { return l.name; }).join(",");
    };

    getWeights   = function() {
        var notZeros = _.filter(layers, function(l) { return l.weight != 0 });
        return _.map(notZeros, function(l) { return l.weight; }).join(",");
    };

    updateLayer = function() {
        if (WOLayer) {
            map.removeLayer(WOLayer);
        }

        var layers = getLayers();
        if(layers == "") return;

        var geoJson = "";
        if(summary.polygon != null) {
            geoJson = GJ.fromPolygon(summary.polygon);
        }

        WOLayer = new L.TileLayer.WMS("gt/wo", {
            layers: 'default',
            format: 'image/png',
            breaks: breaks,
            transparent: true,
            layers: layers,
            weights: getWeights(),
            colorRamp: colorRamp,
            mask: geoJson,
            attribution: 'Azavea'
        })

        WOLayer.setOpacity(opacity);
        WOLayer.addTo(map);
    }

    updateBreaks = function() {
        $.ajax({
            url: 'gt/breaks',
            data: { 'layers' : getLayers(), 'weights' : getWeights() },
            dataType: "json",
            success: function(r) {
                breaks = r.classBreaks;
                updateLayer();
            }
        });
    };

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
                weightedOverlay.update();
            }
        });
        div.find( '.weight' ).text( "+" + layer.weight );
    };

    // Color Ramp
    var colorRamp = "blue-to-red"
    var setColorRamp = function(key) { colorRamp = key }

    // Opacity
    $("#opacity-slider").slider({
        value: opacity,
        min: 0,
        max: 1,
        step: .02,
      slide: function( event, ui ) {
          WOLayer.setOpacity(ui.value);
        }
    });

    return {
        update: updateBreaks,
        layers: layers,
        activeLayers: getLayers,
        activeWeights: getWeights,
        makeSlider: makeSlider,
        setColorRamp: setColorRamp,
        getMapLayer: function() { return WOLayer; }
    };

})();

var summary = (function() {
    var o = { polygon: null }
    o.fetchSummary = function() {
            if(o.polygon != null) {
                var geoJson = GJ.fromPolygon(o.polygon);
                $.ajax({        
                    url: 'gt/sum',
                    data: { polygon : geoJson, 
                            layers  : weightedOverlay.activeLayers(), 
                            weights : weightedOverlay.activeWeights() 
                          },
                    dataType: "json",
                    success : function(data) {
                        var sdata = $("#summary-data");
                        sdata.empty();
                        
                        var p = $("#summaryTemplate").clone();
                        p.find("#summary-score").text(data.sum);
                        p.show();
                        sdata.append(p);
                        $('a[href=#summary]').tab('show');
                    }
                });
            }
    };
    return o;
})();

var drawing = (function() {
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
	        title: 'Draw a polygon for summary information.',
	        allowIntersection: false,
	        drawError: {
		    color: '#b00b00',
		    timeout: 1000
	        },
	        shapeOptions: {
		    color: '#338FF2'
	        }
	    },
        },
        edit: {
	    featureGroup: drawnItems,
            remove: false
        }
    });
    map.addControl(drawControl);

    map.on('draw:created', function (e) {
        var type = e.layerType,
        layer = e.layer;

        if (type === 'polygon') {
            // Perform call to GeoTrellis
            summary.polygon = layer;
            summary.fetchSummary(summary.polygon);
        }
    });

    map.on('draw:edited', function(e) {
        if(summary.polygon != null && summary.polygon == e.layer) {
            weightedOverlay.update();
            summary.fetchSummary(summary.polygon);
        }
    });

    map.on('draw:drawstart', function(e) {
        if(summary.polygon != null) { drawnItems.removeLayer(summary.polygon); }
    });

    map.on('draw:drawstop', function(e) {
        drawnItems.addLayer(summary.polygon);
    });

})();

var colorRamps = (function() {
    var makeColorRamp = function(colorDef) {
        var ramps = $("#color-ramp-menu");
        
        var p = $("#colorRampTemplate").clone();
        var img = p.find('img');
        img.attr("src",colorDef.image);
        img.click(function() {
            weightedOverlay.setColorRamp(colorDef.key);
            weightedOverlay.update();
        });
        p.show();
        ramps.append(p);
    }

    $.ajax({
        url: 'gt/colors',
        dataType: 'json',
        success: function(data) {
            _.map(data.colors, makeColorRamp)
        }
    });
})();


// On page load
$(document).ready(function() {
    var pList = $("#parameters");
    pList.empty();
    
    _.map(weightedOverlay.layers, function(l) {
        var p = $("#parameterSlider").clone();
        p.find(".slider-label").text(l.display);
        p.show();
        pList.append(p);
        weightedOverlay.makeSlider(p,l);
    });

    weightedOverlay.update();
});

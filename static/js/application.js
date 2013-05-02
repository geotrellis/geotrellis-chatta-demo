var getLayer = function(url,attrib) {
    return L.tileLayer(url, { maxZoom: 18, attribution: attrib });
};

var Layers = {
    stamen: { 
        toner:  'http://{s}.tile.stamen.com/toner/{z}/{x}/{y}.png',   
        terrain: 'http://{s}.tile.stamen.com/terrain/{z}/{x}/{y}.png',
        watercolor: 'http://{s}.tile.stamen.com/watercolor/{z}/{x}/{y}.png',
        attrib: 'Map data &copy;2013 OpenStreetMap contributors, Tiles &copy;2013 Stamen Design'
    },
    mapBox: {
        azavea:     'http://{s}.tiles.mapbox.com/v3/azavea.map-zbompf85/{z}/{x}/{y}.png',
        worldLight: 'http://c.tiles.mapbox.com/v3/mapbox.world-light/{z}/{x}/{y}.png',
        attrib: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery &copy; <a href="http://mapbox.com">MapBox</a>'
    }
};

var map = (function() {
    var selected = getLayer(Layers.mapBox.azavea,Layers.mapBox.attrib);
    var baseLayers = {
	"Azavea" : selected,
        "World Light" : getLayer(Layers.mapBox.worldLight,Layers.mapBox.attrib),
        "Terrain" : getLayer(Layers.stamen.terrain,Layers.stamen.attrib),
        "Watercolor" : getLayer(Layers.stamen.watercolor,Layers.stamen.attrib),
        "Toner" : getLayer(Layers.stamen.toner,Layers.stamen.attrib),
    };

    var m = L.map('map');

    m.setView([34.76192255039478,-85.35140991210938], 9);

    selected.addTo(m);

    m.lc = L.control.layers(baseLayers).addTo(m);
    return m;
})()

var weightedOverlay = (function() {
    var layers = [];

    var breaks = null;
    var WOLayer = null;
    var opacity = 0.5;
    var colorRamp = "blue-to-red";
    var numBreaks = 10;

    getLayers   = function() {
        var notZeros = _.filter(layers, function(l) { return l.weight != 0 });
        return _.map(notZeros, function(l) { return l.name; }).join(",");
    };

    getWeights   = function() {
        var notZeros = _.filter(layers, function(l) { return l.weight != 0 });
        return _.map(notZeros, function(l) { return l.weight; }).join(",");
    };

    update = function() {
        if(getLayers().length == 0) { 
            if (WOLayer) {
                map.lc.removeLayer(WOLayer);
                map.removeLayer(WOLayer);
                WOLayer = null;
            }
            return; 
        };

        $.ajax({
            url: 'http://207.245.89.238/gt/breaks',
            data: { 'layers' : getLayers(), 
                    'weights' : getWeights(),
                    'numBreaks': numBreaks },
            dataType: "json",
            success: function(r) {
                breaks = r.classBreaks;

                if (WOLayer) {
                    map.lc.removeLayer(WOLayer);
                    map.removeLayer(WOLayer);
                }

                var layerNames = getLayers();
                if(layerNames == "") return;

                var geoJson = "";
                var polygon = summary.getPolygon();
                if(polygon != null) {
                    geoJson = GJ.fromPolygon(polygon);
                }

                WOLayer = new L.TileLayer.WMS("http://207.245.89.238/gt/wo", {
                    layers: 'default',
                    format: 'image/png',
                    breaks: breaks,
                    transparent: true,
                    layers: layerNames,
                    weights: getWeights(),
                    colorRamp: colorRamp,
                    mask: geoJson,
                    attribution: 'Azavea'
                })

                WOLayer.setOpacity(opacity);
                WOLayer.addTo(map);
                map.lc.addOverlay(WOLayer, "Weighted Overlay");

            }
        });
    };

    // Sliders
    var makeSlider = function(div,layer) {
        div.find( ".slider" ).slider({
            value:layer.weight,
            min: -5,
            max: 5,
            step: 1,
            change: function( event, ui ) {
                var pn = ui.value > 0 ? "+" : "";
                $( this ).prev('.weight').text( pn + ui.value );
                layer.weight = ui.value;
                update();
                summary.update(false);
            }
        });
        div.find( '.weight' ).text( (layer.weight > 0 ? "+" : "") + layer.weight );
    };

    var bindSliders = function() {
        var pList = $("#parameters");
        pList.empty();
        
        _.map(layers, function(l) {
            var p = $("#parameterSlider").clone();
            p.find(".slider-label").text(l.display);
            p.show();
            pList.append(p);
            makeSlider(p,l);
        });

        update();
    };

    // Opacity
    var opacitySlider = $("#opacity-slider").slider({
        value: opacity,
        min: 0,
        max: 1,
        step: .02,
        slide: function( event, ui ) {
          opacity = ui.value;
          WOLayer.setOpacity(opacity);
        }
    });

    return {
        activeLayers: getLayers,
        activeWeights: getWeights,
        
        bindSliders : bindSliders,

        setLayers: function(ls) { 
            layers = ls; 
            bindSliders();
            update(); 
        },
        setNumBreaks: function(nb) {
            numBreaks = nb;
            update();
        },
        setOpacity: function(o) {
            opacity = o;
            opacitySlider.slider('value', o);
        },
        setColorRamp: function(key) { 
            colorRamp = key;
            update();
        },
        getColorRamp: function() { return colorRamp; },

        update: update,

        getMapLayer: function() { return WOLayer; }
    };

})();

var summary = (function() {
    var polygon = null;
    var layers = {};
    var update = function(switchTab) {
        if(polygon != null) {
            if(weightedOverlay.activeLayers().length == 0) {
                $("#summary-data").empty();                
                return;
            };

            var geoJson = GJ.fromPolygon(polygon);
            $.ajax({        
                url: 'http://207.245.89.238/gt/sum',
                data: { polygon : geoJson, 
                        layers  : weightedOverlay.activeLayers(), 
                        weights : weightedOverlay.activeWeights() 
                      },
                dataType: "json",
                success : function(data) {
                    var sdata = $("#summary-data");
                    sdata.empty();

                    _.map(data.layerSummaries, function(ls) {
                        if(layers.hasOwnProperty(ls.layer)) {
                            var layerName = layers[ls.layer];
                        } else {
                            var layerName = "Layer:";
                        }

                        sdata.append($('<tr><td>10</td<td>' + layerName + '</td>' + '<td class="bold" style="text-align:right;">' + ls.total + '</td></tr>'));
                    });

                    sdata.append($('<tr class="warning"><td class="bold">Score:</td>' + '<td class="bold" style="text-align:right;">' + data.total + '</td></tr>'));

                    if(switchTab) { $('a[href=#summary]').tab('show'); };
                }
            });
        }
    };
    return {
        getPolygon: function() { return polygon; },
        setPolygon: function(p) { 
            polygon = p; 
            weightedOverlay.update();
            update(true);
        },
        setLayers: function(ls) {
            _.map(ls, function(l) {
                layers[l.name] = l.display;
            });
        },
        update: update,
        clear: function() {
            if(polygon) {
                drawing.clear(polygon);
                polygon = null;
                weightedOverlay.update();
                $('a[href=#parameters]').tab('show');
                $("#summary-data").empty();
            }
        }
    };
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
        if (e.layerType === 'polygon') {
            summary.setPolygon(e.layer);
        }
    });

    map.on('draw:edited', function(e) {
        var polygon = summary.getPolygon();
        if(polygon != null) { 
            summary.update();
            weightedOverlay.update();
        }
    });

    map.on('draw:drawstart', function(e) {
        var polygon = summary.getPolygon();
        if(polygon != null) { drawnItems.removeLayer(polygon); }
    });

    map.on('draw:drawstop', function(e) {
        drawnItems.addLayer(summary.getPolygon());
    });

    return {
        clear: function(polygon) {
            drawnItems.removeLayer(polygon);
        }
    }
})();

var colorRamps = (function() {
    var makeColorRamp = function(colorDef) {
        var ramps = $("#color-ramp-menu");
        var p = $("#colorRampTemplate").clone();
        p.find('img').attr("src",colorDef.image);
        p.click(function() {
            $("#activeRamp").attr("src",colorDef.image);
            weightedOverlay.setColorRamp(colorDef.key);
        });
        if(colorDef.key == weightedOverlay.getColorRamp()) {
            $("#activeRamp").attr("src",colorDef.image);
        }
        p.show();
        ramps.append(p);
    }

    return { 
        bindColorRamps: function() {
            $.ajax({
                url: 'http://207.245.89.238/gt/colors',
                dataType: 'json',
                success: function(data) {
                    _.map(data.colors, makeColorRamp)
                }
            });
        }
    }
})();

// Set up from config
$.getJSON('config.json', function(data) {
    summary.setLayers(data.weightedOverlay.layers);
    weightedOverlay.setLayers(data.weightedOverlay.layers);
    weightedOverlay.setNumBreaks(data.weightedOverlay.numBreaks);
    weightedOverlay.setOpacity(data.weightedOverlay.opacity);
    weightedOverlay.setColorRamp(data.weightedOverlay.ramp);
});

var setupSize = function() {
    var bottomPadding = 10;

    var resize = function(){
        var pane = $('#main');
        var height = $(window).height() - pane.offset().top - bottomPadding;
        pane.css({'height': height +'px'});

        var sidebar = $('#tabBody');
        var height = $(window).height() - sidebar.offset().top - bottomPadding;
        sidebar.css({'height': height +'px'});

        var mapDiv = $('#map');
		var wrapDiv = $('#wrap');
        var height = $(window).height() - mapDiv.offset().top - bottomPadding - wrapDiv.height();
        mapDiv.css({'height': height +'px'});
        map.invalidateSize();
    };
    resize();
    $(window).resize(resize);
};

// On page load
$(document).ready(function() {
    // Set heights

    weightedOverlay.bindSliders();
    colorRamps.bindColorRamps();

    $('#clearButton').click( function() {
        summary.clear();
        return false;
    });
    setupSize();
});

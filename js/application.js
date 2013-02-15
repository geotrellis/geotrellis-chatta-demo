// Map
var map = L.map('map').setView([39.9522, -75.1642], 13);

L.tileLayer('http://{s}.tiles.mapbox.com/v3/azavea.map-zbompf85/{z}/{x}/{y}.png', {
	maxZoom: 18,
	attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery &copy; <a href="http://mapbox.com">MapBox</a>'
}).addTo(map);
	
// Sliders
  $(function() {
    $( ".slider" ).slider({
      value:0,
      min: 0,
      max: 10,
      step: 1,
      slide: function( event, ui ) {
        $( "#amount" ).val( "+" + ui.value );
      }
    });
    $( "#amount" ).val( "+" + $( ".slider" ).slider( "value" ) );
  });
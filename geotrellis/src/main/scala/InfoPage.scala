package chatta

object InfoPage {
    def infoPage(cols:String, rows:String, ms:Long, url:String, tree:String) = 
s"""
<html>
<head>
 <script type="text/javascript">
 </script>
</head>
<body>
 <h2>raster time!</h2>

 <h3>rendered $cols x $rows image (${cols.toInt*rows.toInt} pixels) in $ms ms</h3>

 <table>
  <tr>
   <td style="vertical-align:top"><img style="vertical-align:top" src="$url" /></td>
   <td><pre>$tree</pre></td>
  </tr>
 </table>

</body>
</html>
"""
}

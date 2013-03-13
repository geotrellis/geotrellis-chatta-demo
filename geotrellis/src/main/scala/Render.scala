package chatta

import geotrellis._
import geotrellis.data.ColorRamp
import geotrellis.statistics.op._

object Render {
  def operation(r:Op[Raster], ramp:Op[ColorRamp], breaks:Op[Array[Int]]) = {
    val histo = stat.GetHistogram(r) // Fast?
    val colorBreaks = stat.BuildColorBreaks(breaks,ramp.map { r => r.toArray} )

    io.RenderPng(r,colorBreaks,histo,0)
  }
}

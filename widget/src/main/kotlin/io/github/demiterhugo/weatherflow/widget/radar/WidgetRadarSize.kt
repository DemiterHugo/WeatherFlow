package io.github.demiterhugo.weatherflow.widget.radar

/** Room for the map, timestamp, actions and credits, including on smaller launchers. */
internal object WidgetRadarSize {
    const val WIDTH_DP = 300
    const val HEIGHT_DP = 360
    fun fits(width: Int, height: Int): Boolean = width >= WIDTH_DP && height >= HEIGHT_DP
}

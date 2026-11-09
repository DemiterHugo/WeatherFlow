package io.github.demiterhugo.weatherflow.widget.radar

/** Room for the map, replay control and attribution. */
internal object WidgetRadarSize {
    const val WIDTH_DP = 240
    const val HEIGHT_DP = 145

    fun fits(width: Int, height: Int): Boolean = width >= WIDTH_DP && height >= HEIGHT_DP
}

package io.github.demiterhugo.weatherflow.widget.radar

/** Room for the map, timestamp, actions and credits, including on smaller launchers. */
internal object WidgetRadarSize {
    const val WIDTH_DP = 260
    const val HEIGHT_DP = 270
    fun mapSize(width: Int, height: Int): Int =
        if (width >= 300 && height >= 300) 144 else 104

    fun fits(width: Int, height: Int): Boolean = width >= WIDTH_DP && height >= HEIGHT_DP
}

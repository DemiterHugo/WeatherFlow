package io.github.demiterhugo.weatherflow.data.radar.map

import io.github.demiterhugo.weatherflow.data.radar.remote.MAX_MERCATOR_LATITUDE
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

internal data class MapTile(val x: Int, val y: Int, val left: Float, val top: Float)

/** Pixel offsets are shared by both renderers; wrapping applies to longitude, never latitude. */
internal fun mapTiles(coordinates: Coordinates): List<MapTile> {
    require(coordinates.latitude in -MAX_MERCATOR_LATITUDE..MAX_MERCATOR_LATITUDE)
    val count = 1 shl 7
    val world = 256.0 * count
    val centerX = (coordinates.longitude + 180) / 360 * world
    val radians = coordinates.latitude * PI / 180
    val centerY = (1 - ln(tan(PI / 4 + radians / 2)) / PI) / 2 * world
    val left = centerX - 128
    val top = centerY - 128
    return buildList {
        for (y in floor(top / 256).toInt()..floor((top + 256 - 0.000001) / 256).toInt()) {
            if (y !in 0 until count) continue
            for (x in floor(left / 256).toInt()..floor((left + 256 - 0.000001) / 256).toInt()) {
                add(MapTile(Math.floorMod(x, count), y, (x * 256 - left).toFloat(), (y * 256 - top).toFloat()))
            }
        }
    }
}

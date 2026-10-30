package io.github.demiterhugo.weatherflow.data.radar.map

import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import org.junit.Assert.*
import org.junit.Test

class MercatorTilesTest {
    @Test
    fun primeMeridianAtEquatorCentersFourKnownTiles() {
        val tiles = mapTiles(Coordinates(0.0, 0.0))
        assertEquals(setOf(63 to 63, 64 to 63, 63 to 64, 64 to 64), tiles.map { it.x to it.y }.toSet())
        assertEquals(setOf(-128f, 128f), tiles.map { it.left }.toSet())
        assertEquals(setOf(-128f, 128f), tiles.map { it.top }.toSet())
    }

    @Test
    fun barcelonaViewportContainsItsKnownSlippyMapTileAndNoMoreThanFourTiles() {
        val tiles = mapTiles(Coordinates(41.3874, 2.1686))
        assertTrue(tiles.any { it.x == 64 && it.y == 47 })
        assertTrue(tiles.size in 1..4)
        assertTrue(tiles.all { it.left > -256 && it.left < 256 && it.top > -256 && it.top < 256 })
    }

    @Test
    fun datelineWrapsLongitudeAndPolarEdgesDoNotWrapLatitude() {
        val west = mapTiles(Coordinates(0.0, -180.0))
        val east = mapTiles(Coordinates(0.0, 180.0))
        assertEquals(west, east)
        assertEquals(setOf(127, 0), east.map { it.x }.toSet())
        assertTrue(mapTiles(Coordinates(85.05112878, 0.0)).all { it.y == 0 })
        assertThrows(IllegalArgumentException::class.java) { mapTiles(Coordinates(90.0, 0.0)) }
    }
}

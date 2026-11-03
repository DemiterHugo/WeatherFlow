package io.github.demiterhugo.weatherflow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CoordinatesTest {

    @Test
    fun acceptsOrdinaryCoordinates() {
        val coordinates = Coordinates(latitude = 40.4168, longitude = -3.7038)

        assertEquals(40.4168, coordinates.latitude, 0.0)
        assertEquals(-3.7038, coordinates.longitude, 0.0)
    }

    @Test
    fun acceptsInclusiveCoordinateBoundaries() {
        for (latitude in listOf(-90.0, 0.0, 90.0)) {
            for (longitude in listOf(-180.0, 0.0, 180.0)) {
                val coordinates = Coordinates(latitude, longitude)

                assertEquals(latitude, coordinates.latitude, 0.0)
                assertEquals(longitude, coordinates.longitude, 0.0)
            }
        }
    }

    @Test
    fun rejectsLatitudesOutsideRange() {
        for (latitude in listOf(-90.0001, 90.0001)) {
            assertThrows("Latitude: $latitude", IllegalArgumentException::class.java) {
                Coordinates(latitude = latitude, longitude = 0.0)
            }
        }
    }

    @Test
    fun rejectsLongitudesOutsideRange() {
        for (longitude in listOf(-180.0001, 180.0001)) {
            assertThrows("Longitude: $longitude", IllegalArgumentException::class.java) {
                Coordinates(latitude = 0.0, longitude = longitude)
            }
        }
    }

    @Test
    fun rejectsNonFiniteLatitudes() {
        for (latitude in listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertThrows("Latitude: $latitude", IllegalArgumentException::class.java) {
                Coordinates(latitude = latitude, longitude = 0.0)
            }
        }
    }

    @Test
    fun rejectsNonFiniteLongitudes() {
        for (longitude in listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertThrows("Longitude: $longitude", IllegalArgumentException::class.java) {
                Coordinates(latitude = 0.0, longitude = longitude)
            }
        }
    }

    @Test
    fun copyCannotIntroduceInvalidCoordinates() {
        val coordinates = Coordinates(latitude = 0.0, longitude = 0.0)

        assertThrows(IllegalArgumentException::class.java) {
            coordinates.copy(latitude = 91.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            coordinates.copy(longitude = 181.0)
        }
    }
}

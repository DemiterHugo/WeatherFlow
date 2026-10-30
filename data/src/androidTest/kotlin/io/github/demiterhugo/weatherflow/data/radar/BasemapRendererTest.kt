package io.github.demiterhugo.weatherflow.data.radar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.demiterhugo.weatherflow.data.radar.map.AndroidBasemapRenderer
import io.github.demiterhugo.weatherflow.data.radar.map.mapTiles
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BasemapRendererTest {
    @Test fun fourTilesAreCroppedToTheSameGeographicCenterWithoutFlippingAxes() {
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
        val tiles = mapTiles(Coordinates(0.0, 0.0)).mapIndexed { index, tile ->
            val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val bytes = try {
                bitmap.eraseColor(colors[index])
                ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
            } finally { bitmap.recycle() }
            tile to bytes
        }
        val bytes = AndroidBasemapRenderer().render(tiles).copyBytes()
        val rendered = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        try {
            assertEquals(256, rendered.width)
            assertEquals(256, rendered.height)
            assertEquals(Color.RED, rendered.getPixel(64, 64))
            assertEquals(Color.GREEN, rendered.getPixel(192, 64))
            assertEquals(Color.BLUE, rendered.getPixel(64, 192))
            assertEquals(Color.YELLOW, rendered.getPixel(192, 192))
        } finally { rendered.recycle() }
    }
}

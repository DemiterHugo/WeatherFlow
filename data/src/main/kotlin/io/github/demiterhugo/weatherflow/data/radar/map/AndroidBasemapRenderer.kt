package io.github.demiterhugo.weatherflow.data.radar.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import io.github.demiterhugo.weatherflow.domain.model.RadarImage
import java.io.ByteArrayOutputStream

internal fun interface BasemapRenderer {
    fun render(tiles: List<Pair<MapTile, ByteArray>>): RadarImage
}

internal class AndroidBasemapRenderer : BasemapRenderer {
    override fun render(tiles: List<Pair<MapTile, ByteArray>>): RadarImage {
        val target = createBitmap(256, 256)
        try {
            val canvas = Canvas(target)
            canvas.drawColor(Color.LTGRAY)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            tiles.forEach { (tile, bytes) ->
                val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                try { canvas.drawBitmap(bitmap, tile.left, tile.top, paint) } finally { bitmap.recycle() }
            }
            return ByteArrayOutputStream().use {
                check(target.compress(Bitmap.CompressFormat.PNG, 100, it))
                RadarImage(it.toByteArray())
            }
        } finally {
            target.recycle()
        }
    }
}

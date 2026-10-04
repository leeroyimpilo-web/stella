package com.stella.game.ui

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal object PixelArtAssets {
    private val sceneCache = mutableMapOf<String, ImageBitmap>()
    private val itemCache = mutableMapOf<String, ImageBitmap>()

    fun scene(key: String): ImageBitmap {
        val canonical = when (key) {
            "cryo_closeup" -> "cryo_awaken"
            "cryo_exit", "empty_pod_detail" -> "cryo_bay_dark"
            "stella_terminal" -> "signal_static"
            else -> key
        }
        return sceneCache.getOrPut(canonical) {
            decode(SCENE_PIXEL_DATA[canonical] ?: SCENE_PIXEL_DATA.getValue("cryo_awaken"))
        }
    }

    fun item(id: String): ImageBitmap =
        itemCache.getOrPut(id) {
            decode(ITEM_PIXEL_DATA[id] ?: ITEM_PIXEL_DATA.getValue("data_shard"))
        }

    private fun decode(encoded: String): ImageBitmap {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val width = bytes[0].toInt() and 0xFF
        val height = bytes[1].toInt() and 0xFF
        val colorCount = bytes[2].toInt() and 0xFF
        var offset = 3
        val palette = IntArray(colorCount)

        for (i in 0 until colorCount) {
            val red = bytes[offset++].toInt() and 0xFF
            val green = bytes[offset++].toInt() and 0xFF
            val blue = bytes[offset++].toInt() and 0xFF
            palette[i] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
        }

        val pixels = IntArray(width * height)
        for (i in pixels.indices) {
            val paletteIndex = bytes[offset + i].toInt() and 0xFF
            pixels[i] = palette[paletteIndex.coerceIn(0, palette.lastIndex)]
        }

        return Bitmap.createBitmap(
            pixels,
            width,
            height,
            Bitmap.Config.ARGB_8888
        ).asImageBitmap()
    }
}

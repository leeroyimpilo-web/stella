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
            decodeSafely(
                SCENE_PIXEL_DATA[canonical]
                    ?: SCENE_PIXEL_DATA["cryo_awaken"]
                    ?: error("No fallback scene artwork")
            )
        }
    }

    fun item(id: String): ImageBitmap =
        itemCache.getOrPut(id) {
            decodeSafely(
                ITEM_PIXEL_DATA[id]
                    ?: ITEM_PIXEL_DATA["data_shard"]
                    ?: error("No fallback item artwork")
            )
        }

    private fun decodeSafely(encoded: String): ImageBitmap {
        return runCatching { decode(encoded) }
            .getOrElse { fallbackBitmap() }
    }

    private fun decode(encoded: String): ImageBitmap {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size >= 3) { "Artwork payload header is incomplete" }

        val width = bytes[0].toInt() and 0xFF
        val height = bytes[1].toInt() and 0xFF
        val colorCount = bytes[2].toInt() and 0xFF

        require(width > 0 && height > 0) { "Artwork dimensions are invalid" }
        require(colorCount > 0) { "Artwork palette is empty" }

        val paletteBytes = colorCount * 3
        val pixelOffset = 3 + paletteBytes
        require(bytes.size >= pixelOffset) { "Artwork palette data is incomplete" }

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
            val byteIndex = pixelOffset + i
            val paletteIndex =
                if (byteIndex < bytes.size) bytes[byteIndex].toInt() and 0xFF
                else 0

            pixels[i] = palette[paletteIndex.coerceIn(0, palette.lastIndex)]
        }

        return Bitmap.createBitmap(
            pixels,
            width,
            height,
            Bitmap.Config.ARGB_8888
        ).asImageBitmap()
    }

    private fun fallbackBitmap(): ImageBitmap {
        val width = 48
        val height = 27
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            when {
                y > height * 2 / 3 -> 0xFF03070A.toInt()
                (x + y) % 9 == 0 -> 0xFF15354A.toInt()
                else -> 0xFF07131D.toInt()
            }
        }
        return Bitmap.createBitmap(
            pixels,
            width,
            height,
            Bitmap.Config.ARGB_8888
        ).asImageBitmap()
    }
}

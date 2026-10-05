package com.stella.game.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.zip.ZipInputStream

object CinematicSceneAssets {
    private val cache = mutableMapOf<String, ImageBitmap>()

    private val aliases = mapOf(
        "cryo_closeup" to "cryo_awaken"
    )

    fun scene(context: Context, key: String): ImageBitmap? {
        val resolved = aliases[key] ?: key
        cache[resolved]?.let { return it }

        return runCatching {
            context.assets.open("scenes.zip").use { stream ->
                ZipInputStream(stream).use { zip ->
                    var entry = zip.nextEntry
                    val target = "$resolved.webp"
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name == target) {
                            val bytes = zip.readBytes()
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                ?: return@runCatching null
                            return@runCatching bitmap.asImageBitmap().also {
                                cache[resolved] = it
                            }
                        }
                        entry = zip.nextEntry
                    }
                    null
                }
            }
        }.getOrNull()
    }
}

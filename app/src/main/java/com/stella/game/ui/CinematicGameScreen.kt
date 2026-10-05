package com.stella.game.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stella.game.game.*
import com.stella.game.audio.AudioDirector
import kotlinx.coroutines.delay

private val HudBlack = Color(0xFF02070B)
private val HudPanel = Color(0xEA041019)
private val HudPanelSoft = Color(0xD905121C)
private val HudCyan = Color(0xFF45D9FF)
private val HudCyanBright = Color(0xFFBFF7FF)
private val HudBlue = Color(0xFF5EA7FF)
private val HudMuted = Color(0xFF6E96A8)
private val HudRed = Color(0xFFFF4B59)
private val HudOrange = Color(0xFFFF9D4D)
private val HudGreen = Color(0xFF8EFEC5)
private val HudGrid = Color(0x182EDCFF)

@Composable
internal fun CinematicGameScreen(engine: GameEngine) {
    val state = engine.state
    val scene = engine.currentScene
    val context = androidx.compose.ui.platform.LocalContext.current
    val note = engine.notification
    val audio = remember { AudioDirector(context) }
    var lineIndex by remember(scene.id) { mutableIntStateOf(0) }
    val storyReady = lineIndex >= scene.lines.size

    DisposableEffect(Unit) {
        onDispose { audio.release() }
    }

    LaunchedEffect(scene.id) {
        cinematicHaptic(context, scene.direction.haptic)
        lineIndex = 0
        audio.playAmbience(scene.direction.ambience)
    }

    LaunchedEffect(scene.id, lineIndex) {
        if (lineIndex >= scene.lines.size) return@LaunchedEffect
        val line = scene.lines[lineIndex]
        val voiced = audio.playVoiceAndWait(scene.id, lineIndex)
        if (!voiced) {
            val words = line.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            val readingTime = (words * 430L).coerceIn(3200L, 7600L)
            delay(maxOf(line.delayBeforeMs, readingTime))
        }
        if (lineIndex < scene.lines.size) {
            lineIndex = (lineIndex + 1).coerceAtMost(scene.lines.size)
        }
    }

    LaunchedEffect(note) {
        if (note != null) {
            audio.playSfx("pickup")
            delay(3600)
            engine.clearNotification()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(HudBlack)
    ) {
        HudGridBackground()

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HeaderAndStatus(
                state = state,
                onMenu = engine::returnToMenu
            )

            ObjectiveBar(scene)

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                MainScene(scene)

                if (scene.lines.isNotEmpty() && lineIndex < scene.lines.size) {
                    CinematicStoryOverlay(
                        lines = scene.lines,
                        visibleCount = (lineIndex + 1).coerceAtMost(scene.lines.size),
                        onAdvance = {
                            audio.stopVoice()
                            lineIndex = (lineIndex + 1).coerceAtMost(scene.lines.size)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    )
                }

                MiniMapCard(
                    state = state,
                    sceneId = scene.id,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .width(118.dp)
                        .height(96.dp)
                )
                AmbientAudioCard(
                    ambience = scene.direction.ambience,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .width(124.dp)
                        .height(62.dp)
                )

                if (note != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 76.dp)
                    ) {
                        PickupCard(note)
                    }
                }
            }

            InventoryBelt(engine) { audio.playSfx("choice") }

            CodexStrip(state)

            ChoiceRow(engine, scene, storyReady) { audio.playSfx("choice") }
        }

        ScanlineGlass()
    }
}

@Composable
private fun HeaderAndStatus(state: GameState, onMenu: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(94.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HudPanel(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight(),
            accent = HudCyan
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "STELLA",
                        color = Color.White,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 5.sp,
                        maxLines = 1
                    )
                    Text(
                        "THE SILENCE BETWEEN STARS",
                        color = HudCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                        letterSpacing = 1.35.sp,
                        maxLines = 1
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "△",
                        color = HudCyan,
                        fontSize = 20.sp,
                        lineHeight = 25.sp
                    )
                    Text(
                        "EIDOLON",
                        color = HudCyanBright,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "MENU",
                        color = HudMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 6.sp,
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .clickable(onClick = onMenu)
                    )
                }
            }
        }

        HudPanel(
            modifier = Modifier
                .weight(.95f)
                .fillMaxHeight(),
            accent = HudCyan
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MeterLine("⚡", "PWR", state.power, HudCyan)
                MeterLine("◉", "O2", state.oxygen, HudCyanBright)
                MeterLine("◆", "HULL", state.hull, HudCyan)
                MeterLine("☣", "BIO", state.health, if (state.health < 40) HudRed else HudOrange)
                LinkLine(state.stellaTrust)
            }
        }
    }
}

@Composable
private fun MeterLine(icon: String, label: String, value: Int, color: Color) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = color, fontSize = 9.sp, modifier = Modifier.width(15.dp))
        Text(
            label,
            color = HudCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 7.sp,
            modifier = Modifier.width(28.dp)
        )
        Meter(
            value = value,
            color = color,
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            value.toString() + "%",
            color = if (value < 30) HudRed else Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 7.sp,
            modifier = Modifier.width(26.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun LinkLine(value: Int) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⌁", color = HudCyan, fontSize = 10.sp, modifier = Modifier.width(15.dp))
        Text(
            "STELLA LINK",
            color = HudCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 6.sp,
            modifier = Modifier.width(54.dp)
        )
        Meter(
            value = value,
            color = HudCyanBright,
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            if (value > 10) "ONLINE" else "WEAK",
            color = if (value > 10) HudCyanBright else HudRed,
            fontFamily = FontFamily.Monospace,
            fontSize = 6.sp
        )
    }
}

@Composable
private fun Meter(value: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(Color.Black.copy(alpha = .65f))
            .border(.5.dp, HudCyan.copy(alpha = .35f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(value.coerceIn(0, 100) / 100f)
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = .75f), color)
                    )
                )
        )
    }
}

@Composable
private fun ObjectiveBar(scene: Scene) {
    HudPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        accent = HudCyan
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .border(1.dp, HudCyan.copy(alpha = .7f), RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("›", color = HudCyanBright, fontSize = 24.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "MAIN OBJECTIVE",
                    color = HudCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    objectiveFor(scene.id),
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun objectiveFor(sceneId: String): String = when {
    sceneId.startsWith("awakening") || sceneId.startsWith("pods") -> "Exit cryogenic chamber C-7"
    sceneId.startsWith("corridor") -> "Reach the sealed bridge access"
    sceneId.startsWith("service") -> "Find a route to the bridge"
    sceneId == "door_01" -> "Identify the figure beyond the door"
    sceneId.startsWith("bridge") || sceneId.startsWith("chapter1") -> "Restore navigation and identify this system"
    else -> "Survive the Eidolon"
}

@Composable
private fun MainScene(scene: Scene) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cinematic = remember(scene.direction.artworkKey) {
        CinematicSceneAssets.scene(context, scene.direction.artworkKey)
    }

    HudPanel(
        modifier = Modifier.fillMaxSize(),
        accent = if (scene.direction.danger) HudRed else HudCyan
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                bitmap = cinematic ?: PixelArtAssets.scene(scene.direction.artworkKey),
                contentDescription = scene.title,
                contentScale = ContentScale.Crop,
                filterQuality = if (cinematic != null) FilterQuality.Medium else FilterQuality.None,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = .08f),
                            .55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = .42f)
                        )
                    )
            )

            Column(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    scene.direction.location,
                    color = HudCyanBright,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 6.sp
                )
                Text(
                    scene.title,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MiniMapCard(state: GameState, sceneId: String, modifier: Modifier = Modifier) {
    HudPanel(modifier = modifier, accent = HudCyan) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    deckLabel(sceneId),
                    color = HudCyanBright,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text("⌗", color = HudCyan, fontSize = 9.sp)
            }
            Spacer(Modifier.height(4.dp))
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val line = HudCyan.copy(alpha = .75f)
                val faint = HudCyan.copy(alpha = .18f)
                val red = HudRed.copy(alpha = .9f)

                for (i in 0..6) {
                    val x = i * w / 6
                    drawLine(faint, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), 1f)
                }
                for (i in 0..4) {
                    val y = i * h / 4
                    drawLine(faint, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), 1f)
                }

                val path = Path().apply {
                    moveTo(w * .08f, h * .72f)
                    lineTo(w * .22f, h * .72f)
                    lineTo(w * .22f, h * .40f)
                    lineTo(w * .45f, h * .40f)
                    lineTo(w * .45f, h * .20f)
                    lineTo(w * .67f, h * .20f)
                    lineTo(w * .67f, h * .55f)
                    lineTo(w * .86f, h * .55f)
                }
                drawPath(path, line, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))

                drawRect(
                    red,
                    topLeft = androidx.compose.ui.geometry.Offset(w * .78f, h * .42f),
                    size = androidx.compose.ui.geometry.Size(w * .18f, h * .25f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )

                val px = when {
                    sceneId.startsWith("awakening") || sceneId.startsWith("pods") -> w * .20f
                    sceneId.startsWith("corridor") || sceneId == "door_01" -> w * .47f
                    sceneId.startsWith("service") -> w * .58f
                    else -> w * .76f
                }
                val py = when {
                    sceneId.startsWith("awakening") || sceneId.startsWith("pods") -> h * .67f
                    sceneId.startsWith("corridor") || sceneId == "door_01" -> h * .36f
                    sceneId.startsWith("service") -> h * .22f
                    else -> h * .50f
                }

                val marker = Path().apply {
                    moveTo(px, py - 7f)
                    lineTo(px - 6f, py + 5f)
                    lineTo(px + 6f, py + 5f)
                    close()
                }
                drawPath(marker, HudCyanBright)
            }
        }
    }
}

private fun deckLabel(sceneId: String): String = when {
    sceneId.startsWith("service") -> "SHAFT 7-B"
    sceneId.startsWith("bridge") || sceneId.startsWith("chapter1") -> "BRIDGE"
    sceneId.startsWith("awakening") || sceneId.startsWith("pods") -> "DECK C-7"
    else -> "DECK 7"
}

@Composable
private fun AmbientAudioCard(ambience: String, modifier: Modifier = Modifier) {
    HudPanel(modifier = modifier, accent = HudCyan) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Waveform(Modifier.width(40.dp).fillMaxHeight())
            Spacer(Modifier.width(7.dp))
            Column {
                Text(
                    "AMBIENT AUDIO",
                    color = HudCyanBright,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    ambience.replace("_", " ").uppercase(),
                    color = HudCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 5.sp,
                    maxLines = 1
                )
                Text(
                    "DYNAMIC MIX",
                    color = HudMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 5.sp
                )
            }
        }
    }
}

@Composable
private fun Waveform(modifier: Modifier = Modifier) {
    val heights = listOf(.25f, .62f, .38f, .82f, .52f, .94f, .44f, .70f, .31f)
    Canvas(modifier) {
        val gap = size.width / (heights.size * 1.45f)
        heights.forEachIndexed { index, value ->
            val x = gap * (index * 1.45f + .7f)
            val hh = size.height * value * .72f
            drawLine(
                HudCyanBright,
                androidx.compose.ui.geometry.Offset(x, size.height / 2f - hh / 2f),
                androidx.compose.ui.geometry.Offset(x, size.height / 2f + hh / 2f),
                2f
            )
        }
    }
}

@Composable
private fun CinematicStoryOverlay(
    lines: List<StoryLine>,
    visibleCount: Int,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = lines.take(visibleCount).takeLast(4)

    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onAdvance)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = .54f),
                        Color.Black.copy(alpha = .84f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        visible.forEachIndexed { index, line ->
            val isCurrent = index == visible.lastIndex
            val accent = when (line.speaker) {
                Speaker.STELLA -> HudCyan
                Speaker.SYSTEM -> HudMuted
                Speaker.PLAYER -> HudGreen
                Speaker.UNKNOWN -> HudRed
                Speaker.NARRATOR -> HudCyanBright
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (line.speaker != Speaker.NARRATOR) {
                    Text(
                        line.speaker.name,
                        color = accent.copy(alpha = if (isCurrent) 1f else .55f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = .7.sp,
                        modifier = Modifier.width(50.dp)
                    )
                } else {
                    Spacer(Modifier.width(50.dp))
                }

                Text(
                    line.text,
                    color = Color.White.copy(alpha = if (isCurrent) 1f else .48f),
                    fontSize = if (isCurrent) 12.sp else 10.sp,
                    lineHeight = if (isCurrent) 16.sp else 13.sp,
                    fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            "TAP TO ADVANCE",
            color = HudMuted.copy(alpha = .72f),
            fontFamily = FontFamily.Monospace,
            fontSize = 5.sp,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun PickupCard(text: String) {
    HudPanel(
        modifier = Modifier
            .width(190.dp)
            .height(48.dp),
        accent = HudCyan
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .border(1.dp, HudCyan.copy(alpha = .55f)),
                contentAlignment = Alignment.Center
            ) {
                Text("◈", color = HudCyanBright, fontSize = 27.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text.replace(" // ", "\n"),
                    color = HudCyanBright,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 8.sp,
                    maxLines = 2
                )
            }
            Text("›", color = HudCyan, fontSize = 18.sp)
        }
    }
}

@Composable
private fun InventoryBelt(engine: GameEngine, onSfx: () -> Unit) {
    val state = engine.state
    val slots = state.inventory.distinct().take(5)

    HudPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        accent = HudCyan
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("▣", color = HudCyanBright, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "INVENTORY",
                    color = HudCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(6) { index ->
                    val id = slots.getOrNull(index)
                    InventorySlot(
                        id = id,
                        count = id?.let { state.itemCount(it) } ?: 0,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (id != null) {
                                onSfx()
                                engine.useItem(id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InventorySlot(
    id: String?,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = .34f))
            .border(1.dp, HudCyan.copy(alpha = .38f), RoundedCornerShape(2.dp))
            .then(if (id != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (id != null) {
            Image(
                bitmap = PixelArtAssets.item(id),
                contentDescription = ItemCatalog.get(id)?.name,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
            )
            Text(
                count.toString(),
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
            )
        } else {
            Text("+", color = HudBlue.copy(alpha = .75f), fontSize = 24.sp)
        }
    }
}

@Composable
private fun CodexStrip(state: GameState) {
    val entry = state.codex
        .toList()
        .mapNotNull { CodexCatalog.all[it] }
        .lastOrNull()

    HudPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        accent = HudCyan
    ) {
        if (entry == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "CODEX // NO ENTRY SELECTED",
                    color = HudMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.sp
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(118.dp)
                        .fillMaxHeight()
                        .border(1.dp, HudCyan.copy(alpha = .45f), RoundedCornerShape(2.dp))
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val bridgeArt = remember {
                        CinematicSceneAssets.scene(context, "bridge_first_view")
                    }
                    Image(
                        bitmap = bridgeArt ?: PixelArtAssets.scene("bridge_first_view"),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = if (bridgeArt != null) FilterQuality.Medium else FilterQuality.None,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "CODEX ENTRY",
                        color = HudCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = .7.sp
                    )
                    Text(
                        entry.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        entry.summary,
                        color = HudCyanBright.copy(alpha = .78f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("›", color = HudCyan, fontSize = 25.sp)
            }
        }
    }
}

@Composable
private fun ChoiceRow(engine: GameEngine, scene: Scene, storyReady: Boolean, onSfx: () -> Unit) {
    val choices = scene.choices.take(3)

    Row(
        Modifier
            .fillMaxWidth()
            .height(94.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { index ->
            val choice = choices.getOrNull(index)
            ActionChoice(
                choice = choice,
                index = index + 1,
                engine = engine,
                storyReady = storyReady,
                onSfx = onSfx,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionChoice(
    choice: Choice?,
    index: Int,
    engine: GameEngine,
    storyReady: Boolean,
    onSfx: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (choice == null) {
        HudPanel(
            modifier = modifier.fillMaxHeight().alpha(.3f),
            accent = HudMuted
        ) {}
        return
    }

    val enabled = storyReady && engine.canChoose(choice)
    val danger = choice.id.contains("plasma", ignoreCase = true) ||
        choice.text.contains("PLASMA", ignoreCase = true) ||
        choice.text.contains("BREACH", ignoreCase = true)
    val accent = when {
        !enabled -> HudMuted
        danger -> HudRed
        else -> HudCyan
    }

    HudPanel(
        modifier = modifier
            .fillMaxHeight()
            .then(if (enabled) Modifier.clickable {
                onSfx()
                engine.choose(choice)
            } else Modifier),
        accent = accent
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    actionIcon(choice),
                    color = accent,
                    fontSize = 22.sp,
                    modifier = Modifier.width(30.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    choice.text,
                    color = if (enabled) if (danger) HudRed else HudCyanBright else HudMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 11.sp,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    index.toString().padStart(2, '0'),
                    color = accent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 6.sp
                )
            }

            Text(
                if (enabled) actionHint(choice) else if (!storyReady) "LISTENING..." else (engine.choiceLockReason(choice) ?: "LOCKED"),
                color = if (enabled) accent.copy(alpha = .78f) else HudMuted.copy(alpha = .6f),
                fontSize = 6.sp,
                maxLines = 2
            )
        }
    }
}

private fun actionIcon(choice: Choice): String = when {
    choice.text.contains("SCAN", true) -> "⌕"
    choice.text.contains("SERVICE", true) -> "▥"
    choice.text.contains("OPEN", true) -> "□"
    choice.text.contains("TAKE", true) -> "↓"
    choice.text.contains("CUT", true) || choice.text.contains("PLASMA", true) -> "⌁"
    choice.text.contains("FOLLOW", true) -> "›"
    else -> "◇"
}

private fun actionHint(choice: Choice): String = when {
    choice.requiredItems.isNotEmpty() -> "Requires " + choice.requiredItems.joinToString { ItemCatalog.get(it)?.name ?: it }
    choice.text.contains("SCAN", true) -> "Search for hidden access points"
    choice.text.contains("SERVICE", true) -> "Find another route"
    choice.text.contains("OPEN", true) -> "Access secured system"
    choice.text.contains("TAKE", true) -> "Collect and store item"
    else -> "Choose this action"
}

@Composable
private fun HudPanel(
    modifier: Modifier = Modifier,
    accent: Color,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = CutCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomEnd = 7.dp, bottomStart = 7.dp)
    Box(
        modifier
            .clip(shape)
            .background(HudPanel)
            .border(1.dp, accent.copy(alpha = .88f), shape)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = .035f),
                            Color.Transparent,
                            Color.Black.copy(alpha = .08f)
                        )
                    )
                )
        )
        content()
    }
}

@Composable
private fun HudGridBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(HudBlack)
        val step = 22f
        var x = 0f
        while (x < size.width) {
            drawLine(HudGrid, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(HudGrid, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f)
            y += step
        }
    }
}

@Composable
private fun ScanlineGlass() {
    Canvas(Modifier.fillMaxSize()) {
        var y = 0f
        while (y < size.height) {
            drawLine(
                Color.Black.copy(alpha = .08f),
                androidx.compose.ui.geometry.Offset(0f, y),
                androidx.compose.ui.geometry.Offset(size.width, y),
                1f
            )
            y += 7f
        }
    }
}

private fun cinematicHaptic(context: Context, cue: HapticCue) {
    if (cue == HapticCue.NONE) return
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    val timings = when (cue) {
        HapticCue.TAP -> longArrayOf(0, 18)
        HapticCue.IMPACT -> longArrayOf(0, 70, 35, 40)
        HapticCue.HEARTBEAT -> longArrayOf(0, 50, 80, 70)
        HapticCue.ALARM -> longArrayOf(0, 110, 65, 110)
        HapticCue.NONE -> longArrayOf(0)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(timings, -1)
    }
}

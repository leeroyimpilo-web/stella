package com.stella.game.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stella.game.game.*
import kotlinx.coroutines.delay

private val SpaceBlack = Color(0xFF02050A)
private val Panel = Color(0xED06111A)
private val PanelSoft = Color(0xD9091722)
private val Ice = Color(0xFF8BE9FD)
private val IceBright = Color(0xFFC6F7FF)
private val Muted = Color(0xFF8298A8)
private val Danger = Color(0xFFFF5264)
private val Good = Color(0xFF8FFFC1)

private enum class PanelMode { INVENTORY, MAP, CODEX }

@Composable
fun StellaApp() {
    val context = LocalContext.current
    val engine = remember { GameEngine(context.applicationContext) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = SpaceBlack,
            surface = Panel,
            primary = Ice,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Box(Modifier.fillMaxSize().background(SpaceBlack)) {
            Starfield()
            AnimatedContent(
                targetState = engine.state.playStarted,
                transitionSpec = { fadeIn(tween(650)) togetherWith fadeOut(tween(350)) },
                label = "screen"
            ) { playing ->
                if (playing) CinematicGameScreen(engine) else MainMenu(engine)
            }
        }
    }
}

@Composable
private fun MainMenu(engine: GameEngine) {
    Box(Modifier.fillMaxSize()) {
        Image(
            bitmap = PixelArtAssets.scene("bridge_first_view"),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.None,
            modifier = Modifier.fillMaxSize().alpha(.46f)
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = .32f), SpaceBlack.copy(alpha = .68f), SpaceBlack)
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "STELLA",
                    color = Color.White,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp
                )
                Text(
                    "THE SILENCE BETWEEN STARS",
                    color = Ice,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(16.dp))
                Box(Modifier.width(120.dp).height(1.dp).background(Ice.copy(alpha = .85f)))
                Spacer(Modifier.height(16.dp))
                Text(
                    "AN INTERACTIVE SCI-FI FILM",
                    color = IceBright.copy(alpha = .78f),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "YOUR CHOICES. YOUR INVENTORY. STELLA REMEMBERS.",
                    color = Muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    letterSpacing = 1.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (engine.hasSave()) {
                    MenuButton("CONTINUE", "Resume your last autosave") { engine.continueGame() }
                }
                MenuButton("NEW GAME", "Wake aboard the Eidolon") { engine.newGame() }
                DisabledMenuButton("CHAPTER SELECT", "Unlock chapters by surviving them")
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "BUILD 0.2 // INVENTORY",
                    color = Muted.copy(alpha = .7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                )
                Text(
                    "OFFLINE",
                    color = Good.copy(alpha = .75f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                )
            }
        }
        ScanlineOverlay()
    }
}

@Composable
private fun MenuButton(text: String, subtext: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, Ice.copy(alpha = .48f), RoundedCornerShape(3.dp))
            .background(Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(text, color = Color.White, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(subtext, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun DisabledMenuButton(text: String, subtext: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(.42f)
            .border(1.dp, Muted.copy(alpha = .35f))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(text, color = Muted, letterSpacing = 2.sp)
        Text(subtext, color = Muted.copy(alpha = .7f), fontSize = 10.sp)
    }
}

@Composable
private fun GameScreen(engine: GameEngine) {
    val scene = engine.currentScene
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    val context = LocalContext.current
    var panel by remember { mutableStateOf<PanelMode?>(null) }

    LaunchedEffect(scene.id) { triggerHaptic(context, scene.direction.haptic) }

    val note = engine.notification
    LaunchedEffect(note) {
        if (note != null) {
            delay(2100)
            engine.clearNotification()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 6.dp)
        ) {
            Hud(engine.state, scene, onMenu = engine::returnToMenu)
            Spacer(Modifier.height(6.dp))

            if (landscape) {
                Row(Modifier.weight(1f)) {
                    SceneVisual(scene, Modifier.weight(1.25f).fillMaxHeight())
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(.85f).fillMaxHeight()) {
                        QuickInventory(engine.state, onOpen = { panel = PanelMode.INVENTORY })
                        Spacer(Modifier.height(6.dp))
                        DialoguePanel(engine, scene, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            } else {
                SceneVisual(scene, Modifier.fillMaxWidth().weight(.42f))
                Spacer(Modifier.height(6.dp))
                QuickInventory(engine.state, onOpen = { panel = PanelMode.INVENTORY })
                Spacer(Modifier.height(6.dp))
                DialoguePanel(engine, scene, Modifier.fillMaxWidth().weight(.58f))
            }

            Spacer(Modifier.height(6.dp))
            BottomNav(
                onInventory = { panel = PanelMode.INVENTORY },
                onMap = { panel = PanelMode.MAP },
                onCodex = { panel = PanelMode.CODEX }
            )
        }

        AnimatedVisibility(
            visible = panel != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150))
        ) {
            when (panel) {
                PanelMode.INVENTORY -> InventoryPanel(engine, onClose = { panel = null })
                PanelMode.MAP -> MapPanel(engine.state, scene.id, onClose = { panel = null })
                PanelMode.CODEX -> CodexPanel(engine.state, onClose = { panel = null })
                null -> Unit
            }
        }

        AnimatedVisibility(
            visible = note != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            if (note != null) {
                NotificationBanner(note)
            }
        }
    }
}

@Composable
private fun Hud(state: GameState, scene: Scene, onMenu: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(Panel)
            .border(1.dp, Ice.copy(alpha = .12f), RoundedCornerShape(3.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    scene.direction.location,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "CH " + scene.chapter + " // " + scene.title,
                    color = Ice.copy(alpha = .8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.sp,
                    letterSpacing = .8.sp
                )
            }
            Text(
                "MENU",
                color = Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                modifier = Modifier.clickable(onClick = onMenu).padding(6.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniStat("PWR", state.power, if (state.power < 25) Danger else Ice, Modifier.weight(1f))
            MiniStat("O2", state.oxygen, if (state.oxygen < 25) Danger else Ice, Modifier.weight(1f))
            MiniStat("HULL", state.hull, if (state.hull < 25) Danger else Ice, Modifier.weight(1f))
            MiniStat("BIO", state.health, if (state.health < 35) Danger else Good, Modifier.weight(1f))
            MiniStat("LINK", state.stellaTrust, IceBright, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 6.sp)
            Text(value.toString(), color = color, fontFamily = FontFamily.Monospace, fontSize = 6.sp)
        }
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = .08f))) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value.coerceIn(0, 100) / 100f))
                    .background(color)
            )
        }
    }
}

@Composable
private fun SceneVisual(scene: Scene, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "scene")
    val dangerPulse by infinite.animateFloat(
        initialValue = .04f,
        targetValue = if (scene.direction.danger) .24f else .07f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (scene.direction.danger) 850 else 2500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dangerPulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, if (scene.direction.danger) Danger.copy(alpha = .35f) else Ice.copy(alpha = .22f))
            .background(Color.Black)
    ) {
        Image(
            bitmap = PixelArtAssets.scene(scene.direction.artworkKey),
            contentDescription = scene.title,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.None,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = .08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = .52f)
                        )
                    )
                )
        )
        if (scene.direction.danger) {
            Box(Modifier.fillMaxSize().background(Danger.copy(alpha = dangerPulse)))
        }

        Column(
            Modifier.align(Alignment.BottomStart).padding(10.dp)
        ) {
            Text(
                scene.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.4.sp
            )
            Text(
                "CAMERA // " + scene.direction.cameraMove.uppercase(),
                color = IceBright.copy(alpha = .65f),
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp
            )
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = .55f))
                .border(1.dp, Ice.copy(alpha = .2f))
                .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
            Text(
                "LIVE SCENE",
                color = Ice.copy(alpha = .85f),
                fontFamily = FontFamily.Monospace,
                fontSize = 6.sp,
                letterSpacing = 1.sp
            )
        }

        Box(Modifier.fillMaxWidth().height(5.dp).align(Alignment.TopCenter).background(Color.Black.copy(alpha = .72f)))
        Box(Modifier.fillMaxWidth().height(5.dp).align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .72f)))
        ScanlineOverlay()
    }
}

@Composable
private fun QuickInventory(state: GameState, onOpen: () -> Unit) {
    val distinct = state.inventory.distinct()
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp, max = 82.dp)
            .background(PanelSoft)
            .border(1.dp, Ice.copy(alpha = .14f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "INVENTORY",
                color = Ice,
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.width(7.dp))
            Text(
                distinct.size.toString() + " TYPES",
                color = Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 6.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                "OPEN",
                color = IceBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                modifier = Modifier.clickable(onClick = onOpen).padding(4.dp)
            )
        }
        Spacer(Modifier.height(4.dp))

        if (distinct.isEmpty()) {
            Text(
                "NO ITEMS COLLECTED // SEARCH THE SHIP",
                color = Muted.copy(alpha = .75f),
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(distinct.take(8)) { id ->
                    val def = ItemCatalog.get(id) ?: return@items
                    Row(
                        Modifier
                            .width(98.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .border(1.dp, Ice.copy(alpha = .16f))
                            .clickable(onClick = onOpen)
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = PixelArtAssets.item(id),
                            contentDescription = def.name,
                            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.None,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(5.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                def.name.uppercase(),
                                color = Color.White,
                                fontSize = 6.sp,
                                lineHeight = 7.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val count = state.itemCount(id)
                            if (count > 1) {
                                Text("x$count", color = Ice, fontFamily = FontFamily.Monospace, fontSize = 6.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNav(onInventory: () -> Unit, onMap: () -> Unit, onCodex: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(42.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NavButton("INVENTORY", Modifier.weight(1f), onInventory)
        NavButton("SHIP MAP", Modifier.weight(1f), onMap)
        NavButton("CODEX", Modifier.weight(1f), onCodex)
    }
}

@Composable
private fun NavButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxHeight()
            .border(1.dp, Ice.copy(alpha = .22f))
            .background(Panel)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = IceBright.copy(alpha = .9f),
            fontFamily = FontFamily.Monospace,
            fontSize = 7.sp,
            letterSpacing = .8.sp
        )
    }
}

@Composable
private fun DialoguePanel(engine: GameEngine, scene: Scene, modifier: Modifier = Modifier) {
    var revealedCount by remember(scene.id) { mutableIntStateOf(0) }

    LaunchedEffect(scene.id) {
        revealedCount = 0
        scene.lines.forEachIndexed { index, line ->
            delay(line.delayBeforeMs.coerceAtLeast(120L))
            revealedCount = index + 1
        }
    }

    val allLinesVisible = revealedCount >= scene.lines.size

    LazyColumn(
        modifier = modifier
            .background(Panel)
            .border(1.dp, Ice.copy(alpha = .10f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        itemsIndexed(scene.lines.take(revealedCount)) { _, line -> DialogueLine(line) }

        if (!allLinesVisible) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("▌", color = Ice, fontFamily = FontFamily.Monospace)
                    Text(
                        "REVEAL",
                        color = Muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                        modifier = Modifier
                            .clickable { revealedCount = scene.lines.size }
                            .padding(4.dp)
                    )
                }
            }
        }

        if (allLinesVisible) {
            item { Box(Modifier.fillMaxWidth().height(1.dp).background(Ice.copy(alpha = .14f))) }

            itemsIndexed(scene.choices) { index, choice ->
                val enabled = engine.canChoose(choice)
                ChoiceButton(
                    number = index + 1,
                    choice = choice,
                    enabled = enabled,
                    lockReason = engine.choiceLockReason(choice),
                    onClick = { engine.choose(choice) }
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "STELLA LINK // " + engine.state.stellaTrust + "%",
                    color = Muted.copy(alpha = .7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.sp
                )
            }
        }
    }
}

@Composable
private fun DialogueLine(line: StoryLine) {
    val speakerColor = when (line.speaker) {
        Speaker.STELLA -> Ice
        Speaker.SYSTEM -> Muted
        Speaker.PLAYER -> Good
        Speaker.UNKNOWN -> Danger
        Speaker.NARRATOR -> Color.White
    }

    Column {
        if (line.speaker != Speaker.NARRATOR) {
            Text(
                line.speaker.name,
                color = speakerColor.copy(alpha = .72f),
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                letterSpacing = 1.3.sp
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            line.text,
            color = if (line.speaker == Speaker.NARRATOR) Color.White.copy(alpha = .9f) else speakerColor,
            fontSize = if (line.speaker == Speaker.SYSTEM) 11.sp else 14.sp,
            lineHeight = 19.sp,
            fontFamily = if (line.speaker == Speaker.SYSTEM) FontFamily.Monospace else FontFamily.SansSerif
        )
    }
}

@Composable
private fun ChoiceButton(
    number: Int,
    choice: Choice,
    enabled: Boolean,
    lockReason: String?,
    onClick: () -> Unit
) {
    val border = if (enabled) Ice.copy(alpha = .32f) else Muted.copy(alpha = .16f)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, border, RoundedCornerShape(2.dp))
            .background(if (enabled) Color.Transparent else Color.Black.copy(alpha = .18f))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            number.toString().padStart(2, '0'),
            color = if (enabled) Ice.copy(alpha = .65f) else Muted.copy(alpha = .4f),
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                choice.text,
                color = if (enabled) Color.White else Muted.copy(alpha = .55f),
                fontSize = 11.sp,
                letterSpacing = .55.sp,
                fontWeight = FontWeight.Medium
            )
            if (!enabled && lockReason != null) {
                Text(
                    lockReason,
                    color = if (lockReason == "COMPLETED") Good.copy(alpha = .65f) else Danger.copy(alpha = .7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 6.sp,
                    letterSpacing = .6.sp
                )
            }
        }
        Text(if (enabled) "›" else "×", color = if (enabled) Ice else Muted.copy(alpha = .35f), fontSize = 16.sp)
    }
}

@Composable
private fun InventoryPanel(engine: GameEngine, onClose: () -> Unit) {
    val state = engine.state
    val distinct = state.inventory.distinct()
    var selectedId by remember { mutableStateOf(distinct.firstOrNull()) }

    LaunchedEffect(state.inventory) {
        if (selectedId != null && !state.hasItem(selectedId!!)) {
            selectedId = state.inventory.distinct().firstOrNull()
        }
    }

    SystemSurface("INVENTORY", state.inventory.size.toString() + " ITEMS", onClose) {
        if (distinct.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "EMPTY INVENTORY\n\nSearch scenes and investigate optional routes.",
                    color = Muted,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(105.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                gridItems(distinct) { id ->
                    val def = ItemCatalog.get(id) ?: return@gridItems
                    val selected = id == selectedId
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .border(1.dp, if (selected) Ice else Ice.copy(alpha = .14f))
                            .background(if (selected) Ice.copy(alpha = .06f) else PanelSoft)
                            .clickable { selectedId = id }
                            .padding(5.dp)
                    ) {
                        Image(
                            bitmap = PixelArtAssets.item(id),
                            contentDescription = def.name,
                            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.None,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            def.name.uppercase(),
                            color = if (selected) IceBright else Color.White,
                            fontSize = 7.sp,
                            lineHeight = 8.sp,
                            maxLines = 2
                        )
                        if (state.itemCount(id) > 1) {
                            Text("QTY " + state.itemCount(id), color = Ice, fontFamily = FontFamily.Monospace, fontSize = 6.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            selectedId?.let { id ->
                val def = ItemCatalog.get(id)
                if (def != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 116.dp)
                            .border(1.dp, Ice.copy(alpha = .18f))
                            .background(Panel)
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = PixelArtAssets.item(id),
                            contentDescription = def.name,
                            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.None,
                            modifier = Modifier.width(105.dp).aspectRatio(1.3f).clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(def.name.uppercase(), color = IceBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                def.category.name + " // QTY " + state.itemCount(id),
                                color = Muted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 7.sp
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(def.description, color = Color.White.copy(alpha = .86f), fontSize = 10.sp, lineHeight = 14.sp)
                            Spacer(Modifier.height(7.dp))
                            SmallActionButton(if (def.consumable) "USE ITEM" else "INTERACT") { engine.useItem(id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPanel(state: GameState, sceneId: String, onClose: () -> Unit) {
    val nodes = listOf(
        Triple("CRYO BAY", true, sceneId.startsWith("awakening") || sceneId.startsWith("pods")),
        Triple("DECK 7 CORRIDOR", state.history.any { it.contains("bridge") || it.contains("locker") || it.contains("service") || it.contains("door") }, sceneId.contains("corridor") || sceneId == "door_01"),
        Triple("SERVICE SHAFT 7-B", state.history.any { it.contains("service") || it.contains("scanner") }, sceneId.startsWith("service")),
        Triple("EIDOLON BRIDGE", state.history.any { it.contains("retreat_bridge") || it.contains("enter_dark") || it.contains("nav_") || it.contains("scan_unknown") }, sceneId.startsWith("bridge") || sceneId.startsWith("chapter1")),
        Triple("COMMAND VAULT", state.flags.contains("vault_looted") || state.flags.contains("vault_checked"), sceneId == "bridge_vault"),
        Triple("ENGINEERING", false, false),
        Triple("MEDICAL", false, false),
        Triple("STELLA CORE", false, false)
    )

    SystemSurface("SHIP MAP", "EIDOLON // DECK ACCESS", onClose) {
        Text(
            "Discovered areas persist. Locked decks will open as power and credentials are restored.",
            color = Muted,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(nodes) { index, node ->
                val (name, unlocked, current) = node
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(34.dp)) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .border(1.dp, if (current) IceBright else if (unlocked) Ice else Muted.copy(alpha = .3f))
                                .background(if (current) Ice.copy(alpha = .35f) else Color.Transparent)
                        )
                        if (index < nodes.lastIndex) {
                            Box(Modifier.width(1.dp).height(34.dp).background(if (unlocked) Ice.copy(alpha = .22f) else Muted.copy(alpha = .12f)))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(name, color = if (unlocked) Color.White else Muted.copy(alpha = .4f), fontSize = 11.sp, letterSpacing = .5.sp)
                        Text(
                            when {
                                current -> "CURRENT LOCATION"
                                unlocked -> "DISCOVERED"
                                else -> "LOCKED"
                            },
                            color = when {
                                current -> IceBright
                                unlocked -> Good.copy(alpha = .75f)
                                else -> Muted.copy(alpha = .35f)
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 7.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodexPanel(state: GameState, onClose: () -> Unit) {
    val entries = CodexCatalog.all.values.filter { state.codex.contains(it.id) }
    SystemSurface("CODEX", entries.size.toString() + " / " + CodexCatalog.all.size + " DISCOVERED", onClose) {
        Text(
            "STELLA archives information you uncover. Some entries change meaning when new evidence is found.",
            color = Muted,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(entries) { entry ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, Ice.copy(alpha = .14f))
                        .background(PanelSoft)
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.title, color = IceBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(entry.category.name, color = Ice.copy(alpha = .7f), fontFamily = FontFamily.Monospace, fontSize = 6.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(entry.summary, color = Color.White.copy(alpha = .82f), fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun SystemSurface(title: String, subtitle: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(SpaceBlack.copy(alpha = .97f))) {
        Starfield()
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
                .background(Panel)
                .border(1.dp, Ice.copy(alpha = .28f))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text(subtitle, color = Ice, fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = .8.sp)
                }
                Box(
                    Modifier
                        .border(1.dp, Ice.copy(alpha = .28f))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("CLOSE", color = IceBright, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Ice.copy(alpha = .15f)))
            Spacer(Modifier.height(10.dp))
            content()
        }
        ScanlineOverlay()
    }
}

@Composable
private fun SmallActionButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .border(1.dp, Ice.copy(alpha = .38f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(label, color = IceBright, fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = .7.sp)
    }
}

@Composable
private fun NotificationBanner(text: String) {
    Box(
        Modifier
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xF20A1A24))
            .border(1.dp, Ice.copy(alpha = .55f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            color = IceBright,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = .8.sp
        )
    }
}

@Composable
private fun ScanlineOverlay() {
    val infinite = rememberInfiniteTransition(label = "scan")
    val y by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "scanY"
    )

    Canvas(Modifier.fillMaxSize()) {
        val lineY = size.height * y
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, Ice.copy(alpha = .035f), Color.Transparent),
                startY = lineY - 60f,
                endY = lineY + 60f
            )
        )
        repeat((size.height / 8f).toInt()) { i ->
            drawLine(
                Color.Black.copy(alpha = .08f),
                androidx.compose.ui.geometry.Offset(0f, i * 8f),
                androidx.compose.ui.geometry.Offset(size.width, i * 8f),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
private fun Starfield() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF030711), SpaceBlack, Color.Black)))
        repeat(60) { i ->
            val x = (i * 137f) % size.width
            val y = (i * 83f) % size.height
            drawCircle(
                Color.White.copy(alpha = .04f + (i % 5) * .018f),
                radius = if (i % 11 == 0) 1.6f else .8f,
                center = androidx.compose.ui.geometry.Offset(x, y),
                style = if (i % 7 == 0) Stroke(.5f) else androidx.compose.ui.graphics.drawscope.Fill
            )
        }
    }
}

private fun triggerHaptic(context: Context, cue: HapticCue) {
    if (cue == HapticCue.NONE) return
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    val timings = when (cue) {
        HapticCue.TAP -> longArrayOf(0, 20)
        HapticCue.IMPACT -> longArrayOf(0, 80, 40, 45)
        HapticCue.HEARTBEAT -> longArrayOf(0, 55, 90, 80)
        HapticCue.ALARM -> longArrayOf(0, 120, 80, 120)
        HapticCue.NONE -> longArrayOf(0)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(timings, -1)
    }
}

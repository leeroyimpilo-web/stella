package com.stella.game.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stella.game.game.*
import kotlinx.coroutines.delay

private val SpaceBlack = Color(0xFF02050A)
private val Panel = Color(0xCC07111B)
private val Ice = Color(0xFF8BE9FD)
private val Muted = Color(0xFF8A9AAA)
private val Danger = Color(0xFFFF5C6C)

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
                if (playing) GameScreen(engine) else MainMenu(engine)
            }
        }
    }
}

@Composable
private fun MainMenu(engine: GameEngine) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("STELLA", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
            Text("THE SILENCE BETWEEN STARS", color = Ice, fontSize = 12.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.width(110.dp).height(1.dp).background(Ice.copy(alpha = .8f)))
            Spacer(Modifier.height(18.dp))
            Text("AN INTERACTIVE SCI-FI FILM", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (engine.hasSave()) MenuButton("CONTINUE") { engine.continueGame() }
            MenuButton("NEW GAME") { engine.newGame() }
            DisabledMenuButton("CHAPTER SELECT   // LOCKED")
            DisabledMenuButton("CODEX            // LOCKED")
        }

        Text(
            "PRE-ALPHA // CINEMATIC ENGINE BUILD",
            color = Muted.copy(alpha = .55f),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, Ice.copy(alpha = .5f), RoundedCornerShape(2.dp))
            .background(Panel)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Text(text, color = Color.White, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DisabledMenuButton(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().alpha(.38f).border(1.dp, Muted.copy(alpha = .35f)).padding(18.dp)
    ) {
        Text(text, color = Muted, letterSpacing = 2.sp)
    }
}

@Composable
private fun GameScreen(engine: GameEngine) {
    val scene = engine.currentScene
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    val context = LocalContext.current

    LaunchedEffect(scene.id) { triggerHaptic(context, scene.direction.haptic) }

    Column(
        Modifier.fillMaxSize().padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
    ) {
        Hud(engine.state, scene, onMenu = engine::returnToMenu)
        Spacer(Modifier.height(8.dp))

        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                SceneVisual(scene, Modifier.weight(1.15f).fillMaxHeight())
                Spacer(Modifier.width(10.dp))
                DialoguePanel(
                    scene = scene,
                    state = engine.state,
                    onChoice = engine::choose,
                    modifier = Modifier.weight(.85f).fillMaxHeight()
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                SceneVisual(scene, Modifier.fillMaxWidth().weight(.47f))
                Spacer(Modifier.height(8.dp))
                DialoguePanel(
                    scene = scene,
                    state = engine.state,
                    onChoice = engine::choose,
                    modifier = Modifier.fillMaxWidth().weight(.53f)
                )
            }
        }
    }
}

@Composable
private fun Hud(state: GameState, scene: Scene, onMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Panel).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                scene.direction.location,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.3.sp
            )
            Text(
                "CHAPTER " + scene.chapter + " // " + scene.title,
                color = Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp
            )
        }

        Stat("PWR", state.power, if (state.power < 25) Danger else Ice)
        Spacer(Modifier.width(8.dp))
        Stat("O2", state.oxygen, if (state.oxygen < 25) Danger else Ice)
        Spacer(Modifier.width(8.dp))
        Stat("HULL", state.hull, if (state.hull < 25) Danger else Ice)
        Spacer(Modifier.width(12.dp))

        Text("MENU", color = Muted, fontSize = 9.sp, modifier = Modifier.clickable(onClick = onMenu))
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Text(label, color = Muted, fontSize = 7.sp)
        Text(value.toString() + "%", color = color, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
    }
}

@Composable
private fun SceneVisual(scene: Scene, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "scene")
    val pulse by infinite.animateFloat(
        initialValue = .25f,
        targetValue = .75f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (scene.direction.danger) 900 else 2600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val drift by infinite.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, if (scene.direction.danger) Danger.copy(alpha = .35f) else Ice.copy(alpha = .22f))
            .background(Color(0xFF040914))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF050916),
                        if (scene.direction.danger) Color(0xFF17060A) else Color(0xFF07172A),
                        Color(0xFF010205)
                    )
                )
            )

            repeat(28) { i ->
                val x = ((i * 97f + drift * (1 + i % 3)) % (w + 80f)) - 40f
                val y = ((i * 61f) % h)
                drawCircle(
                    color = Color.White.copy(alpha = .15f + (i % 4) * .08f),
                    radius = if (i % 7 == 0) 2.2f else 1.1f,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }

            val horizonY = h * .68f
            drawLine(
                color = if (scene.direction.danger) Danger.copy(alpha = .35f + pulse * .25f) else Ice.copy(alpha = .22f),
                start = androidx.compose.ui.geometry.Offset(0f, horizonY),
                end = androidx.compose.ui.geometry.Offset(w, horizonY),
                strokeWidth = 2f
            )

            repeat(8) { i ->
                val yy = horizonY + i * (h * .045f)
                drawLine(
                    color = Ice.copy(alpha = .05f),
                    start = androidx.compose.ui.geometry.Offset(0f, yy),
                    end = androidx.compose.ui.geometry.Offset(w, yy),
                    strokeWidth = 1f
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        (if (scene.direction.danger) Danger else Ice).copy(alpha = .26f * pulse),
                        Color.Transparent
                    )
                ),
                radius = minOf(w, h) * .32f,
                center = androidx.compose.ui.geometry.Offset(w * .5f, h * .42f)
            )
        }

        Column(
            Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                scene.direction.artworkKey.uppercase(),
                color = Color.White.copy(alpha = .9f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "CINEMATIC PIXEL SCENE PLACEHOLDER",
                color = Ice.copy(alpha = .55f),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "CAMERA // " + scene.direction.cameraMove.uppercase(),
                color = Muted.copy(alpha = .6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp
            )
        }
        ScanlineOverlay()
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
                listOf(Color.Transparent, Ice.copy(alpha = .045f), Color.Transparent),
                startY = lineY - 70f,
                endY = lineY + 70f
            )
        )
        repeat((size.height / 7f).toInt()) { i ->
            drawLine(
                Color.Black.copy(alpha = .10f),
                androidx.compose.ui.geometry.Offset(0f, i * 7f),
                androidx.compose.ui.geometry.Offset(size.width, i * 7f),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
private fun DialoguePanel(
    scene: Scene,
    state: GameState,
    onChoice: (Choice) -> Unit,
    modifier: Modifier = Modifier
) {
    var revealedCount by remember(scene.id) { mutableIntStateOf(0) }

    LaunchedEffect(scene.id) {
        revealedCount = 0
        scene.lines.forEachIndexed { index, line ->
            delay(line.delayBeforeMs.coerceAtLeast(120L))
            revealedCount = index + 1
        }
    }

    val availableChoices = scene.choices.filter { state.flags.containsAll(it.requiredFlags) }
    val allLinesVisible = revealedCount >= scene.lines.size

    LazyColumn(
        modifier = modifier.background(Panel).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(scene.lines.take(revealedCount)) { _, line -> DialogueLine(line) }

        if (!allLinesVisible) {
            item {
                Text("▌", color = Ice, fontFamily = FontFamily.Monospace, modifier = Modifier.alpha(.8f))
            }
        }

        if (allLinesVisible) {
            item {
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Ice.copy(alpha = .15f)))
            }

            itemsIndexed(availableChoices) { index, choice ->
                ChoiceButton(number = index + 1, choice = choice, onClick = { onChoice(choice) })
            }

            item {
                Spacer(Modifier.height(14.dp))
                Text(
                    "STELLA LINK // " + state.stellaTrust + "%",
                    color = Muted.copy(alpha = .7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
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
        Speaker.PLAYER -> Color(0xFFC9FFB7)
        Speaker.UNKNOWN -> Danger
        Speaker.NARRATOR -> Color.White
    }

    Column {
        if (line.speaker != Speaker.NARRATOR) {
            Text(
                line.speaker.name,
                color = speakerColor.copy(alpha = .72f),
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            line.text,
            color = if (line.speaker == Speaker.NARRATOR) Color.White.copy(alpha = .9f) else speakerColor,
            fontSize = if (line.speaker == Speaker.SYSTEM) 12.sp else 15.sp,
            lineHeight = 21.sp,
            fontFamily = if (line.speaker == Speaker.SYSTEM) FontFamily.Monospace else FontFamily.SansSerif
        )
    }
}

@Composable
private fun ChoiceButton(number: Int, choice: Choice, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, Ice.copy(alpha = .24f), RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp)
    ) {
        Row {
            Text(number.toString(), color = Ice.copy(alpha = .6f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Spacer(Modifier.width(10.dp))
            Text(choice.text, color = Color.White, fontSize = 12.sp, letterSpacing = .7.sp, fontWeight = FontWeight.Medium)
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

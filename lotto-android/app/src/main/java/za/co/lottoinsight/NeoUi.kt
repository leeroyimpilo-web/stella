package za.co.lottoinsight

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import za.co.lottoinsight.model.Draw
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Navy = Color(0xFF06173F)
private val Royal = Color(0xFF062C79)
private val Electric = Color(0xFF087BFA)
private val Emerald = Color(0xFF00CF86)
private val Violet = Color(0xFF7620F5)
private val Gold = Color(0xFFFFD339)
private val Turquoise = Color(0xFF00D9E9)
private val Cream = Color(0xFFF8FAFF)
private val Subdued = Color(0xFFB8D7FA)
private val Johannesburg = ZoneId.of("Africa/Johannesburg")

/**
 * Original Compose widgets modelled on the supplied design reference.
 * All text/cards/buttons are real controls: the screenshot is never used as an app background.
 */
@Composable
fun NeoHome(
    latest: Draw?,
    selected: List<Int>,
    onSelectedChange: (List<Int>) -> Unit,
    onQuickPick: () -> Unit,
    onSaveSelection: () -> Unit,
    onPlay: () -> Unit,
    onResults: () -> Unit,
    onMore: () -> Unit,
    onSync: () -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }
    var jackpotPage by remember { mutableIntStateOf(0) }
    var now by remember { mutableStateOf(ZonedDateTime.now(Johannesburg)) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now(Johannesburg)
            delay(1000)
        }
    }
    val nextDraw = remember(now.toLocalDate(), now.hour) { nextApproximateDraw(now) }
    val remaining = Duration.between(now, nextDraw).seconds.coerceAtLeast(0)
    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Royal, Navy, Color(0xFF071E56), Color(0xFF061638)))
    )) {
        NeoBackground()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 13.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeoHero(onMore, onSync)
            NeoGlass(Modifier.fillMaxWidth(), deep = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("♛", fontSize = 27.sp, color = Gold)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        NeoOverline("ESTIMATED JACKPOT")
                        Text(
                            if (jackpotPage == 0) "R32,000,000" else "Not available",
                            fontSize = if (jackpotPage == 0) 32.sp else 25.sp,
                            fontWeight = FontWeight.Black, color = Gold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (jackpotPage == 0) "DESIGN SAMPLE • NOT LIVE" else "Check official operator",
                            fontSize = 9.sp, letterSpacing = 1.3.sp, color = Turquoise
                        )
                    }
                    Box(
                        Modifier.size(34.dp).background(Color(0x99203760), CircleShape)
                            .clickable { jackpotPage = (jackpotPage + 1) % 5 },
                        contentAlignment = Alignment.Center
                    ) { Text("›", color = Cream, fontSize = 29.sp) }
                }
            }
            Row(
                Modifier.fillMaxWidth().height(11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { dot ->
                    Box(
                        Modifier.padding(horizontal = 4.dp).size(if (dot == jackpotPage) 7.dp else 6.dp)
                            .background(if (dot == jackpotPage) Cream else Electric.copy(alpha = 0.47f), CircleShape)
                            .clickable { jackpotPage = dot }
                    )
                }
            }
            NeoGlass(Modifier.fillMaxWidth(), accent = Emerald) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("▦", color = Cream, fontSize = 25.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        NeoOverline("NEXT DRAW")
                        Text(
                            nextDraw.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)).uppercase(),
                            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                            color = Cream, maxLines = 1
                        )
                        Text("Estimated schedule • closes vary", fontSize = 10.sp, color = Subdued)
                    }
                    Text("›", color = Cream, fontSize = 24.sp, modifier = Modifier.clickable { onPlay() })
                }
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val fields = listOf(
                            (remaining / 86400).toString() to "DAYS",
                            ((remaining / 3600) % 24).toString() to "HRS",
                            ((remaining / 60) % 60).toString() to "MINS",
                            (remaining % 60).toString() to "SECS"
                        )
                        fields.forEach { (n, unit) ->
                            Column(
                                Modifier.weight(1f).border(0.8.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(11.dp))
                                    .background(Color(0x55101E56), RoundedCornerShape(11.dp))
                                    .padding(vertical = 9.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(n, fontWeight = FontWeight.Black, fontSize = 17.sp, color = Cream)
                                Text(unit, fontSize = 8.sp, color = Subdued)
                            }
                        }
                    }
                    Column(Modifier.weight(0.94f), horizontalAlignment = Alignment.CenterHorizontally) {
                        NeonButton("PLAY NOW  ›", Emerald, Color(0xFF71F559), modifier = Modifier.fillMaxWidth()) { onPlay() }
                        Spacer(Modifier.height(6.dp))
                        Text("GENERATE & SAVE PICKS", fontSize = 8.sp, color = Subdued, letterSpacing = 0.8.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            NeoGlass(Modifier.fillMaxWidth(), accent = Violet) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("☑", color = Cream, fontSize = 21.sp)
                    Spacer(Modifier.width(9.dp))
                    NeoOverline("YOUR NUMBERS", Modifier.weight(1f))
                    Text("⌫  Clear All", color = Subdued, fontSize = 11.sp, modifier = Modifier.clickable { onSelectedChange(emptyList()) })
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        if (index < selected.size) {
                            NeonBall(
                                selected[index], ballColor(index), 36.dp,
                                onClick = { pickerOpen = true }
                            )
                        } else {
                            Box(
                                Modifier.size(36.dp).border(1.dp, Subdued.copy(alpha = 0.8f), CircleShape)
                                    .clickable { pickerOpen = true },
                                contentAlignment = Alignment.Center
                            ) { Text("+", color = Cream, fontSize = 22.sp) }
                        }
                    }
                    Box(
                        Modifier.size(34.dp).border(1.dp, Subdued.copy(alpha = 0.8f), CircleShape)
                            .clickable { pickerOpen = true },
                        contentAlignment = Alignment.Center
                    ) { Text("+", color = Cream, fontSize = 21.sp) }
                }
                Spacer(Modifier.height(17.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    NeonButton("⤨  Quick Pick", Violet, Color(0xFFB064FE), modifier = Modifier.weight(1f), onClick = onQuickPick)
                    NeonButton(
                        "➤  Save Numbers", Emerald, Color(0xFF59F45A),
                        modifier = Modifier.weight(1f), enabled = selected.size == 6,
                        onClick = onSaveSelection
                    )
                }
            }
            NeoGlass(Modifier.fillMaxWidth(), accent = Electric) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("♜", fontSize = 20.sp, color = Cream)
                    Spacer(Modifier.width(9.dp))
                    NeoOverline("LATEST RESULTS", Modifier.weight(1f))
                    Text("View All", color = Emerald, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onResults))
                }
                Spacer(Modifier.height(9.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(Color(0x664269AC), RoundedCornerShape(16.dp))
                        .border(0.7.dp, Color(0x558FCBFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 11.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            latest?.date ?: "NO STORED RESULTS",
                            color = Subdued, fontSize = 10.sp, letterSpacing = 0.8.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        if (latest != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                latest.numbers.take(6).forEachIndexed { i, value ->
                                    NeonBall(value, ballColor(i), 29.dp)
                                }
                            }
                        } else Text("Connect once to import historical draws.", color = Cream, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.width(0.7.dp).height(64.dp).background(Cream.copy(alpha = 0.22f)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.width(94.dp)) {
                        Text("HISTORICAL", color = Subdued, fontSize = 9.sp)
                        Text("RESULTS", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Verify with operator", color = Subdued, fontSize = 9.sp)
                    }
                }
            }
            Text(
                "UNOFFICIAL RESEARCH APP  •  18+  •  NO TICKET SALES\n" +
                    "Jackpot on this concept screen is a sample, not a live estimate. All valid combinations have equal odds.",
                color = Subdued.copy(alpha = 0.85f), fontSize = 9.sp, lineHeight = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)
            )
        }
    }
    if (pickerOpen) {
        NeoNumberPicker(selected, onSelectedChange) { pickerOpen = false }
    }
}

private fun nextApproximateDraw(now: ZonedDateTime): ZonedDateTime {
    for (day in 0L..8L) {
        val date = now.toLocalDate().plusDays(day)
        if (date.dayOfWeek == DayOfWeek.WEDNESDAY || date.dayOfWeek == DayOfWeek.SATURDAY) {
            val draw = date.atTime(20, 30).atZone(Johannesburg)
            if (draw.isAfter(now)) return draw
        }
    }
    return now.plusDays(3)
}

@Composable
private fun NeoBackground() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawCircle(
            Brush.radialGradient(listOf(Emerald.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(w * 1.02f, h * 0.36f), radius = w * 0.74f),
            radius = w * 0.74f, center = Offset(w * 1.02f, h * 0.36f)
        )
        drawCircle(
            Brush.radialGradient(listOf(Violet.copy(alpha = 0.37f), Color.Transparent),
                center = Offset(0f, h * 0.63f), radius = w * 0.75f),
            radius = w * 0.75f, center = Offset(0f, h * 0.63f)
        )
        drawCircle(
            Brush.radialGradient(listOf(Electric.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(w * 0.6f, h * 0.9f), radius = w * 0.8f),
            radius = w * 0.8f, center = Offset(w * 0.6f, h * 0.9f)
        )
    }
}

@Composable
private fun NeoHero(onMore: () -> Unit, onSync: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(210.dp)) {
        val full = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val ribbons = listOf(
                Color(0xFFEF1A43), Cream, Color(0xFF00B46A), Gold, Electric, Turquoise
            )
            ribbons.forEachIndexed { index, color ->
                val y = h * (0.61f + index * 0.044f)
                val wave = Path().apply {
                    moveTo(-45f, y - 80f)
                    cubicTo(w * 0.26f, y + 100f, w * 0.56f, y - 72f, w + 85f, y - 135f)
                }
                drawPath(wave, color.copy(alpha = if (index == 3) 0.64f else 0.72f),
                    style = Stroke(width = 13f, cap = StrokeCap.Round))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("☰", modifier = Modifier.padding(top = 17.dp).size(35.dp).clickable(onClick = onMore),
                color = Cream, fontSize = 26.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("♛", color = Gold, fontSize = 20.sp, lineHeight = 21.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SA", fontSize = 31.sp, color = Cream, fontWeight = FontWeight.Black)
                    Text("Lotto", fontSize = 31.sp, color = Gold, fontWeight = FontWeight.Black)
                }
                Text("P L A Y  A  B R I G H T E R", fontSize = 7.sp, color = Cream, lineHeight = 11.sp)
                Text("T O M O R R O W", fontSize = 7.sp, color = Cream, lineHeight = 11.sp)
            }
            Box(Modifier.padding(top = 13.dp).size(35.dp).clickable(onClick = onSync),
                contentAlignment = Alignment.Center) {
                Text("♧", color = Cream, fontSize = 30.sp)
                Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(Color(0xFFFF4D5D), CircleShape))
            }
        }
        Column(Modifier.align(Alignment.TopStart).padding(top = 87.dp, start = 5.dp)) {
            Text("Hi There 👋", color = Cream, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Big dreams start with a ticket.", color = Subdued, fontSize = 11.sp)
        }
        val inf = rememberInfiniteTransition(label = "ball float")
        val floatY by inf.animateFloat(-3f, 4f, infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse), label = "float")
        NeonBall(36, Color(0xFF02BF61), 87.dp, true,
            Modifier.offset(x = full * 0.21f - 8.dp, y = 142.dp + floatY.dp))
        NeonBall(28, Electric, 83.dp, true,
            Modifier.offset(x = full * 0.52f - 2.dp, y = 98.dp - floatY.dp))
        NeonBall(7, Gold, 105.dp, true,
            Modifier.offset(x = full * 0.74f - 14.dp, y = 65.dp + floatY.dp))
        NeonBall(14, Violet, 91.dp, true,
            Modifier.offset(x = full * 0.64f - 2.dp, y = 150.dp - floatY.dp))
    }
}

private fun ballColor(i: Int): Color = listOf(Gold, Electric, Emerald, Violet, Gold, Electric)[i % 6]

@Composable
private fun NeonBall(
    number: Int,
    color: Color,
    diameter: Dp,
    hero: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val base = modifier.size(diameter).shadow(
        if (hero) 13.dp else 5.dp, CircleShape,
        ambientColor = color.copy(alpha = 0.8f), spotColor = color
    )
    Box(
        (if (onClick != null) base.clickable { onClick() } else base).clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val d = size.minDimension
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.85f), color.copy(alpha = 0.99f),
                        color, Color.Black.copy(alpha = 0.86f)),
                    center = Offset(d * 0.24f, d * 0.15f), radius = d * 0.98f
                ),
                radius = d / 2f
            )
            drawCircle(Cream.copy(alpha = 0.60f), radius = d * 0.465f,
                style = Stroke(width = if (hero) 2.5f else 1.5f))
            drawOval(Cream.copy(alpha = 0.40f),
                topLeft = Offset(d * 0.16f, d * 0.07f), size = Size(d * 0.41f, d * 0.17f))
        }
        if (hero) {
            Box(
                Modifier.size(diameter * 0.57f).background(
                    Brush.linearGradient(listOf(Color.White, Color(0xFFDBE2F2))), CircleShape
                ), contentAlignment = Alignment.Center
            ) {
                Text(number.toString(), color = Color(0xFF171A23), fontSize = (diameter.value * 0.30f).sp,
                    fontWeight = FontWeight.Black)
            }
        } else {
            Text(number.toString(), color = if (color == Gold) Color(0xFF111B25) else Cream,
                fontSize = (diameter.value * 0.42f).sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun NeoGlass(
    modifier: Modifier = Modifier,
    accent: Color = Turquoise,
    deep: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.shadow(11.dp, shape, ambientColor = accent.copy(alpha = 0.38f))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.75f),
                accent.copy(alpha = 0.5f), Emerald.copy(alpha = 0.8f))), shape)
            .background(
                Brush.linearGradient(
                    listOf(if (deep) Color(0xD6093467) else Color(0xC319226B),
                        Color(0xCF041C4B), Color(0x9C065953))
                ), shape
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        content = content
    )
}

@Composable
private fun NeoOverline(value: String, modifier: Modifier = Modifier) {
    Text(value, modifier = modifier, color = Cream, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun NeonButton(
    label: String,
    from: Color,
    to: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = CircleShape
    Box(
        modifier.height(39.dp)
            .shadow(6.dp, shape, spotColor = from.copy(alpha = 0.84f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(to, from, from)))
            .border(1.dp, Cream.copy(alpha = 0.58f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (from == Emerald) Color(0xFF001F2B) else Cream,
            fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun NeoNumberPicker(
    selected: List<Int>, onChange: (List<Int>) -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C275C),
        title = { Text("Choose 6 Lotto numbers", color = Cream, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(selected.joinToString(" • ").ifEmpty { "Choose your first number" },
                    color = Gold, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(52) { index ->
                        val number = index + 1
                        val active = number in selected
                        Box(
                            Modifier.aspectRatio(1f)
                                .background(if (active) Emerald else Color(0xFF224C82), CircleShape)
                                .border(1.dp, if (active) Gold else Electric.copy(alpha = 0.48f), CircleShape)
                                .clickable {
                                    onChange(
                                        if (active) selected.filterNot { it == number }
                                        else if (selected.size < 6) (selected + number).sorted()
                                        else selected
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(number.toString(), color = Cream, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE", color = Emerald) }
        },
        dismissButton = {
            TextButton(onClick = { onChange(emptyList()) }) { Text("CLEAR", color = Subdued) }
        }
    )
}

@Composable
fun NeoBottomBar(tab: Int, navigate: (Int) -> Unit) {
    val active = when (tab) { 0 -> 0; 2 -> 1; 1 -> 2; else -> 3 }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF2E087C), Royal, Color(0xFF06467B))))
            .border(1.dp, Color(0xFF81A9FB).copy(alpha = 0.75f), RoundedCornerShape(26.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(Triple("⌂", "Home", 0), Triple("♧", "Play", 2),
            Triple("▥", "Results", 1), Triple("⠿", "More", 5)).forEachIndexed { index, item ->
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable { navigate(item.third) },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.first, fontSize = 23.sp,
                    color = if (index == active) Cream else Subdued,
                    fontWeight = if (index == active) FontWeight.Black else FontWeight.Normal)
                Text(item.second, fontSize = 10.sp,
                    color = if (index == active) Cream else Subdued)
            }
        }
    }
}

@Composable
fun NeoMore(onAnalyse: () -> Unit, onSaved: () -> Unit, onSync: () -> Unit, status: String) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Royal, Navy, Color(0xFF02122F)))
        )
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Text("MORE TOOLS", color = Cream, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text("Your Lotto research dashboard", color = Subdued, fontSize = 12.sp)
            NeoGlass(Modifier.fillMaxWidth(), accent = Violet) {
                Text("▥  Historical number analysis", color = Cream, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onAnalyse).padding(8.dp))
                Text("Frequency analysis of stored official-source draws. Never predicts the next draw.",
                    color = Subdued, fontSize = 11.sp)
            }
            NeoGlass(Modifier.fillMaxWidth(), accent = Emerald) {
                Text("★  Your saved number lines", color = Cream, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSaved).padding(8.dp))
                Text("Offline saved lines and PDF / JPEG export", color = Subdued, fontSize = 11.sp)
            }
            NeoGlass(Modifier.fillMaxWidth(), accent = Electric) {
                Text("↻  Refresh results archive", color = Cream, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSync).padding(8.dp))
                Text(status, color = Subdued, fontSize = 11.sp)
            }
            Text("INDEPENDENT APP • NO TICKET SALES • 18+", color = Gold,
                fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("All valid lines have identical draw odds. Data courtesy of BetTip (bettip.co.za). " +
                "Always verify results with the official lottery operator. " +
                "Responsible gambling helpline: 0800 006 008.",
                color = Subdued, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

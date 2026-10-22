package com.nh.callbrize.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.nh.callbrize.domain.Card
import com.nh.callbrize.domain.Play
import com.nh.callbrize.game.GameState
import com.nh.callbrize.game.Phase
import com.nh.callbrize.navigation.Screen
import com.nh.callbrize.ui.AppViewModel
import com.nh.callbrize.ui.Felt
import com.nh.callbrize.ui.FeltDark
import com.nh.callbrize.ui.FeltLight
import com.nh.callbrize.ui.GlassBrush
import com.nh.callbrize.ui.Gold
import com.nh.callbrize.ui.GoldBrush
import com.nh.callbrize.ui.GoldDim
import com.nh.callbrize.ui.GoldLight
import com.nh.callbrize.ui.GoldRim
import com.nh.callbrize.ui.MidNight
import com.nh.callbrize.ui.Navy
import com.nh.callbrize.ui.SoftText
import com.nh.callbrize.ui.components.AvatarBubble
import com.nh.callbrize.ui.components.CardBack
import com.nh.callbrize.ui.components.GoldButton
import com.nh.callbrize.ui.components.OutlineGoldButton
import com.nh.callbrize.ui.components.PlayingCard
import com.nh.callbrize.util.fmt
import kotlin.math.roundToInt

@Composable
fun GameScreen(vm: AppViewModel) {
    val g = vm.game ?: return
    val settings by vm.settings.collectAsState()
    val viewer = vm.viewerSeat
    val anim = settings.animations
    val canAct = g.phase == Phase.PLAYING && !g.trickPending && g.turn == viewer &&
        vm.handoverSeat == null && !vm.paused
    val legal = if (canAct) vm.legalNow() else emptyList()
    val bidding = g.phase == Phase.BIDDING && g.turn == viewer && vm.handoverSeat == null && !vm.paused

    val status = vm.toast ?: when {
        g.phase == Phase.BIDDING ->
            if (g.turn == viewer) "Your bid, ${g.players[viewer].name}" else "${g.players[g.turn].name} is bidding…"
        g.trickPending -> "${g.players[g.pendingWinner ?: 0].name} wins the trick!"
        g.phase != Phase.PLAYING -> "Round complete"
        g.turn == viewer -> "▲  Your turn: swipe a card up to play"
        else -> "${g.players[g.turn].name} is thinking…"
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(g, vm)
            Scoreboard(g)
            Table(
                g = g, viewer = viewer, anim = anim, showBid = bidding,
                minBid = vm.rules.minBid, maxBid = vm.rules.maxBid, onBid = { vm.humanBid(it) },
                modifier = Modifier.weight(1f)
            )
            StatusPill(status, highlight = vm.toast != null || canAct)
            HandRow(
                cards = g.hands[viewer], legal = legal.toSet(), selected = vm.selected,
                canAct = canAct, anim = anim, onTap = { vm.onCardTapped(it) }, onPlay = { vm.playCard(it) }
            )
        }
        val hand = vm.handoverSeat
        if (hand != null) HandoverOverlay(g.players[hand].name) { vm.confirmHandover() }
        if (vm.paused && hand == null) PauseDialog(vm)
    }
}

@Composable
private fun StatusPill(text: String, highlight: Boolean) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.clip(RoundedCornerShape(50))
                .background(if (highlight) Color(0x33E0B341) else Color(0x22FFFFFF))
                .border(1.dp, if (highlight) Gold else GoldDim, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text, color = if (highlight) GoldLight else SoftText, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2
            )
        }
    }
}

@Composable
private fun CircleBtn(label: String, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).padding(2.dp).clip(CircleShape).background(GlassBrush)
            .border(1.dp, GoldDim, CircleShape).clickable(onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center
    ) { Text(label, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun TopBar(g: GameState, vm: AppViewModel) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleBtn("❚❚", "Pause") { vm.pause() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(GlassBrush)
                    .border(1.dp, GoldRim, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    "ROUND ${g.roundIndex + 1} / ${g.totalRounds}   ·   ♠ TRUMP",
                    color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
            }
        }
        CircleBtn("⚙", "Settings") { vm.pause(); vm.nav.go(Screen.SettingsScreen) }
    }
}

@Composable
private fun Scoreboard(g: GameState) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (seat in g.players.indices) {
            val active = g.turn == seat && g.phase != Phase.ROUND_OVER && g.phase != Phase.MATCH_OVER
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier.weight(1f).clip(shape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF1F3073), Color(0xFF0F1A45))))
                    .border(if (active) 2.dp else 1.dp, if (active) SolidGold else SolidDim, shape)
                    .padding(vertical = 5.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(g.players[seat].name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    g.totals[seat].fmt(), style = androidx.compose.ui.text.TextStyle(
                        brush = GoldBrush, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold
                    )
                )
                Text("Bid ${g.bids[seat] ?: "-"} · Won ${g.tricksWon[seat]}", color = SoftText, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

private val SolidGold = androidx.compose.ui.graphics.SolidColor(Gold)
private val SolidDim = androidx.compose.ui.graphics.SolidColor(Color(0x558C7430))

@Composable
private fun Table(
    g: GameState, viewer: Int, anim: Boolean, showBid: Boolean,
    minBid: Int, maxBid: Int, onBid: (Int) -> Unit, modifier: Modifier
) {
    val n = g.players.size
    val outer = RoundedCornerShape(24.dp)
    val inner = RoundedCornerShape(21.dp)
    Box(
        modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
            .shadow(10.dp, outer)
            .clip(outer)
            .background(GoldRim)
            .padding(3.dp)
    ) {
        Box(
            Modifier.fillMaxSize().clip(inner)
                .background(Brush.radialGradient(listOf(FeltLight, Felt, FeltDark)))
                .border(1.dp, Color(0x44F6D77A), inner)
        ) {
            Text("♠", color = Color(0x12FFFFFF), fontSize = 110.sp, modifier = Modifier.align(Alignment.Center))
            for (rel in 0 until n) {
                val seat = (viewer + rel) % n
                val align = when (rel) {
                    0 -> Alignment.BottomCenter
                    1 -> Alignment.CenterStart
                    2 -> Alignment.TopCenter
                    else -> Alignment.CenterEnd
                }
                SeatBadge(g = g, seat = seat, viewer = viewer, anim = anim, modifier = Modifier.align(align).padding(6.dp))
            }
            g.currentTrick.forEach { play ->
                key(play.card.key) {
                    PlayedCard(
                        play = play, rel = (play.seat - viewer + n) % n,
                        highlight = g.trickPending && g.pendingWinner == play.seat, anim = anim,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            if (showBid) {
                val panel = RoundedCornerShape(22.dp)
                Column(
                    Modifier.align(Alignment.Center).padding(12.dp)
                        .shadow(16.dp, panel)
                        .clip(panel)
                        .background(Brush.verticalGradient(listOf(Color(0xFF1B2A66), Navy)))
                        .border(1.5.dp, GoldRim, panel)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Place your bid", color = GoldLight, fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("How many tricks will you win?", color = SoftText, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp))
                    (minBid..maxBid).chunked(5).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                            row.forEach { b ->
                                Box(
                                    Modifier.size(40.dp).shadow(4.dp, CircleShape).clip(CircleShape)
                                        .background(GoldBrush)
                                        .clickable { onBid(b) }
                                        .semantics { contentDescription = "Bid $b" },
                                    contentAlignment = Alignment.Center
                                ) { Text("$b", color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatBadge(g: GameState, seat: Int, viewer: Int, anim: Boolean, modifier: Modifier) {
    val p = g.players[seat]
    val active = g.turn == seat && (g.phase == Phase.BIDDING || (g.phase == Phase.PLAYING && !g.trickPending))
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            if (active && anim) {
                val pulse = rememberInfiniteTransition(label = "pulse")
                val glow by pulse.animateFloat(
                    initialValue = 0.35f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "glow"
                )
                Box(
                    Modifier.size(52.dp).background(
                        Brush.radialGradient(listOf(Gold.copy(alpha = 0.6f * glow), Color.Transparent)), CircleShape
                    )
                )
            }
            AvatarBubble(p.avatar, 38.dp, active = active)
        }
        Box(
            Modifier.padding(top = 2.dp).clip(RoundedCornerShape(50)).background(Color(0x99000000))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(p.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 72.dp))
        }
        Row(
            Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (seat != viewer) CardBack(12.dp)
            Text("${g.hands[seat].size}", color = SoftText, fontSize = 11.sp)
            val bid = g.bids[seat]
            if (bid != null) {
                Text("${g.tricksWon[seat]}/$bid", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlayedCard(play: Play, rel: Int, highlight: Boolean, anim: Boolean, modifier: Modifier) {
    val p = remember { Animatable(if (anim) 0f else 1f) }
    LaunchedEffect(Unit) { if (anim) p.animateTo(1f, tween(280)) }
    val fromX = when (rel) { 1 -> -90f; 3 -> 90f; else -> 0f }
    val fromY = when (rel) { 0 -> 120f; 2 -> -120f; else -> 0f }
    val targetX = when (rel) { 1 -> -50f; 3 -> 50f; else -> 0f }
    val targetY = when (rel) { 0 -> 34f; 2 -> -34f; else -> 0f }
    Box(
        modifier
            .offset {
                IntOffset(
                    (targetX + fromX * (1f - p.value)).dp.toPx().roundToInt(),
                    (targetY + fromY * (1f - p.value)).dp.toPx().roundToInt()
                )
            }
            .graphicsLayer { alpha = 0.2f + 0.8f * p.value }
    ) {
        PlayingCard(play.card, 46.dp, highlight = highlight)
    }
}

/**
 * The player's hand. Tap lifts a card; swipe a card up past the threshold to play it.
 * Illegal cards are dimmed and bounce back (the engine still validates every move).
 */
@Composable
private fun HandRow(
    cards: List<Card>, legal: Set<Card>, selected: Card?, canAct: Boolean, anim: Boolean,
    onTap: (Card) -> Unit, onPlay: (Card) -> Unit
) {
    val thresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(100.dp)) {
        val n = cards.size
        val w: Dp = 52.dp
        val avail = maxWidth
        val step: Dp = if (n <= 1) 0.dp else minOf(w * 0.78f, (avail - w) / (n - 1))
        val total: Dp = if (n == 0) 0.dp else w + step * (n - 1)
        val start: Dp = (avail - total) / 2
        cards.forEachIndexed { i, c ->
            key(c.key) {
                val entry = remember { Animatable(if (anim) 0f else 1f) }
                LaunchedEffect(Unit) {
                    if (anim) {
                        kotlinx.coroutines.delay(i * 35L)
                        entry.animateTo(1f, tween(250))
                    }
                }
                var dragY by remember { mutableStateOf(0f) }
                var dragging by remember { mutableStateOf(false) }
                val settle by animateFloatAsState(
                    targetValue = dragY,
                    animationSpec = if (dragging) snap() else spring(),
                    label = "drag"
                )
                val lift = if (c == selected) 2.dp else 18.dp
                val dim = canAct && c !in legal
                val armed = dragging && dragY < -thresholdPx
                val dragModifier = if (canAct) {
                    Modifier.pointerInput(c) {
                        detectVerticalDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = {
                                val fire = dragY < -thresholdPx
                                dragging = false
                                dragY = 0f
                                if (fire) onPlay(c)
                            },
                            onDragCancel = { dragging = false; dragY = 0f },
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                dragY = (dragY + amount).coerceIn(-420f, 0f)
                            }
                        )
                    }
                } else Modifier
                Box(
                    Modifier
                        .offset(x = start + step * i, y = lift)
                        .zIndex(if (dragging || settle < -2f) 10f else 0f)
                        .graphicsLayer { alpha = entry.value; translationY = (1f - entry.value) * 60f + settle }
                        .alpha(if (dim) 0.45f else 1f)
                        .then(dragModifier)
                        .clickable { onTap(c) }
                ) {
                    PlayingCard(c, w, highlight = c == selected || armed)
                }
            }
        }
    }
}

@Composable
private fun HandoverOverlay(nextName: String, onReady: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Navy).pointerInput(Unit) { detectTapGestures { } }.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("🔒", fontSize = 56.sp)
            Text("Pass the phone to", color = SoftText, fontSize = 16.sp)
            Text(nextName, color = Gold, fontSize = 32.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text("Cards are hidden. Only tap when only $nextName can see the screen.",
                color = SoftText, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            GoldButton("I'm $nextName: show my cards", onReady)
        }
    }
}

@Composable
private fun PauseDialog(vm: AppViewModel) {
    Dialog(onDismissRequest = { vm.resume() }) {
        Surface(shape = RoundedCornerShape(26.dp), color = MidNight, border = BorderStroke(1.5.dp, GoldRim)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Paused", color = Gold, fontSize = 26.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.ExtraBold)
                GoldButton("Resume", { vm.resume() })
                OutlineGoldButton("How to Play", { vm.nav.go(Screen.HowToPlay) })
                OutlineGoldButton("Settings", { vm.nav.go(Screen.SettingsScreen) })
                OutlineGoldButton("Quit to Menu", { vm.quitToMenu() })
            }
        }
    }
}

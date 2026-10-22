package com.nh.callbrize.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.nh.callbrize.ui.GoldBrush
import com.nh.callbrize.ui.GoldRim
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.callbrize.ai.Difficulty
import com.nh.callbrize.navigation.Screen
import com.nh.callbrize.settings.Avatars
import com.nh.callbrize.ui.AppViewModel
import com.nh.callbrize.ui.Gold
import com.nh.callbrize.ui.GoldDim
import com.nh.callbrize.ui.GoldLight
import com.nh.callbrize.ui.GameMode
import com.nh.callbrize.ui.Panel
import com.nh.callbrize.ui.SoftText
import com.nh.callbrize.ui.components.AvatarBubble
import com.nh.callbrize.ui.components.BodyText
import com.nh.callbrize.ui.components.GlassPanel
import com.nh.callbrize.ui.components.GoldButton
import com.nh.callbrize.ui.components.OutlineGoldButton
import com.nh.callbrize.ui.components.ScreenFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun Logo(sizeSp: Int = 64) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size((sizeSp * 1.9f).dp)
                .background(Brush.radialGradient(listOf(Color(0x55E0B341), Color.Transparent)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size((sizeSp * 1.4f).dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF1B2A66), Color(0xFF0A1330))))
                    .border(2.dp, GoldRim, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("♛", style = TextStyle(brush = GoldBrush, fontSize = sizeSp.sp))
            }
        }
        Text(
            "CALLBRIZE",
            style = TextStyle(
                brush = GoldBrush, fontSize = (sizeSp * 0.46f).sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black, letterSpacing = 5.sp
            )
        )
        Text("INDIAN CARD GAME", color = SoftText, fontSize = 12.sp, letterSpacing = 4.sp)
        Text("♠  ♥  ♣  ♦", color = Gold.copy(alpha = 0.7f), fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun SplashScreen(vm: AppViewModel) {
    val animate = vm.settings.collectAsState().value.animations
    val scale = remember { Animatable(0.6f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (animate) {
            launch { scale.animateTo(1f, tween(900)) }
            fade.animateTo(1f, tween(900))
        } else {
            scale.snapTo(1f)
            fade.snapTo(1f)
        }
        delay(1200)
        vm.nav.replaceAll(Screen.Menu)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = fade.value }) {
            Logo(60)
        }
    }
}

@Composable
fun MenuScreen(vm: AppViewModel) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Logo(44)
        Spacer(Modifier.height(8.dp))
        val note = vm.notice
        if (note != null) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0x33E0B341))
                    .border(1.dp, Gold, RoundedCornerShape(50))
                    .clickable { vm.dismissNotice() }.padding(horizontal = 14.dp, vertical = 8.dp)
            ) { Text(note, color = GoldLight, fontSize = 13.sp) }
        }
        GoldButton("Play", { vm.nav.go(Screen.ModeSelect) })
        OutlineGoldButton("How to Play", { vm.nav.go(Screen.HowToPlay) })
        OutlineGoldButton("Statistics & History", { vm.nav.go(Screen.StatsScreen) })
        OutlineGoldButton("Settings", { vm.nav.go(Screen.SettingsScreen) })
        OutlineGoldButton("About & Privacy", { vm.nav.go(Screen.About) })
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun ModeScreen(vm: AppViewModel) {
    ScreenFrame("Choose Mode", { vm.nav.back() }) {
        GoldButton(
            "Single Player", {
                vm.selectMode(GameMode.SINGLE)
                vm.nav.go(Screen.DifficultySelect)
            },
            subtitle = "You vs 3 computer opponents"
        )
        OutlineGoldButton(
            "Pass-and-Play", {
                vm.selectMode(GameMode.PASS_AND_PLAY)
                vm.nav.go(Screen.Setup)
            },
            subtitle = "4 people, one device, hidden hands"
        )
        OutlineGoldButton(
            "Wi-Fi / Bluetooth Play", { vm.nav.go(Screen.NetMenu) },
            subtitle = "Play with nearby phones"
        )
        OutlineGoldButton("Internet Online", {}, enabled = false, subtitle = "Coming Soon")
    }
}

@Composable
fun DifficultyScreen(vm: AppViewModel) {
    val current = vm.settings.collectAsState().value.difficulty
    ScreenFrame("Difficulty", { vm.nav.back() }) {
        Difficulty.values().forEach { d ->
            val chosen = d == current
            val onPick = {
                vm.setDifficulty(d)
                vm.nav.go(Screen.Setup)
            }
            if (chosen) GoldButton(d.label, onPick, subtitle = d.blurb)
            else OutlineGoldButton(d.label, onPick, subtitle = d.blurb)
        }
    }
}

@Composable
fun SetupScreen(vm: AppViewModel) {
    val saved = vm.settings.collectAsState().value
    var name by remember { mutableStateOf(saved.name) }
    var avatar by remember { mutableStateOf(saved.avatar) }
    val pass = vm.mode == GameMode.PASS_AND_PLAY

    ScreenFrame("Player Setup", { vm.nav.back() }) {
        GlassPanel {
            Text("Your name", color = GoldLight, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(16) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            Text("Avatar", color = GoldLight, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
            for (row in 0 until 2) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 0 until 4) {
                        val idx = row * 4 + col
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).clickable { avatar = idx },
                            contentAlignment = Alignment.Center
                        ) { AvatarBubble(idx, 52.dp, active = idx == avatar) }
                    }
                }
            }
        }
        if (pass) {
            GlassPanel {
                Text("Other players", color = GoldLight, fontWeight = FontWeight.Bold)
                for (i in 0 until 3) {
                    OutlinedTextField(
                        value = vm.passNames[i],
                        onValueChange = { vm.setPassName(i, it) },
                        singleLine = true,
                        label = { Text("Player ${i + 2}") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
        GoldButton("Start Game", {
            vm.startMatch(name, avatar, saved.difficulty)
            vm.nav.go(Screen.Game)
        })
    }
}

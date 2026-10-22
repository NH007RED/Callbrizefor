package com.nh.callbrize.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.callbrize.navigation.Screen
import com.nh.callbrize.ui.screens.AboutScreen
import com.nh.callbrize.ui.screens.DifficultyScreen
import com.nh.callbrize.ui.screens.GameScreen
import com.nh.callbrize.ui.screens.HostLobbyScreen
import com.nh.callbrize.ui.screens.HowToPlayScreen
import com.nh.callbrize.ui.screens.JoinScreen
import com.nh.callbrize.ui.screens.NetMenuScreen
import com.nh.callbrize.ui.screens.MatchResultsScreen
import com.nh.callbrize.ui.screens.MenuScreen
import com.nh.callbrize.ui.screens.ModeScreen
import com.nh.callbrize.ui.screens.RoundResultsScreen
import com.nh.callbrize.ui.screens.SettingsScreen
import com.nh.callbrize.ui.screens.SetupScreen
import com.nh.callbrize.ui.screens.SplashScreen
import com.nh.callbrize.ui.screens.StatsScreen

@Composable
fun AppRoot(vm: AppViewModel) {
    val screen = vm.nav.current
    val settings by vm.settings.collectAsState()
    val activity = LocalContext.current as? Activity

    BackHandler(enabled = screen != Screen.Splash) {
        when (screen) {
            Screen.Game -> vm.pause()
            Screen.RoundResults, Screen.MatchResults -> vm.quitToMenu()
            Screen.Menu -> activity?.finish()
            Screen.HostLobby, Screen.JoinGame -> vm.leaveLobby()
            else -> vm.nav.back()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BgBrush)
            .drawBehind {
                val c = Offset(size.width / 2f, 0f)
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0x44E0B341), Color.Transparent), center = c, radius = size.width),
                    radius = size.width,
                    center = c
                )
            }
    ) {
        // Very faint suit watermarks for depth (decorative only).
        Text("♠", color = Color(0x0BFFFFFF), fontSize = 220.sp, modifier = Modifier.align(Alignment.BottomEnd))
        Text("♥", color = Color(0x0BFFFFFF), fontSize = 180.sp, modifier = Modifier.align(Alignment.CenterStart))
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Crossfade(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxHeight(),
                targetState = screen,
                animationSpec = if (settings.animations) tween(280) else snap(),
                label = "screens"
            ) { s ->
                when (s) {
                    Screen.Splash -> SplashScreen(vm)
                    Screen.Menu -> MenuScreen(vm)
                    Screen.ModeSelect -> ModeScreen(vm)
                    Screen.NetMenu -> NetMenuScreen(vm)
                    Screen.HostLobby -> HostLobbyScreen(vm)
                    Screen.JoinGame -> JoinScreen(vm)
                    Screen.DifficultySelect -> DifficultyScreen(vm)
                    Screen.Setup -> SetupScreen(vm)
                    Screen.Game -> GameScreen(vm)
                    Screen.RoundResults -> RoundResultsScreen(vm)
                    Screen.MatchResults -> MatchResultsScreen(vm)
                    Screen.HowToPlay -> HowToPlayScreen(vm)
                    Screen.StatsScreen -> StatsScreen(vm)
                    Screen.SettingsScreen -> SettingsScreen(vm)
                    Screen.About -> AboutScreen(vm)
                }
            }
        }
    }
}

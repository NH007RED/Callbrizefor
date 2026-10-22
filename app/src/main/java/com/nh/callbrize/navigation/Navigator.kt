package com.nh.callbrize.navigation

import androidx.compose.runtime.mutableStateListOf

sealed class Screen {
    object Splash : Screen()
    object Menu : Screen()
    object ModeSelect : Screen()
    object NetMenu : Screen()
    object HostLobby : Screen()
    object JoinGame : Screen()
    object DifficultySelect : Screen()
    object Setup : Screen()
    object Game : Screen()
    object RoundResults : Screen()
    object MatchResults : Screen()
    object HowToPlay : Screen()
    object StatsScreen : Screen()
    object SettingsScreen : Screen()
    object About : Screen()
}

/** Tiny back-stack navigator (no external navigation dependency needed). */
class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Splash)

    val current: Screen get() = stack.last()

    fun go(s: Screen) { stack.add(s) }

    fun back(): Boolean {
        if (stack.size > 1) { stack.removeAt(stack.lastIndex); return true }
        return false
    }

    fun replace(s: Screen) { stack[stack.lastIndex] = s }

    fun replaceAll(s: Screen) { stack.clear(); stack.add(s) }
}

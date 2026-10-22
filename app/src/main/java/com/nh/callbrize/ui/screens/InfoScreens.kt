package com.nh.callbrize.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.callbrize.ai.Difficulty
import com.nh.callbrize.ui.AppViewModel
import com.nh.callbrize.ui.Gold
import com.nh.callbrize.ui.GoldLight
import com.nh.callbrize.ui.SoftText
import com.nh.callbrize.ui.components.BodyText
import com.nh.callbrize.ui.components.GlassPanel
import com.nh.callbrize.ui.components.GoldButton
import com.nh.callbrize.ui.components.OutlineGoldButton
import com.nh.callbrize.ui.components.ScreenFrame
import com.nh.callbrize.ui.components.SectionTitle
import com.nh.callbrize.util.fmt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HowToPlayScreen(vm: AppViewModel) {
    ScreenFrame("Rules & How to Play", { vm.nav.back() }) {
        GlassPanel {
            SectionTitle("About these rules")
            BodyText(
                "No verifiable, documented ruleset called \"Callbrize\" could be established, so this app " +
                    "does not claim these are official Callbrize rules. The default is a popular " +
                    "Call-Break-style trick-taking game. The rules are isolated in one class " +
                    "(TrickTakingRules / GameRules) so they can be changed easily."
            )
        }
        GlassPanel {
            SectionTitle("Setup")
            BodyText("4 players, a standard 52-card deck, 13 cards each. A match is 5 rounds. Spades (♠) are always trump.")
            SectionTitle("1. Bidding")
            BodyText("Look at your hand and bid how many tricks (1 to 13) you expect to win. Bidding goes clockwise.")
            SectionTitle("2. Playing")
            BodyText(
                "The player after the dealer leads any card. Everyone must:\n" +
                    "• follow the lead suit and play a higher card than any played so far if they can;\n" +
                    "• otherwise follow suit with any card;\n" +
                    "• if they have no cards of the lead suit, play a spade (higher than any spade already played, if possible);\n" +
                    "• if they have no spades either, play any card."
            )
            SectionTitle("3. Winning a trick")
            BodyText("The highest spade wins; if no spade was played, the highest card of the lead suit wins. The winner leads the next trick.")
            SectionTitle("4. Scoring")
            BodyText("Win at least your bid: you score your bid plus 0.1 for every extra trick (bid 3, win 5 = 3.2). Win fewer: you lose your bid (bid 3, win 2 = -3.0). Highest total after 5 rounds wins.")
            SectionTitle("Wi-Fi / Bluetooth play")
            BodyText("One phone hosts, up to 3 friends join; empty seats are played by the computer. Wi-Fi: all phones on the same Wi-Fi or the host's hotspot, friends type the host's address. Bluetooth: pair the phones in Android Settings first, then friends tap the host's phone.")
            SectionTitle("Using the app")
            BodyText("Tap a card to lift it, then swipe it up with your finger to play it. Dimmed cards are not legal right now and will bounce back. In Pass-and-Play a handover screen hides cards between turns.")
        }
    }
}

@Composable
fun StatsScreen(vm: AppViewModel) {
    val stats by vm.stats.collectAsState()
    val history by vm.history.collectAsState()
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    ScreenFrame("Statistics", { vm.nav.back() }) {
        GlassPanel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatBox("Played", "${stats.played}")
                StatBox("Wins", "${stats.wins}")
                StatBox("Losses", "${stats.losses}")
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val rate = if (stats.played > 0) stats.wins * 100 / stats.played else 0
                StatBox("Win rate", "$rate%")
                StatBox("Best score", stats.bestScore?.fmt() ?: "-")
            }
            BodyText("Statistics count Single Player matches only.", modifier = Modifier.padding(top = 10.dp))
        }
        SectionTitle("Match history")
        if (history.isEmpty()) {
            BodyText("No matches yet. Play one!")
        } else {
            history.forEach { e ->
                GlassPanel {
                    Text("${e.mode}${if (e.difficulty != "-") " · ${e.difficulty}" else ""}", color = GoldLight, fontWeight = FontWeight.Bold)
                    BodyText("${fmt.format(Date(e.time))}")
                    BodyText("Winner: ${e.winner}   ·   Your score: ${e.humanScore.fmt()} (rank ${e.rank})")
                    BodyText("Players: ${e.players}")
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Gold, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = SoftText, fontSize = 13.sp)
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = SoftText, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    ScreenFrame("Settings", { vm.nav.back() }) {
        GlassPanel {
            SwitchRow("Sound effects", "Card, trick and result sounds", s.sound) { vm.setSound(it) }
            SwitchRow("Vibration", "Haptic feedback when playing cards", s.vibration) { vm.setVibration(it) }
            SwitchRow("Animations", "Turn off for reduced motion", s.animations) { vm.setAnimations(it) }
        }
        GlassPanel {
            SectionTitle("Default difficulty")
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.values().forEach { d ->
                    if (d == s.difficulty) GoldButton(d.label, { vm.setDifficulty(d) }, Modifier.weight(1f))
                    else OutlineGoldButton(d.label, { vm.setDifficulty(d) }, Modifier.weight(1f))
                }
            }
        }
        OutlineGoldButton("Reset statistics & history", { confirm = true })
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Reset statistics?") },
            text = { Text("This permanently deletes your saved statistics and match history on this device.") },
            confirmButton = { TextButton(onClick = { vm.clearStats(); confirm = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AboutScreen(vm: AppViewModel) {
    ScreenFrame("About & Privacy", { vm.nav.back() }) {
        GlassPanel {
            SectionTitle("Callbrize 1.0")
            BodyText("An offline Indian-style trick-taking card game. Single Player against three on-device computer opponents, or Pass-and-Play on one phone.")
        }
        GlassPanel {
            SectionTitle("Privacy")
            BodyText(
                "• No account, ads, analytics or tracking.\n" +
                    "• Single Player and Pass-and-Play never use the network.\n" +
                    "• Wi-Fi play talks directly to a friend's phone on your local network or hotspot; Bluetooth play talks directly to a paired phone. The app never contacts any server on the internet. The INTERNET permission is needed only for those local Wi-Fi connections.\n" +
                    "• Your name, avatar, settings, statistics and match history are stored only on this device and are removed when you uninstall the app or reset them in Settings.\n" +
                    "• Sound effects are generated for this app and bundled locally."
            )
        }
        GlassPanel {
            SectionTitle("Internet online play")
            BodyText("Not available. It is shown as \"Coming Soon\" and is not faked.")
        }
    }
}

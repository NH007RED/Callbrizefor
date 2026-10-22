package com.nh.callbrize.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.callbrize.game.GameState
import com.nh.callbrize.navigation.Screen
import com.nh.callbrize.ui.AppViewModel
import com.nh.callbrize.ui.Gold
import com.nh.callbrize.ui.GameMode
import com.nh.callbrize.ui.GoldLight
import com.nh.callbrize.ui.Panel
import com.nh.callbrize.ui.SoftText
import com.nh.callbrize.ui.components.AvatarBubble
import com.nh.callbrize.ui.components.BodyText
import com.nh.callbrize.ui.components.GlassPanel
import com.nh.callbrize.ui.components.GoldButton
import com.nh.callbrize.ui.components.OutlineGoldButton
import com.nh.callbrize.ui.components.ScreenFrame
import com.nh.callbrize.util.fmt

/** Simple 5-column table row: label + one column per player. */
@Composable
private fun TableRow(label: String, values: List<String>, bold: Boolean = false, color: Color = Color.White) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SoftText, fontSize = 13.sp, modifier = Modifier.weight(1.1f))
        values.forEach { v ->
            Text(
                v, color = color, fontSize = 14.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScoreTable(g: GameState, showRound: Boolean) {
    val last = g.history.lastOrNull()
    GlassPanel {
        TableRow("", g.players.map { it.name }, bold = true, color = GoldLight)
        if (showRound && last != null) {
            TableRow("Bid", last.bids.map { "$it" })
            TableRow("Won", last.tricks.map { "$it" })
            TableRow("Round", last.scores.map { (if (it > 0) "+" else "") + it.fmt() }, bold = true)
        }
        TableRow("Total", g.totals.map { it.fmt() }, bold = true, color = Gold)
    }
}

@Composable
fun RoundResultsScreen(vm: AppViewModel) {
    val g = vm.game ?: return
    ScreenFrame("Round ${g.roundIndex + 1} Results", null) {
        ScoreTable(g, showRound = true)
        if (vm.mode == GameMode.CLIENT) {
            BodyText("Waiting for the host to start the next round…")
        } else {
            GoldButton("Next Round", { vm.nextRound() })
        }
        OutlineGoldButton("Quit to Menu", { vm.quitToMenu() })
    }
}

@Composable
fun MatchResultsScreen(vm: AppViewModel) {
    val g = vm.game ?: return
    val order = g.players.indices.sortedByDescending { g.totals[it] }
    val best = g.totals.max()
    val topSeats = g.players.indices.filter { g.totals[it] == best }
    val pass = vm.mode == GameMode.PASS_AND_PLAY
    val headline = when {
        pass -> "${g.players[topSeats.first()].name} wins! 🏆"
        vm.me in topSeats -> "You won! 🏆"
        else -> "${g.players[topSeats.first()].name} won the match"
    }
    ScreenFrame("Match Results", null) {
        Text(headline, color = Gold, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        GlassPanel {
            order.forEachIndexed { pos, seat ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("${pos + 1}", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    AvatarBubble(g.players[seat].avatar, 34.dp, active = pos == 0)
                    Text(g.players[seat].name, color = Color.White, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(g.totals[seat].fmt(), color = GoldLight, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        ScoreTable(g, showRound = false)
        if (vm.mode == GameMode.CLIENT) {
            BodyText("Waiting for the host to start another match…")
        } else {
            GoldButton("Play Again", { vm.rematch() })
        }
        OutlineGoldButton("Main Menu", { vm.quitToMenu() })
    }
}

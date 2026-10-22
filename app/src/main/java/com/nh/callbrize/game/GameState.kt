package com.nh.callbrize.game

import com.nh.callbrize.domain.Card
import com.nh.callbrize.domain.Play

enum class Phase { BIDDING, PLAYING, ROUND_OVER, MATCH_OVER }

data class PlayerInfo(val name: String, val avatar: Int, val isHuman: Boolean) {
    override fun toString(): String = name
}

data class RoundResult(val bids: List<Int>, val tricks: List<Int>, val scores: List<Double>)

data class ActionResult(val ok: Boolean, val error: String? = null) {
    companion object {
        val OK = ActionResult(true)
        fun fail(message: String) = ActionResult(false, message)
    }
}

/** Immutable snapshot of a match. The [GameEngine] is the only thing that creates new ones. */
data class GameState(
    val players: List<PlayerInfo>,
    val hands: List<List<Card>>,
    val bids: List<Int?>,
    val tricksWon: List<Int>,
    val currentTrick: List<Play>,
    val trickPending: Boolean,
    val pendingWinner: Int?,
    val lastTrickWinner: Int?,
    val phase: Phase,
    val turn: Int,
    val dealer: Int,
    val roundIndex: Int,
    val totalRounds: Int,
    val totals: List<Double>,
    val history: List<RoundResult>
)

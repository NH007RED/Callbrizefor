package com.nh.callbrize.ai

import com.nh.callbrize.domain.Card
import com.nh.callbrize.domain.Play
import com.nh.callbrize.domain.Rank
import com.nh.callbrize.game.GameRules
import kotlin.random.Random

enum class Difficulty(val label: String, val blurb: String) {
    EASY("Easy", "Relaxed opponents that play legal cards at random."),
    MEDIUM("Medium", "Opponents that try to win every trick with the cheapest winning card."),
    HARD("Hard", "Opponents that count their bid and play to hit it exactly.")
}

/** On-device AI. It only ever picks cards from the rules' own legal-card list. */
class ComputerPlayer(
    private val rules: GameRules,
    val difficulty: Difficulty,
    private val random: Random = Random.Default
) {

    fun chooseBid(hand: List<Card>): Int {
        val trump = rules.trumpSuit
        var estimate = 0.0
        for (c in hand) {
            val isTrump = c.suit == trump
            estimate += when {
                c.rank == Rank.ACE -> 0.95
                c.rank == Rank.KING -> if (isTrump) 0.85 else 0.55
                c.rank == Rank.QUEEN -> if (isTrump) 0.6 else 0.25
                isTrump && c.rank.value >= 8 -> 0.35
                isTrump -> 0.15
                else -> 0.0
            }
        }
        val noise = when (difficulty) {
            Difficulty.EASY -> random.nextInt(-2, 3)
            Difficulty.MEDIUM -> random.nextInt(-1, 2)
            Difficulty.HARD -> 0
        }
        val raw = Math.floor(estimate + 0.3).toInt() + noise
        return raw.coerceIn(rules.minBid, minOf(rules.maxBid, 8))
    }

    fun chooseCard(seat: Int, hand: List<Card>, trick: List<Play>, bid: Int, tricksWon: Int): Card {
        val legal = rules.legalCards(hand, trick)
        require(legal.isNotEmpty()) { "No legal cards" }
        if (legal.size == 1) return legal[0]
        if (difficulty == Difficulty.EASY) return legal.random(random)

        val needTricks = difficulty == Difficulty.MEDIUM || tricksWon < bid
        fun cost(c: Card): Int = (if (c.suit == rules.trumpSuit) 100 else 0) + c.rank.value

        if (trick.isEmpty()) {
            val strong = legal.filter { it.suit != rules.trumpSuit && it.rank.value >= Rank.KING.value }
            return if (needTricks && strong.isNotEmpty()) strong.maxBy { it.rank.value }
            else legal.minBy { cost(it) }
        }
        val winners = legal.filter { rules.trickWinner(trick + Play(seat, it)) == seat }
        return if (needTricks && winners.isNotEmpty()) winners.minBy { cost(it) }
        else legal.minBy { cost(it) }
    }
}

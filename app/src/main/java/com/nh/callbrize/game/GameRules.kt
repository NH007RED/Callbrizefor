package com.nh.callbrize.game

import com.nh.callbrize.domain.Card
import com.nh.callbrize.domain.Play
import com.nh.callbrize.domain.Suit

/**
 * All rule decisions live behind this interface so the rules can be swapped or tuned
 * without touching the engine, the AI or the UI.
 */
interface GameRules {
    val name: String
    val description: String
    val playerCount: Int
    val cardsPerPlayer: Int
    val roundsPerMatch: Int
    val trumpSuit: Suit?
    val minBid: Int
    val maxBid: Int

    /** Cards from [hand] that may legally be played onto [trick] (plays so far, in order). */
    fun legalCards(hand: List<Card>, trick: List<Play>): List<Card>

    /** Seat that wins a complete (or partial) [trick]. */
    fun trickWinner(trick: List<Play>): Int

    /** Round score for one player. */
    fun scoreRound(bid: Int, tricksWon: Int): Double
}

/**
 * DEFAULT RULES - a configurable Call-Break-style trick-taking game.
 *
 * NOTE: these are NOT claimed to be the "official" Callbrize rules. No verifiable, documented
 * ruleset named "Callbrize" could be established, so this is a clearly documented, widely
 * played South-Asian trick-taking ruleset that can be changed via the constructor parameters
 * or by supplying another [GameRules] implementation.
 *
 * - 4 players, standard 52-card deck, 13 cards each, 5 rounds per match.
 * - Spades are always trump (configurable).
 * - Each player bids how many tricks they expect to win (1..13).
 * - Must follow the lead suit; if possible must beat the highest card of that suit played so far.
 * - If you cannot follow suit you must play a trump (and overtrump if you can); otherwise anything.
 * - Highest trump wins the trick, else highest card of the lead suit.
 * - Score: bid met -> bid + 0.1 per extra trick; bid missed -> minus the bid.
 */
class TrickTakingRules(
    override val playerCount: Int = 4,
    override val roundsPerMatch: Int = 5,
    override val trumpSuit: Suit? = Suit.SPADES,
    override val minBid: Int = 1,
    override val maxBid: Int = 13,
    private val mustBeatHighest: Boolean = true
) : GameRules {

    override val name: String = "Call-Break-style trick taking (default ruleset)"
    override val cardsPerPlayer: Int = 52 / playerCount
    override val description: String =
        "4 players, 13 cards each, 5 rounds. Spades are trump. Bid your tricks, follow suit, " +
            "beat the highest card when you can, and trump when you cannot follow."

    override fun legalCards(hand: List<Card>, trick: List<Play>): List<Card> {
        if (hand.isEmpty()) return emptyList()
        if (trick.isEmpty()) return hand
        val lead = trick.first().card.suit
        val trump = trumpSuit
        val sameSuit = hand.filter { it.suit == lead }
        val trumpInTrick = trump != null && lead != trump && trick.any { it.card.suit == trump }

        if (sameSuit.isNotEmpty()) {
            if (trumpInTrick || !mustBeatHighest) return sameSuit
            val top = trick.filter { it.card.suit == lead }.maxOf { it.card.rank.value }
            val higher = sameSuit.filter { it.rank.value > top }
            return if (higher.isNotEmpty()) higher else sameSuit
        }

        if (trump != null) {
            val trumps = hand.filter { it.suit == trump }
            if (trumps.isNotEmpty()) {
                if (!mustBeatHighest || !trumpInTrick) return trumps
                val top = trick.filter { it.card.suit == trump }.maxOf { it.card.rank.value }
                val higher = trumps.filter { it.rank.value > top }
                return if (higher.isNotEmpty()) higher else hand
            }
        }
        return hand
    }

    override fun trickWinner(trick: List<Play>): Int {
        require(trick.isNotEmpty()) { "Trick is empty" }
        val lead = trick.first().card.suit
        val trump = trumpSuit
        val trumpPlays = if (trump != null) trick.filter { it.card.suit == trump } else emptyList()
        val pool = if (trumpPlays.isNotEmpty()) trumpPlays else trick.filter { it.card.suit == lead }
        return pool.maxBy { it.card.rank.value }.seat
    }

    override fun scoreRound(bid: Int, tricksWon: Int): Double =
        if (tricksWon >= bid) bid + (tricksWon - bid) / 10.0 else -bid.toDouble()
}

object ScoreCalculator {
    fun round1(x: Double): Double = Math.round(x * 10.0) / 10.0

    fun roundScores(rules: GameRules, bids: List<Int>, tricks: List<Int>): List<Double> =
        bids.indices.map { round1(rules.scoreRound(bids[it], tricks[it])) }

    fun addToTotals(totals: List<Double>, scores: List<Double>): List<Double> =
        totals.indices.map { round1(totals[it] + scores[it]) }
}

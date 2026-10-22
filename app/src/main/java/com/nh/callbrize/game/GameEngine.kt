package com.nh.callbrize.game

import com.nh.callbrize.domain.Card
import com.nh.callbrize.domain.Deck
import kotlin.random.Random

/**
 * The single source of truth for a match. Every action is validated here;
 * the UI and the AI can only *request* actions.
 */
class GameEngine(val rules: GameRules, private val random: Random = Random.Default) {

    private var state: GameState? = null

    fun snapshot(): GameState = state ?: error("No match in progress")

    fun hasMatch(): Boolean = state != null

    private val handOrder = compareBy<Card>({ it.suit.ordinal }).thenByDescending { it.rank.value }

    fun newMatch(players: List<PlayerInfo>, rounds: Int = rules.roundsPerMatch, firstDealer: Int = 0): GameState {
        require(players.size == rules.playerCount) { "Need ${rules.playerCount} players" }
        require(rounds >= 1) { "Need at least one round" }
        val s = deal(
            players = players,
            dealer = firstDealer % players.size,
            roundIndex = 0,
            totalRounds = rounds,
            totals = List(players.size) { 0.0 },
            history = emptyList()
        )
        state = s
        return s
    }

    private fun deal(
        players: List<PlayerInfo>, dealer: Int, roundIndex: Int, totalRounds: Int,
        totals: List<Double>, history: List<RoundResult>
    ): GameState {
        val n = rules.playerCount
        val per = rules.cardsPerPlayer
        val deck = Deck.shuffled(random)
        val hands = List(n) { i -> deck.subList(i * per, (i + 1) * per).sortedWith(handOrder) }
        return GameState(
            players = players,
            hands = hands,
            bids = List(n) { null },
            tricksWon = List(n) { 0 },
            currentTrick = emptyList(),
            trickPending = false,
            pendingWinner = null,
            lastTrickWinner = null,
            phase = Phase.BIDDING,
            turn = (dealer + 1) % n,
            dealer = dealer,
            roundIndex = roundIndex,
            totalRounds = totalRounds,
            totals = totals,
            history = history
        )
    }

    fun legalCards(seat: Int): List<Card> {
        val s = state ?: return emptyList()
        if (s.phase != Phase.PLAYING || s.trickPending || seat !in s.hands.indices) return emptyList()
        return rules.legalCards(s.hands[seat], s.currentTrick)
    }

    fun placeBid(seat: Int, bid: Int): ActionResult {
        val s = state ?: return ActionResult.fail("No match in progress")
        if (s.phase != Phase.BIDDING) return ActionResult.fail("Not in the bidding phase")
        if (seat != s.turn) return ActionResult.fail("Not this player's turn to bid")
        if (bid < rules.minBid || bid > rules.maxBid) {
            return ActionResult.fail("Bid must be between ${rules.minBid} and ${rules.maxBid}")
        }
        val bids = s.bids.toMutableList()
        bids[seat] = bid
        val allBid = bids.all { it != null }
        state = if (allBid) {
            s.copy(bids = bids, phase = Phase.PLAYING, turn = (s.dealer + 1) % rules.playerCount)
        } else {
            s.copy(bids = bids, turn = (s.turn + 1) % rules.playerCount)
        }
        return ActionResult.OK
    }

    fun playCard(seat: Int, card: Card): ActionResult {
        val s = state ?: return ActionResult.fail("No match in progress")
        if (s.phase != Phase.PLAYING) return ActionResult.fail("Not in the playing phase")
        if (s.trickPending) return ActionResult.fail("The trick is waiting to be resolved")
        if (seat != s.turn) return ActionResult.fail("Not this player's turn")
        val hand = s.hands[seat]
        if (card !in hand) return ActionResult.fail("That card is not in the hand")
        if (card !in rules.legalCards(hand, s.currentTrick)) {
            return ActionResult.fail("That card is not allowed right now")
        }
        val hands = s.hands.toMutableList()
        hands[seat] = hand - card
        val trick = s.currentTrick + com.nh.callbrize.domain.Play(seat, card)
        val n = rules.playerCount
        state = if (trick.size == n) {
            s.copy(hands = hands, currentTrick = trick, trickPending = true, pendingWinner = rules.trickWinner(trick))
        } else {
            s.copy(hands = hands, currentTrick = trick, turn = (seat + 1) % n)
        }
        return ActionResult.OK
    }

    /** Called after a complete trick has been shown. Awards the trick and may finish the round/match. */
    fun resolveTrick(): ActionResult {
        val s = state ?: return ActionResult.fail("No match in progress")
        if (!s.trickPending) return ActionResult.fail("No completed trick to resolve")
        val winner = rules.trickWinner(s.currentTrick)
        val tricks = s.tricksWon.toMutableList()
        tricks[winner] = tricks[winner] + 1
        val roundFinished = s.hands.all { it.isEmpty() }
        if (!roundFinished) {
            state = s.copy(
                tricksWon = tricks, currentTrick = emptyList(), trickPending = false,
                pendingWinner = null, lastTrickWinner = winner, turn = winner
            )
            return ActionResult.OK
        }
        val bids = s.bids.map { it ?: rules.minBid }
        val scores = ScoreCalculator.roundScores(rules, bids, tricks)
        val totals = ScoreCalculator.addToTotals(s.totals, scores)
        val last = s.roundIndex + 1 >= s.totalRounds
        state = s.copy(
            tricksWon = tricks, currentTrick = emptyList(), trickPending = false,
            pendingWinner = null, lastTrickWinner = winner, turn = winner,
            totals = totals, history = s.history + RoundResult(bids, tricks, scores),
            phase = if (last) Phase.MATCH_OVER else Phase.ROUND_OVER
        )
        return ActionResult.OK
    }

    fun startNextRound(): ActionResult {
        val s = state ?: return ActionResult.fail("No match in progress")
        if (s.phase != Phase.ROUND_OVER) return ActionResult.fail("The round is not finished")
        state = deal(
            players = s.players,
            dealer = (s.dealer + 1) % rules.playerCount,
            roundIndex = s.roundIndex + 1,
            totalRounds = s.totalRounds,
            totals = s.totals,
            history = s.history
        )
        return ActionResult.OK
    }
}

package com.nh.callbrize

import com.nh.callbrize.ai.ComputerPlayer
import com.nh.callbrize.ai.Difficulty
import com.nh.callbrize.domain.Card
import com.nh.callbrize.domain.Deck
import com.nh.callbrize.domain.Play
import com.nh.callbrize.domain.Rank
import com.nh.callbrize.domain.Suit
import com.nh.callbrize.game.GameEngine
import com.nh.callbrize.game.Phase
import com.nh.callbrize.game.PlayerInfo
import com.nh.callbrize.game.TrickTakingRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RulesAndEngineTest {
    private val rules = TrickTakingRules()
    private fun c(s: Suit, r: Rank) = Card(s, r)

    @Test fun deckHas52UniqueCards() {
        val d = Deck.standard()
        assertEquals(52, d.size)
        assertEquals(52, d.toSet().size)
    }

    @Test fun shuffleIsDeterministicForSeed() {
        assertEquals(Deck.shuffled(Random(5)), Deck.shuffled(Random(5)))
        assertFalse(Deck.shuffled(Random(5)) == Deck.standard())
    }

    @Test fun dealGivesThirteenEachWithNoDuplicates() {
        val e = GameEngine(rules, Random(1))
        val s = e.newMatch(List(4) { PlayerInfo("P$it", it, false) })
        assertTrue(s.hands.all { it.size == 13 })
        assertEquals(52, s.hands.flatten().toSet().size)
        assertEquals(Phase.BIDDING, s.phase)
        assertEquals(1, s.turn)
    }

    @Test fun mustFollowSuitAndBeatHighest() {
        val hand = listOf(c(Suit.HEARTS, Rank.TWO), c(Suit.HEARTS, Rank.KING), c(Suit.CLUBS, Rank.ACE))
        val trick = listOf(Play(0, c(Suit.HEARTS, Rank.NINE)))
        assertEquals(listOf(c(Suit.HEARTS, Rank.KING)), rules.legalCards(hand, trick))
    }

    @Test fun followSuitWithLowCardWhenCannotBeat() {
        val hand = listOf(c(Suit.HEARTS, Rank.TWO), c(Suit.HEARTS, Rank.FOUR), c(Suit.CLUBS, Rank.ACE))
        val trick = listOf(Play(0, c(Suit.HEARTS, Rank.QUEEN)))
        assertEquals(setOf(c(Suit.HEARTS, Rank.TWO), c(Suit.HEARTS, Rank.FOUR)), rules.legalCards(hand, trick).toSet())
    }

    @Test fun mustTrumpWhenVoid() {
        val hand = listOf(c(Suit.SPADES, Rank.THREE), c(Suit.CLUBS, Rank.ACE))
        val trick = listOf(Play(0, c(Suit.HEARTS, Rank.NINE)))
        assertEquals(listOf(c(Suit.SPADES, Rank.THREE)), rules.legalCards(hand, trick))
    }

    @Test fun mustOvertrumpIfPossible() {
        val hand = listOf(c(Suit.SPADES, Rank.THREE), c(Suit.SPADES, Rank.TEN), c(Suit.CLUBS, Rank.ACE))
        val trick = listOf(Play(0, c(Suit.HEARTS, Rank.NINE)), Play(1, c(Suit.SPADES, Rank.FIVE)))
        assertEquals(listOf(c(Suit.SPADES, Rank.TEN)), rules.legalCards(hand, trick))
    }

    @Test fun anyCardWhenVoidAndNoTrump() {
        val hand = listOf(c(Suit.CLUBS, Rank.TWO), c(Suit.DIAMONDS, Rank.ACE))
        val trick = listOf(Play(0, c(Suit.HEARTS, Rank.NINE)))
        assertEquals(hand, rules.legalCards(hand, trick))
    }

    @Test fun trickWinners() {
        val t1 = listOf(Play(0, c(Suit.HEARTS, Rank.NINE)), Play(1, c(Suit.HEARTS, Rank.ACE)),
            Play(2, c(Suit.CLUBS, Rank.ACE)), Play(3, c(Suit.HEARTS, Rank.KING)))
        assertEquals(1, rules.trickWinner(t1))
        val t2 = listOf(Play(0, c(Suit.HEARTS, Rank.NINE)), Play(1, c(Suit.HEARTS, Rank.ACE)),
            Play(2, c(Suit.SPADES, Rank.TWO)), Play(3, c(Suit.SPADES, Rank.THREE)))
        assertEquals(3, rules.trickWinner(t2))
    }

    @Test fun scoring() {
        assertEquals(3.0, rules.scoreRound(3, 3), 0.0001)
        assertEquals(3.2, rules.scoreRound(3, 5), 0.0001)
        assertEquals(-3.0, rules.scoreRound(3, 2), 0.0001)
    }

    @Test fun engineRejectsIllegalActions() {
        val e = GameEngine(rules, Random(3))
        val s = e.newMatch(List(4) { PlayerInfo("P$it", it, false) })
        assertFalse(e.placeBid(0, 3).ok)          // wrong seat
        assertFalse(e.placeBid(s.turn, 0).ok)     // out of range
        assertFalse(e.playCard(s.turn, s.hands[s.turn][0]).ok) // not playing yet
        assertTrue(e.placeBid(s.turn, 2).ok)
    }

    @Test fun fullMatchWithBotsOfAllLevelsCompletesLegally() {
        val e = GameEngine(rules, Random(42))
        e.newMatch(List(4) { PlayerInfo("P$it", it, false) }, 3)
        val bots = List(4) { ComputerPlayer(rules, Difficulty.values()[it % 3], Random(it)) }
        var guard = 0
        while (true) {
            guard++
            assertTrue("engine stalled", guard < 5000)
            val s = e.snapshot()
            when {
                s.phase == Phase.MATCH_OVER -> break
                s.phase == Phase.ROUND_OVER -> {
                    assertEquals(13, s.tricksWon.sum())
                    assertTrue(e.startNextRound().ok)
                }
                s.phase == Phase.BIDDING ->
                    assertTrue(e.placeBid(s.turn, bots[s.turn].chooseBid(s.hands[s.turn])).ok)
                s.trickPending -> assertTrue(e.resolveTrick().ok)
                else -> {
                    val card = bots[s.turn].chooseCard(s.turn, s.hands[s.turn], s.currentTrick,
                        s.bids[s.turn] ?: 1, s.tricksWon[s.turn])
                    assertTrue(card in e.legalCards(s.turn))
                    assertTrue(e.playCard(s.turn, card).ok)
                }
            }
        }
        val end = e.snapshot()
        assertEquals(3, end.history.size)
        assertEquals(13, end.history.last().tricks.sum())
    }
}

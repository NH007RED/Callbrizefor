package com.nh.callbrize

import com.nh.callbrize.domain.Card
import com.nh.callbrize.domain.Rank
import com.nh.callbrize.domain.Suit
import com.nh.callbrize.game.GameEngine
import com.nh.callbrize.game.PlayerInfo
import com.nh.callbrize.game.TrickTakingRules
import com.nh.callbrize.net.NetCodec
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class NetCodecTest {
    @Test fun cardKeysRoundTrip() {
        val c = Card(Suit.HEARTS, Rank.QUEEN)
        assertEquals(c, NetCodec.cardOrNull(c.key))
        assertNull(NetCodec.cardOrNull("nonsense"))
        assertNotNull(NetCodec.cardOrNull("SPADES_ACE"))
    }

    @Test fun stateRoundTripHidesOtherHands() {
        val e = GameEngine(TrickTakingRules(), Random(9))
        val s = e.newMatch(List(4) { PlayerInfo("P$it", it, it == 0) })
        val back = NetCodec.decodeState(JSONObject(NetCodec.encodeState(s, 2)))
        assertEquals(s.hands[2], back.hands[2])
        assertEquals(13, back.hands[0].size)
        assertEquals(s.turn, back.turn)
        assertEquals(s.phase, back.phase)
        assertEquals(s.players.map { it.name }, back.players.map { it.name })
    }
}

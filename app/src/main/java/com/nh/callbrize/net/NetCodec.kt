package com.nh.callbrize.net

import com.nh.callbrize.domain.Card
import com.nh.callbrize.domain.Play
import com.nh.callbrize.domain.Rank
import com.nh.callbrize.domain.Suit
import com.nh.callbrize.game.GameState
import com.nh.callbrize.game.Phase
import com.nh.callbrize.game.PlayerInfo
import com.nh.callbrize.game.RoundResult
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON wire format. The host is authoritative: it sends every guest a personalised view
 * of the game where other players' hands are replaced by a card count.
 */
object NetCodec {
    private val PLACEHOLDER = Card(Suit.SPADES, Rank.TWO)

    fun hello(name: String, avatar: Int): String =
        JSONObject().put("t", "hello").put("n", name).put("a", avatar).toString()

    fun bid(v: Int): String = JSONObject().put("t", "bid").put("v", v).toString()

    fun play(card: Card): String = JSONObject().put("t", "play").put("c", card.key).toString()

    fun welcome(seat: Int): String = JSONObject().put("t", "welcome").put("seat", seat).toString()

    fun toast(message: String): String = JSONObject().put("t", "toast").put("m", message).toString()

    fun cardOrNull(key: String): Card? {
        val parts = key.split("_")
        if (parts.size != 2) return null
        return try {
            Card(Suit.valueOf(parts[0]), Rank.valueOf(parts[1]))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun encodeState(s: GameState, forSeat: Int): String {
        val o = JSONObject()
        o.put("t", "state")
        o.put("players", JSONArray().also { a ->
            s.players.forEach { a.put(JSONObject().put("n", it.name).put("a", it.avatar).put("h", it.isHuman)) }
        })
        o.put("hands", JSONArray().also { a ->
            s.hands.forEachIndexed { i, h ->
                a.put(
                    if (i == forSeat) JSONObject().put("cards", JSONArray().also { c -> h.forEach { c.put(it.key) } })
                    else JSONObject().put("count", h.size)
                )
            }
        })
        o.put("bids", intArray(s.bids.map { it ?: -1 }))
        o.put("tricksWon", intArray(s.tricksWon))
        o.put("trick", JSONArray().also { a ->
            s.currentTrick.forEach { a.put(JSONObject().put("s", it.seat).put("c", it.card.key)) }
        })
        o.put("pending", s.trickPending)
        o.put("pendingWinner", s.pendingWinner ?: -1)
        o.put("lastWinner", s.lastTrickWinner ?: -1)
        o.put("phase", s.phase.name)
        o.put("turn", s.turn)
        o.put("dealer", s.dealer)
        o.put("round", s.roundIndex)
        o.put("rounds", s.totalRounds)
        o.put("totals", doubleArray(s.totals))
        o.put("history", JSONArray().also { a ->
            s.history.forEach {
                a.put(JSONObject().put("b", intArray(it.bids)).put("t", intArray(it.tricks)).put("s", doubleArray(it.scores)))
            }
        })
        return o.toString()
    }

    fun decodeState(o: JSONObject): GameState {
        val pa = o.getJSONArray("players")
        val players = (0 until pa.length()).map {
            val p = pa.getJSONObject(it)
            PlayerInfo(p.getString("n"), p.getInt("a"), p.getBoolean("h"))
        }
        val ha = o.getJSONArray("hands")
        val hands = (0 until ha.length()).map {
            val h = ha.getJSONObject(it)
            if (h.has("cards")) {
                val c = h.getJSONArray("cards")
                (0 until c.length()).mapNotNull { i -> cardOrNull(c.getString(i)) }
            } else {
                List(h.getInt("count")) { PLACEHOLDER }
            }
        }
        val ta = o.getJSONArray("trick")
        val trick = (0 until ta.length()).mapNotNull {
            val p = ta.getJSONObject(it)
            cardOrNull(p.getString("c"))?.let { card -> Play(p.getInt("s"), card) }
        }
        val hist = o.getJSONArray("history")
        val history = (0 until hist.length()).map {
            val r = hist.getJSONObject(it)
            RoundResult(ints(r.getJSONArray("b")), ints(r.getJSONArray("t")), doubles(r.getJSONArray("s")))
        }
        val pw = o.getInt("pendingWinner")
        val lw = o.getInt("lastWinner")
        return GameState(
            players = players,
            hands = hands,
            bids = ints(o.getJSONArray("bids")).map { if (it < 0) null else it },
            tricksWon = ints(o.getJSONArray("tricksWon")),
            currentTrick = trick,
            trickPending = o.getBoolean("pending"),
            pendingWinner = if (pw < 0) null else pw,
            lastTrickWinner = if (lw < 0) null else lw,
            phase = Phase.valueOf(o.getString("phase")),
            turn = o.getInt("turn"),
            dealer = o.getInt("dealer"),
            roundIndex = o.getInt("round"),
            totalRounds = o.getInt("rounds"),
            totals = doubles(o.getJSONArray("totals")),
            history = history
        )
    }

    private fun intArray(l: List<Int>): JSONArray = JSONArray().also { a -> l.forEach { a.put(it) } }
    private fun doubleArray(l: List<Double>): JSONArray = JSONArray().also { a -> l.forEach { a.put(it) } }
    private fun ints(a: JSONArray): List<Int> = (0 until a.length()).map { a.getInt(it) }
    private fun doubles(a: JSONArray): List<Double> = (0 until a.length()).map { a.getDouble(it) }
}

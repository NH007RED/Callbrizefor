package com.nh.callbrize.ui

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nh.callbrize.ai.ComputerPlayer
import com.nh.callbrize.ai.Difficulty
import com.nh.callbrize.data.HistoryEntry
import com.nh.callbrize.data.Repository
import com.nh.callbrize.data.Settings
import com.nh.callbrize.data.Stats
import com.nh.callbrize.domain.Card
import com.nh.callbrize.game.GameEngine
import com.nh.callbrize.game.GameRules
import com.nh.callbrize.game.GameState
import com.nh.callbrize.game.Phase
import com.nh.callbrize.game.PlayerInfo
import com.nh.callbrize.game.TrickTakingRules
import com.nh.callbrize.navigation.Navigator
import com.nh.callbrize.navigation.Screen
import com.nh.callbrize.net.BtClient
import com.nh.callbrize.net.BtHost
import com.nh.callbrize.net.HostTransport
import com.nh.callbrize.net.NetCodec
import com.nh.callbrize.net.RawConn
import com.nh.callbrize.net.StreamLink
import com.nh.callbrize.net.TcpClient
import com.nh.callbrize.net.TcpHost
import com.nh.callbrize.net.WIFI_PORT
import com.nh.callbrize.util.Feedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface

enum class GameMode { SINGLE, PASS_AND_PLAY, HOST, CLIENT }
enum class NetKind { WIFI, BLUETOOTH }

private class Guest(var name: String, var avatar: Int)

/**
 * Orchestrates a match. All rule decisions are delegated to [GameEngine].
 * In HOST mode this device runs the engine and sends every guest a personalised view;
 * in CLIENT mode the device only displays the host's state and sends bid/play requests.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)
    val feedback = Feedback(app)
    val rules: GameRules = TrickTakingRules()
    val nav = Navigator()

    val settings: StateFlow<Settings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val stats: StateFlow<Stats> =
        repo.stats.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())
    val history: StateFlow<List<HistoryEntry>> =
        repo.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var mode by mutableStateOf(GameMode.SINGLE)
        private set
    var passNames by mutableStateOf(listOf("Player 2", "Player 3", "Player 4"))
        private set
    var game by mutableStateOf<GameState?>(null)
        private set
    var selected by mutableStateOf<Card?>(null)
        private set
    var paused by mutableStateOf(false)
        private set
    var handoverSeat by mutableStateOf<Int?>(null)
        private set
    var viewerSeat by mutableStateOf(0)
        private set
    var toast by mutableStateOf<String?>(null)
        private set

    // ---- Wi-Fi / Bluetooth state ----
    var netKind by mutableStateOf(NetKind.WIFI)
        private set
    var lobbyPlayers by mutableStateOf<List<String>>(emptyList())
        private set
    var lobbyInfo by mutableStateOf("")
        private set
    var netStatus by mutableStateOf("")
        private set
    var btDevices by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    private var netName = "Player"

    /** Seat of the person holding this device (0 except when joined to someone else's game). */
    val me: Int get() = if (mode == GameMode.CLIENT) mySeat else 0

    private val engine = GameEngine(rules)
    private var bots: Map<Int, ComputerPlayer> = emptyMap()
    private var loopJob: Job? = null
    private var toastJob: Job? = null
    private var matchSaved = false

    private var hostTransport: HostTransport? = null
    private val guests = LinkedHashMap<StreamLink, Guest>()
    private var guestSeats: Map<StreamLink, Int> = emptyMap()
    private var clientLink: StreamLink? = null
    private var mySeat = 0
    private var matchLive = false
    private var matchDifficulty = Difficulty.MEDIUM
    private var awaitingHost = false

    init {
        viewModelScope.launch {
            settings.collect {
                feedback.soundOn = it.sound
                feedback.vibrationOn = it.vibration
            }
        }
    }

    // ---------- settings / profile ----------
    fun selectMode(m: GameMode) { mode = m }
    fun setPassName(index: Int, name: String) {
        passNames = passNames.toMutableList().also { it[index] = name.take(16) }
    }
    fun saveProfile(name: String, avatar: Int) { viewModelScope.launch { repo.saveProfile(name, avatar) } }
    fun setNetProfile(name: String) {
        netName = name.trim().take(16).ifBlank { "Player" }
        saveProfile(netName, settings.value.avatar)
    }
    fun setSound(v: Boolean) { viewModelScope.launch { repo.setSound(v) } }
    fun setVibration(v: Boolean) { viewModelScope.launch { repo.setVibration(v) } }
    fun setAnimations(v: Boolean) { viewModelScope.launch { repo.setAnimations(v) } }
    fun setDifficulty(d: Difficulty) { viewModelScope.launch { repo.setDifficulty(d) } }
    fun clearStats() { viewModelScope.launch { repo.clearStatsAndHistory() } }
    fun dismissNotice() { notice = null }

    // ---------- local match lifecycle (single player / pass-and-play) ----------
    fun startMatch(name: String, avatar: Int, difficulty: Difficulty) {
        stopNet()
        loopJob?.cancel()
        val cleanName = name.trim().take(16).ifBlank { "Player" }
        viewModelScope.launch { repo.saveProfile(cleanName, avatar) }
        val players = if (mode == GameMode.SINGLE) {
            listOf(
                PlayerInfo(cleanName, avatar, true),
                PlayerInfo("Rohan", 1, false),
                PlayerInfo("Priya", 2, false),
                PlayerInfo("Arjun", 3, false)
            )
        } else {
            listOf(PlayerInfo(cleanName, avatar, true)) + passNames.mapIndexed { i, n ->
                PlayerInfo(n.trim().ifBlank { "Player ${i + 2}" }.take(16), (avatar + i + 1) % 8, true)
            }
        }
        bots = if (mode == GameMode.SINGLE) {
            (1..3).associateWith { ComputerPlayer(rules, difficulty) }
        } else emptyMap()
        game = engine.newMatch(players, rules.roundsPerMatch)
        resetLocalState()
        feedback.deal()
        launchLoop()
    }

    private fun resetLocalState() {
        selected = null
        paused = false
        matchSaved = false
        viewerSeat = 0
        handoverSeat = null
        toast = null
    }

    fun nextRound() {
        if (mode == GameMode.CLIENT) return
        if (!engine.startNextRound().ok) return
        publish()
        selected = null
        feedback.deal()
        nav.replace(Screen.Game)
        launchLoop()
    }

    fun rematch() {
        when (mode) {
            GameMode.HOST -> startHostedMatch()
            GameMode.CLIENT -> {}
            else -> {
                val s = settings.value
                startMatch(s.name, s.avatar, s.difficulty)
                nav.replace(Screen.Game)
            }
        }
    }

    fun quitToMenu() {
        loopJob?.cancel()
        stopNet()
        paused = false
        handoverSeat = null
        selected = null
        nav.replaceAll(Screen.Menu)
        game = null
    }

    fun pause() { paused = true }
    fun resume() { paused = false }
    fun onBackground() {
        if (game != null && nav.current == Screen.Game) paused = true
    }

    fun confirmHandover() {
        val seat = handoverSeat ?: return
        viewerSeat = seat
        handoverSeat = null
        selected = null
    }

    // ---------- Wi-Fi / Bluetooth: host ----------
    private fun btAdapter(): BluetoothAdapter? =
        getApplication<Application>().getSystemService(BluetoothManager::class.java)?.adapter

    @SuppressLint("MissingPermission")
    fun startHosting(kind: NetKind) {
        stopNet()
        loopJob?.cancel()
        netKind = kind
        mode = GameMode.HOST
        netStatus = ""
        game = null
        val transport: HostTransport
        if (kind == NetKind.WIFI) {
            transport = TcpHost(WIFI_PORT)
            val ip = localIp()
            lobbyInfo = if (ip != null) "Your address:  $ip" else "No Wi-Fi address found. Connect to Wi-Fi or turn on your hotspot."
        } else {
            val ad = btAdapter()
            if (ad == null || !ad.isEnabled) {
                lobbyInfo = ""
                netStatus = "Turn Bluetooth on, then go back and try again."
                return
            }
            transport = BtHost(ad)
            val devName = try { ad.name ?: "this phone" } catch (e: SecurityException) { "this phone" }
            lobbyInfo = "Bluetooth name:  $devName"
        }
        hostTransport = transport
        transport.start(
            onConn = { raw -> viewModelScope.launch { onGuestConnected(raw) } },
            onError = { msg -> viewModelScope.launch { netStatus = msg } }
        )
    }

    private fun onGuestConnected(raw: RawConn) {
        if (matchLive || guests.size >= 3 || mode != GameMode.HOST) {
            try { raw.closer() } catch (e: Exception) { }
            return
        }
        val link = StreamLink(
            raw,
            onLine = { l, line -> viewModelScope.launch { onHostLine(l, line) } },
            onClosed = { l -> viewModelScope.launch { onGuestLeft(l) } }
        )
        guests[link] = Guest("Player ${guests.size + 2}", 0)
        refreshLobby()
        link.start()
    }

    private fun refreshLobby() { lobbyPlayers = guests.values.map { it.name } }

    private fun onHostLine(link: StreamLink, line: String) {
        val o = try { JSONObject(line) } catch (e: Exception) { return }
        when (o.optString("t")) {
            "hello" -> {
                guests[link]?.let {
                    it.name = o.optString("n", "Player").trim().take(16).ifBlank { "Player" }
                    it.avatar = o.optInt("a", 0).coerceIn(0, 7)
                }
                refreshLobby()
            }
            "bid" -> remoteBid(link, o.optInt("v", -1))
            "play" -> remotePlay(link, o.optString("c"))
        }
    }

    private fun remoteBid(link: StreamLink, v: Int) {
        val seat = guestSeats[link] ?: return
        val r = engine.placeBid(seat, v)
        if (r.ok) {
            publish()
            launchLoop()
        } else link.send(NetCodec.toast(r.error ?: "Invalid bid"))
    }

    private fun remotePlay(link: StreamLink, key: String) {
        val seat = guestSeats[link] ?: return
        val card = NetCodec.cardOrNull(key) ?: return
        val r = engine.playCard(seat, card)
        if (r.ok) {
            feedback.cardPlayed()
            publish()
            launchLoop()
        } else link.send(NetCodec.toast(r.error ?: "Invalid move"))
    }

    private fun onGuestLeft(link: StreamLink) {
        val seat = guestSeats[link]
        val g = guests.remove(link)
        if (g == null && seat == null) return
        guestSeats = guestSeats - link
        refreshLobby()
        if (matchLive && seat != null) {
            bots = bots + (seat to ComputerPlayer(rules, matchDifficulty))
            showToast("${g?.name ?: "A player"} left. A computer took over.")
            val ph = game?.phase
            if (ph == Phase.BIDDING || ph == Phase.PLAYING) launchLoop()
        }
    }

    fun startHostedMatch() {
        if (mode != GameMode.HOST) return
        loopJob?.cancel()
        val s = settings.value
        val list = guests.entries.toList().take(3)
        val infos = mutableListOf(PlayerInfo(netName.trim().ifBlank { "Player" }.take(16), s.avatar, true))
        val seats = LinkedHashMap<StreamLink, Int>()
        list.forEachIndexed { i, e ->
            infos.add(PlayerInfo(e.value.name, e.value.avatar, true))
            seats[e.key] = i + 1
        }
        val botNames = listOf("Rohan", "Priya", "Arjun")
        val botMap = HashMap<Int, ComputerPlayer>()
        matchDifficulty = s.difficulty
        var k = 0
        while (infos.size < rules.playerCount) {
            val seat = infos.size
            val nm = botNames[k]
            k++
            infos.add(PlayerInfo(nm, k, false))
            botMap[seat] = ComputerPlayer(rules, matchDifficulty)
        }
        bots = botMap
        guestSeats = seats
        matchLive = true
        game = engine.newMatch(infos, rules.roundsPerMatch)
        resetLocalState()
        for ((link, seat) in seats) link.send(NetCodec.welcome(seat))
        broadcast(engine.snapshot())
        feedback.deal()
        if (nav.current != Screen.Game) nav.replace(Screen.Game)
        launchLoop()
    }

    private fun broadcast(s: GameState) {
        for ((link, seat) in guestSeats) link.send(NetCodec.encodeState(s, seat))
    }

    private fun publish() {
        val s = engine.snapshot()
        game = s
        if (mode == GameMode.HOST && matchLive) broadcast(s)
    }

    // ---------- Wi-Fi / Bluetooth: client ----------
    fun prepareJoin(kind: NetKind) {
        stopNet()
        loopJob?.cancel()
        netKind = kind
        mode = GameMode.CLIENT
        netStatus = ""
        game = null
        if (kind == NetKind.BLUETOOTH) refreshBt()
    }

    @SuppressLint("MissingPermission")
    fun refreshBt() {
        val ad = btAdapter()
        if (ad == null) {
            netStatus = "This phone has no Bluetooth."
            btDevices = emptyList()
            return
        }
        if (!ad.isEnabled) {
            netStatus = "Turn Bluetooth on, then tap Refresh."
            btDevices = emptyList()
            return
        }
        try {
            btDevices = ad.bondedDevices.map { (it.name ?: it.address) to it.address }.sortedBy { it.first }
            netStatus = if (btDevices.isEmpty()) {
                "No paired devices. Pair the phones in Android Settings > Bluetooth first."
            } else "Tap the host's phone to connect."
        } catch (e: SecurityException) {
            netStatus = "Bluetooth permission was denied."
        }
    }

    fun joinWifi(ip: String) {
        if (ip.isBlank()) return
        netStatus = "Connecting…"
        TcpClient(ip.trim(), WIFI_PORT).connect(
            onConn = { raw -> viewModelScope.launch { onConnected(raw) } },
            onError = { msg -> viewModelScope.launch { netStatus = msg } }
        )
    }

    fun joinBluetooth(address: String) {
        val ad = btAdapter()
        if (ad == null || !ad.isEnabled) {
            netStatus = "Turn Bluetooth on first."
            return
        }
        netStatus = "Connecting…"
        BtClient(ad, address).connect(
            onConn = { raw -> viewModelScope.launch { onConnected(raw) } },
            onError = { msg -> viewModelScope.launch { netStatus = msg } }
        )
    }

    private fun onConnected(raw: RawConn) {
        val old = clientLink
        clientLink = null
        old?.close()
        val link = StreamLink(
            raw,
            onLine = { _, line -> viewModelScope.launch { onClientLine(line) } },
            onClosed = { l -> viewModelScope.launch { onHostLost(l) } }
        )
        clientLink = link
        link.start()
        link.send(NetCodec.hello(netName, settings.value.avatar))
        netStatus = "Connected! Waiting for the host to start…"
    }

    private fun onHostLost(link: StreamLink) {
        if (link !== clientLink) return
        clientLink = null
        if (mode != GameMode.CLIENT) return
        if (game == null) {
            netStatus = "Disconnected from the host."
        } else {
            quitToMenu()
            notice = "The connection to the host was lost."
        }
    }

    private fun onClientLine(line: String) {
        val o = try { JSONObject(line) } catch (e: Exception) { return }
        when (o.optString("t")) {
            "welcome" -> {
                mySeat = o.optInt("seat", 0)
                viewerSeat = mySeat
            }
            "state" -> {
                val st = try { NetCodec.decodeState(o) } catch (e: Exception) { return }
                onRemoteState(st)
            }
            "toast" -> showToast(o.optString("m"))
        }
    }

    private fun onRemoteState(s: GameState) {
        val prev = game
        awaitingHost = false
        if (prev != null) {
            if (s.currentTrick.size > prev.currentTrick.size) feedback.cardPlayed()
            else if (prev.trickPending && !s.trickPending) feedback.trickDone()
        }
        if (prev == null || prev.roundIndex != s.roundIndex) {
            feedback.deal()
            selected = null
            matchSaved = false
        }
        game = s
        viewerSeat = mySeat
        val sel = selected
        if (sel != null && mySeat < s.hands.size && sel !in s.hands[mySeat]) selected = null
        val cur = nav.current
        if (s.phase == Phase.BIDDING || s.phase == Phase.PLAYING) {
            if (cur == Screen.JoinGame || cur == Screen.RoundResults || cur == Screen.MatchResults) {
                nav.replace(Screen.Game)
            }
        } else {
            viewModelScope.launch {
                delay(700)
                val g = game ?: return@launch
                if (g.phase != s.phase || nav.current != Screen.Game) return@launch
                if (s.phase == Phase.MATCH_OVER) {
                    finishMatch(g)
                    nav.replace(Screen.MatchResults)
                } else nav.replace(Screen.RoundResults)
            }
        }
    }

    fun leaveLobby() {
        stopNet()
        nav.back()
    }

    private fun stopNet() {
        matchLive = false
        val t = hostTransport
        hostTransport = null
        t?.stop()
        val gl = guests.keys.toList()
        guests.clear()
        guestSeats = emptyMap()
        lobbyPlayers = emptyList()
        val cl = clientLink
        clientLink = null
        gl.forEach { it.close() }
        cl?.close()
    }

    private fun localIp(): String? = try {
        val found = ArrayList<Pair<String, String>>()
        for (nif in NetworkInterface.getNetworkInterfaces().toList()) {
            if (!nif.isUp || nif.isLoopback) continue
            for (addr in nif.inetAddresses.toList()) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) found.add(nif.name to (addr.hostAddress ?: ""))
            }
        }
        val best = found.firstOrNull { it.first.startsWith("wlan") || it.first.startsWith("ap") || it.first.startsWith("swlan") }
            ?: found.firstOrNull()
        best?.second?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    // ---------- human actions ----------
    fun legalNow(): List<Card> {
        val s = game ?: return emptyList()
        if (s.phase != Phase.PLAYING || s.trickPending) return emptyList()
        if (s.turn >= s.hands.size) return emptyList()
        return if (mode == GameMode.CLIENT) rules.legalCards(s.hands[s.turn], s.currentTrick)
        else engine.legalCards(s.turn)
    }

    fun humanBid(bid: Int) {
        val s = game ?: return
        if (paused || handoverSeat != null) return
        if (s.phase != Phase.BIDDING || s.turn != viewerSeat) return
        if (mode == GameMode.CLIENT) {
            if (awaitingHost) return
            awaitingHost = true
            feedback.select()
            clientLink?.send(NetCodec.bid(bid))
            return
        }
        if (!s.players[s.turn].isHuman) return
        val r = engine.placeBid(s.turn, bid)
        if (r.ok) {
            feedback.select()
            publish()
            launchLoop()
        } else showToast(r.error ?: "Invalid bid")
    }

    fun onCardTapped(card: Card) {
        val s = game ?: return
        if (paused || handoverSeat != null) return
        if (s.phase != Phase.PLAYING || s.trickPending || s.turn != viewerSeat) return
        if (card !in legalNow()) {
            showToast("Not allowed now: follow suit, then trump, and beat the highest card if you can")
            return
        }
        selected = card
        feedback.select()
    }

    /** Called when the player swipes a card upwards. The engine (host) still validates the move. */
    fun playCard(card: Card) {
        val s = game ?: return
        if (paused || handoverSeat != null) return
        if (s.phase != Phase.PLAYING || s.trickPending || s.turn != viewerSeat) return
        if (mode == GameMode.CLIENT) {
            if (awaitingHost) return
            if (card in legalNow()) {
                awaitingHost = true
                selected = null
                feedback.cardPlayed()
                clientLink?.send(NetCodec.play(card))
            } else showNotAllowed()
            return
        }
        val r = engine.playCard(s.turn, card)
        if (r.ok) {
            selected = null
            feedback.cardPlayed()
            publish()
            launchLoop()
        } else showNotAllowed()
    }

    private fun showNotAllowed() =
        showToast("Not allowed now: follow suit, then trump, and beat the highest card if you can")

    private fun showToast(msg: String) {
        toast = msg
        toastJob?.cancel()
        toastJob = viewModelScope.launch { delay(2200); toast = null }
    }

    // ---------- game loop (computer turns, trick resolution, handovers) ----------
    private fun blocked(): Boolean = paused && mode != GameMode.HOST

    private fun launchLoop() {
        if (mode == GameMode.CLIENT) return
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (isActive) {
                while (blocked()) delay(100)
                val s = game ?: return@launch
                if (s.phase == Phase.ROUND_OVER || s.phase == Phase.MATCH_OVER) {
                    roundEnded(s)
                    return@launch
                }
                if (s.trickPending) {
                    delay(if (settings.value.animations) 1300L else 700L)
                    if (blocked()) continue
                    engine.resolveTrick()
                    feedback.trickDone()
                    publish()
                    continue
                }
                if (bots.containsKey(s.turn)) {
                    delay(if (settings.value.animations) 800L else 400L)
                    if (blocked()) continue
                    botAct(s)
                } else {
                    if (mode == GameMode.PASS_AND_PLAY && s.turn != viewerSeat && handoverSeat == null) {
                        handoverSeat = s.turn
                    }
                    return@launch
                }
            }
        }
    }

    private fun botAct(s: GameState) {
        val seat = s.turn
        val bot = bots[seat] ?: return
        if (s.phase == Phase.BIDDING) {
            val bid = bot.chooseBid(s.hands[seat])
            if (!engine.placeBid(seat, bid).ok) engine.placeBid(seat, rules.minBid)
        } else {
            val card = bot.chooseCard(seat, s.hands[seat], s.currentTrick, s.bids[seat] ?: 1, s.tricksWon[seat])
            if (engine.playCard(seat, card).ok) {
                feedback.cardPlayed()
            } else {
                // Safety net: should never happen, but never stall the game.
                engine.legalCards(seat).firstOrNull()?.let { engine.playCard(seat, it) }
            }
        }
        publish()
    }

    private suspend fun roundEnded(s: GameState) {
        delay(700)
        if (nav.current != Screen.Game) return
        if (s.phase == Phase.MATCH_OVER) {
            finishMatch(s)
            nav.replace(Screen.MatchResults)
        } else {
            nav.replace(Screen.RoundResults)
        }
    }

    private fun finishMatch(s: GameState) {
        if (matchSaved) return
        matchSaved = true
        val seat = me
        val max = s.totals.max()
        val winners = s.totals.indices.filter { s.totals[it] == max }
        val single = mode == GameMode.SINGLE
        val won = seat in winners
        feedback.result(if (mode == GameMode.PASS_AND_PLAY) true else won)
        val rank = 1 + s.totals.count { it > s.totals[seat] }
        val label = when (mode) {
            GameMode.SINGLE -> "Single Player"
            GameMode.PASS_AND_PLAY -> "Pass-and-Play"
            else -> if (netKind == NetKind.WIFI) "Wi-Fi Multiplayer" else "Bluetooth Multiplayer"
        }
        val entry = HistoryEntry(
            time = System.currentTimeMillis(),
            mode = label,
            difficulty = if (single) settings.value.difficulty.label else "-",
            winner = s.players[winners.first()].name,
            humanScore = s.totals[seat],
            rank = rank,
            players = s.players.joinToString(", ") { it.name }
        )
        viewModelScope.launch { repo.recordMatch(entry, single, single && won) }
    }

    override fun onCleared() {
        stopNet()
        feedback.release()
        super.onCleared()
    }
}

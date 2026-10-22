package com.nh.callbrize.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nh.callbrize.ai.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "callbrize")

/** All persistence is local (DataStore). No network, no accounts. */
class Repository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object K {
        val NAME = stringPreferencesKey("name")
        val AVATAR = intPreferencesKey("avatar")
        val SOUND = booleanPreferencesKey("sound")
        val VIBRATION = booleanPreferencesKey("vibration")
        val ANIM = booleanPreferencesKey("animations")
        val DIFF = stringPreferencesKey("difficulty")
        val PLAYED = intPreferencesKey("played")
        val WINS = intPreferencesKey("wins")
        val LOSSES = intPreferencesKey("losses")
        val BEST = doublePreferencesKey("best")
        val HISTORY = stringPreferencesKey("history")
    }

    private val prefs = store.data.catch { emit(emptyPreferences()) }

    val settings: Flow<Settings> = prefs.map { p ->
        Settings(
            name = p[K.NAME] ?: "Player",
            avatar = p[K.AVATAR] ?: 0,
            sound = p[K.SOUND] ?: true,
            vibration = p[K.VIBRATION] ?: true,
            animations = p[K.ANIM] ?: true,
            difficulty = Difficulty.values().firstOrNull { it.name == p[K.DIFF] } ?: Difficulty.MEDIUM
        )
    }

    val stats: Flow<Stats> = prefs.map { p ->
        Stats(p[K.PLAYED] ?: 0, p[K.WINS] ?: 0, p[K.LOSSES] ?: 0, p[K.BEST])
    }

    val history: Flow<List<HistoryEntry>> = prefs.map { p -> parseHistory(p[K.HISTORY]) }

    suspend fun saveProfile(name: String, avatar: Int) {
        val clean = name.trim().take(16).ifBlank { "Player" }
        store.edit { it[K.NAME] = clean; it[K.AVATAR] = avatar }
    }

    suspend fun setSound(v: Boolean) { store.edit { it[K.SOUND] = v } }
    suspend fun setVibration(v: Boolean) { store.edit { it[K.VIBRATION] = v } }
    suspend fun setAnimations(v: Boolean) { store.edit { it[K.ANIM] = v } }
    suspend fun setDifficulty(d: Difficulty) { store.edit { it[K.DIFF] = d.name } }

    suspend fun recordMatch(entry: HistoryEntry, countStats: Boolean, won: Boolean) {
        store.edit { p ->
            if (countStats) {
                p[K.PLAYED] = (p[K.PLAYED] ?: 0) + 1
                if (won) p[K.WINS] = (p[K.WINS] ?: 0) + 1 else p[K.LOSSES] = (p[K.LOSSES] ?: 0) + 1
                val best = p[K.BEST]
                if (best == null || entry.humanScore > best) p[K.BEST] = entry.humanScore
            }
            val list = (listOf(entry) + parseHistory(p[K.HISTORY])).take(30)
            p[K.HISTORY] = toJson(list)
        }
    }

    suspend fun clearStatsAndHistory() {
        store.edit {
            it.remove(K.PLAYED); it.remove(K.WINS); it.remove(K.LOSSES)
            it.remove(K.BEST); it.remove(K.HISTORY)
        }
    }

    private fun toJson(list: List<HistoryEntry>): String {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("t", e.time); put("m", e.mode); put("d", e.difficulty); put("w", e.winner)
                put("s", e.humanScore); put("r", e.rank); put("p", e.players)
            })
        }
        return arr.toString()
    }

    private fun parseHistory(json: String?): List<HistoryEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                HistoryEntry(
                    o.getLong("t"), o.getString("m"), o.getString("d"), o.getString("w"),
                    o.getDouble("s"), o.getInt("r"), o.getString("p")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

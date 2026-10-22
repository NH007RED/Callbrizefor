package com.nh.callbrize.data

import com.nh.callbrize.ai.Difficulty

data class Settings(
    val name: String = "Player",
    val avatar: Int = 0,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val animations: Boolean = true,
    val difficulty: Difficulty = Difficulty.MEDIUM
)

data class Stats(
    val played: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val bestScore: Double? = null
)

data class HistoryEntry(
    val time: Long,
    val mode: String,
    val difficulty: String,
    val winner: String,
    val humanScore: Double,
    val rank: Int,
    val players: String
)

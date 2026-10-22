package com.nh.callbrize.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.nh.callbrize.R
import java.util.Locale

enum class Sfx(val res: Int) {
    DEAL(R.raw.snd_deal), SELECT(R.raw.snd_select), PLAY(R.raw.snd_play),
    TRICK(R.raw.snd_trick), WIN(R.raw.snd_win), LOSE(R.raw.snd_lose)
}

/** Locally bundled sound effects and haptics, both individually switchable. */
class Feedback(context: Context) {
    var soundOn: Boolean = true
    var vibrationOn: Boolean = true

    private val app = context.applicationContext
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val ids = HashMap<Sfx, Int>()

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    init {
        Sfx.values().forEach { ids[it] = pool.load(app, it.res, 1) }
    }

    private fun play(sfx: Sfx) {
        if (!soundOn) return
        val id = ids[sfx] ?: return
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    private fun buzz(ms: Long) {
        if (!vibrationOn) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun deal() = play(Sfx.DEAL)
    fun select() { play(Sfx.SELECT); buzz(12) }
    fun cardPlayed() { play(Sfx.PLAY); buzz(20) }
    fun trickDone() { play(Sfx.TRICK); buzz(35) }
    fun result(win: Boolean) { play(if (win) Sfx.WIN else Sfx.LOSE); buzz(80) }

    fun release() { pool.release() }
}

fun Double.fmt(): String = String.format(Locale.US, "%.1f", this)

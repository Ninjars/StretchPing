package jez.stretchping.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_GAME
import android.media.SoundPool
import jez.stretchping.R
import timber.log.Timber

enum class GameSoundEffect(val resourceId: Int) {
    Stop(R.raw.puzzle_success_xylophone_1_two_note_fast_wet_stereo),
    ActiveSection(R.raw.puzzle_success_xylophone_2_two_note_decline_bright_wet_stereo),
    TransitionSection(R.raw.puzzle_success_xylophone_2_two_note_climb_bright_wet_stereo),
    CountdownBeep(R.raw.puzzle_success_xylophone_2_one_note_wet_stereo),
    Completed(R.raw.special_interface_8),
}

class GameSoundEffectPlayer {
    private var soundPool: SoundPool? = null
    private var soundIds: List<Int> = emptyList()

    fun initialise(context: Context): SoundPool {
        val newSoundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(USAGE_GAME).build())
            .build()

        soundIds = GameSoundEffect.entries.map { effect ->
            newSoundPool.load(context, effect.resourceId, 1)
        }
        soundPool = newSoundPool
        return newSoundPool
    }

    fun tearDown() {
        soundPool?.release()
        soundPool = null
    }

    fun play(effect: GameSoundEffect, volume: Float = 1f) {
        with(soundPool) {
            if (this == null) {
                Timber.e("soundPool as been released or not initialised")
                return
            }
            val soundId = soundIds[effect.ordinal]
            play(
                soundId,
                volume,
                volume,
                1,
                0,
                1f,
            )
        }
    }
}

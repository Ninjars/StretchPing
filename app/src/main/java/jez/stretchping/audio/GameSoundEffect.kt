package jez.stretchping.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_GAME
import android.media.SoundPool
import jez.stretchping.R
import jez.stretchping.audio.GameSoundEffect.values

enum class GameSoundEffect(val resourceId: Int) {
    Stop(R.raw.puzzle_success_xylophone_1_two_note_fast_wet_stereo),
    ActiveSection(R.raw.puzzle_success_xylophone_2_two_note_decline_bright_wet_stereo),
    TransitionSection(R.raw.puzzle_success_xylophone_2_two_note_climb_bright_wet_stereo),
    CountdownBeep(R.raw.puzzle_success_xylophone_2_one_note_wet_stereo),
}

class GameSoundEffectPlayer {
    private lateinit var soundPool: SoundPool
    private var soundIds: List<Int> = emptyList()

    fun initialise(context: Context) {
        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(USAGE_GAME).build())
            .build()

        soundIds = values().map { effect ->
            soundPool.load(context, effect.resourceId, 1)
        }
    }

    fun tearDown() {
        soundPool.release()
    }

    fun play(effect: GameSoundEffect, volume: Float = 1f) {
        val soundId = soundIds[effect.ordinal]
        soundPool.play(
            soundId,
            volume,
            volume,
            1,
            0,
            1f,
        )
    }
}
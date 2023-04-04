package jez.stretchping.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_GAME
import android.media.SoundPool
import jez.stretchping.R
import jez.stretchping.audio.GameSoundEffect.ActiveSection
import jez.stretchping.audio.GameSoundEffect.Back
import jez.stretchping.audio.GameSoundEffect.CountdownBeep
import jez.stretchping.audio.GameSoundEffect.Stop
import jez.stretchping.audio.GameSoundEffect.TransitionSection
import jez.stretchping.audio.GameSoundEffect.values

enum class GameSoundEffect {
    Back,
    Stop,
    ActiveSection,
    TransitionSection,
    CountdownBeep,
}

class GameSoundEffectPlayer {
    private lateinit var soundPool: SoundPool
    private var soundIds: List<Int> = emptyList()

    fun initialise(context: Context) {
        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(USAGE_GAME).build())
            .build()

        soundIds = values()
            .map {
                when (it) {
                    Back, Stop -> R.raw.puzzle_success_xylophone_2_two_note_decline_low_wet_stereo
                    ActiveSection -> R.raw.puzzle_success_xylophone_2_stab_wet_stereo
                    TransitionSection -> R.raw.puzzle_success_xylophone_2_two_note_climb_bright_wet_stereo
                    CountdownBeep -> R.raw.puzzle_success_xylophone_2_one_note_wet_stereo
                }
            }.map {
                soundPool.load(context, it, 1)
            }
    }

    fun tearDown() {
        soundPool.release()
    }

    fun play(effect: GameSoundEffect) {
        val soundId = soundIds[effect.ordinal]
        soundPool.play(
            soundId,
            1f,
            1f,
            1,
            0,
            1f,
        )
    }
}
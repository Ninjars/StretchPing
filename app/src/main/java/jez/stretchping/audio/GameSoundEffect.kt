package jez.stretchping.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_GAME
import android.media.SoundPool

enum class GameSoundEffect {
    Back,
    StartActiveSection,
    StartTransitionSection,
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

//        soundIds = GameSoundEffect.values()
//            .map {
//                when (it) {
//                    GameSoundEffect.Back -> TODO()
//                    GameSoundEffect.StartActiveSection -> TODO()
//                    GameSoundEffect.StartTransitionSection -> TODO()
//                    GameSoundEffect.CountdownBeep -> TODO()
//                }
//            }.map {
//                soundPool.load(context, it, 1)
//            }
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
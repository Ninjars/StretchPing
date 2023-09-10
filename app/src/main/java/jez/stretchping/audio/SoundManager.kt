package jez.stretchping.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SoundManager @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val soundEffectPlayer = GameSoundEffectPlayer().apply { initialise(context) }

    /*
     Needed because android audio handling is apparently trash
     The "warmup latency" on first playing a sound can be huge and arbitrary,
     so we can reduce the UX impact of that by playing a silent sound occasionally.

     This is kinda ridiculous to still be necessary in 2023 tbh, but it looks like "lot latency"
     C++ libs like Oboe get around this by constantly streaming silence when not playing sound,
     so it could be that there's a significant physical hardware aspect creating some of the issue here.
     */
    fun playSilence() {
        soundEffectPlayer.play(GameSoundEffect.CountdownBeep, 0f)
    }

    fun playSound(effect: GameSoundEffect) {
        soundEffectPlayer.play(effect)
    }

    fun dispose() {
        soundEffectPlayer.tearDown()
    }
}

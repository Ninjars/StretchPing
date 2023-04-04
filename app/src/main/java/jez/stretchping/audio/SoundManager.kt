package jez.stretchping.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SoundManager @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val soundEffectPlayer = GameSoundEffectPlayer().apply { initialise(context) }

    fun playSound(effect: GameSoundEffect) {
        soundEffectPlayer.play(effect)
    }

    fun dispose() {
        soundEffectPlayer.tearDown()
    }
}

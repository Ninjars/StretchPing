package jez.stretchping.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private lateinit var ttsEngine: TextToSpeech

    fun initialise() {
        ttsEngine = TextToSpeech(
            context
        ) {
            if (it == TextToSpeech.SUCCESS) {
                Timber.d("TTS engine initialised")
            } else {
                Timber.d("TTS failed to launch, status code: $it")
            }
        }
    }

    fun destroy() {
        ttsEngine.stop()
        ttsEngine.shutdown()
    }

    fun getVoiceOptions() = ttsEngine.voices
    fun activeVoice() = ttsEngine.voice

    fun setVoice(voice: Voice) {
        ttsEngine.voice = voice
    }

    fun announce(message: String) {
        ttsEngine.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}

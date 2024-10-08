package jez.stretchping

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.redfoxstudio.stretchping.BuildConfig
import timber.log.Timber

@HiltAndroidApp
class HiltJetpackApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

package jez.stretchping.features.activetimer.logic

import android.os.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SchedulingModule {

    /**
     * `elapsedRealtime()` is monotonic and keeps counting while the device is
     * asleep, so anchoring delays to it prevents drift/stalls across doze.
     */
    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = TimeProvider { SystemClock.elapsedRealtime() }
}

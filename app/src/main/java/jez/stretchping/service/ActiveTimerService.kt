package jez.stretchping.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine
import jez.stretchping.features.activetimer.logic.RunningStateController
import jez.stretchping.notification.NotificationsHelper
import timber.log.Timber

class ActiveTimerService :
    Service(),
    RunningStateController {

    inner class LocalBinder : Binder() {
        fun getService(): ActiveTimerService = this@ActiveTimerService
    }

    private val binder = LocalBinder()
    private var engine: ActiveTimerEngine? = null

    // Held only while the timer is actively counting down so scheduled pings/TTS
    // keep firing with the screen off; released on pause/complete/destroy. The
    // foreground-service notification keeps the process alive, but a partial wake
    // lock is what keeps the CPU running the delay() coroutines through doze.
    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StretchPing:ActiveTimer")
            .apply { setReferenceCounted(false) }
    }

    override fun onBind(intent: Intent?): IBinder = binder.also { Timber.e("onBind") }

    override fun onCreate() {
        super.onCreate()
        Timber.e("onCreate")
        // create the notification channel
        NotificationsHelper.createNotificationChannel(this)

        // promote service to foreground service
        ServiceCompat.startForeground(
            this,
            1,
            NotificationsHelper.buildNotification(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.e("onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Timber.e("onDestroy")
        engine?.dispose()
        engine = null
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onRunningStateChanged(isRunning: Boolean) {
        if (isRunning) acquireWakeLock() else releaseWakeLock()
    }

    @Suppress("WakelockTimeout")
    private fun acquireWakeLock() {
        if (!wakeLock.isHeld) {
            Timber.e("acquireWakeLock")
            // No timeout: a running segment can legitimately be minutes long, and
            // the lock is always released on pause/complete/destroy.
            wakeLock.acquire()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            Timber.e("releaseWakeLock")
            wakeLock.release()
        }
    }

    fun stopForegroundService() {
        Timber.e("stopForegroundService")
        stopSelf()
    }

    fun getOrCreateEngine(
        factory: (onEndCallback: () -> Unit, runningStateController: RunningStateController) -> ActiveTimerEngine,
    ): ActiveTimerEngine = engine.let { existing ->
        Timber.e("getOrCreateEngine: already exists? ${existing != null}")
        existing ?: factory(
            {
                stopForegroundService()
                engine?.dispose()
                engine = null
            },
            this,
        ).also { engine = it }
    }
}

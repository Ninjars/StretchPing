package jez.stretchping.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine
import jez.stretchping.notification.NotificationsHelper
import timber.log.Timber

class ActiveTimerService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): ActiveTimerService = this@ActiveTimerService
    }

    private val binder = LocalBinder()
    private var engine: ActiveTimerEngine? = null

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
            }
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
        super.onDestroy()
    }

    fun stopForegroundService() {
        Timber.e("stopForegroundService")
        stopSelf()
    }

    fun getOrCreateEngine(factory: (() -> Unit) -> ActiveTimerEngine): ActiveTimerEngine =
        engine.let { existing ->
            Timber.e("getOrCreateEngine: already exists? ${existing != null}")
            existing ?: factory {
                stopForegroundService()
                engine?.dispose()
                engine = null
            }.also { engine = it }
        }
}

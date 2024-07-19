package jez.stretchping.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import jez.stretchping.notification.NotificationsHelper
import timber.log.Timber

class ActiveTimerService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): ActiveTimerService = this@ActiveTimerService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder.also{
        Timber.e("onBind")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.e("onStartCommand")
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

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Timber.e("onDestroy")
        // TODO: cleanup any running jobs
        super.onDestroy()
    }

    fun stopForegroundService() {
        stopSelf()
    }
}

package jez.stretchping.service

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import timber.log.Timber


class ActiveTimerServiceController(private val activity: Activity) {
    private var onConnectedCallback: ((ActiveTimerService) -> Unit)? = null
    private var activeTimerService: ActiveTimerService? = null

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Timber.e("onServiceConnected")
            val binder = service as ActiveTimerService.LocalBinder
            val boundService = binder.getService()
            activeTimerService = boundService
            onConnectedCallback?.invoke(boundService)
            onConnectedCallback = null
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.e("onServiceConnected")
            activeTimerService = null
        }
    }

    fun bind(callback: (ActiveTimerService) -> Unit) {
        Timber.e("bind: already bound? ${isBound()}")
        val existingService = activeTimerService
        if (existingService == null) {
            onConnectedCallback = callback
            with(activity) {
                bindService(
                    Intent(this, ActiveTimerService::class.java),
                    connection,
                    Context.BIND_AUTO_CREATE
                )
            }
        } else {
            callback(existingService)
        }
    }

    fun unbind() {
        Timber.e("unbind: already bound? ${isBound()}")
        if (isBound()) {
            activity.unbindService(connection)
        }
        activeTimerService = null
        onConnectedCallback = null
    }

    private fun isBound(): Boolean = activeTimerService != null
    fun startService() {
        Timber.e("startService")
        activity.startService(Intent(activity, ActiveTimerService::class.java))
    }

    fun stopService() {
        val success = activity.stopService(Intent(activity, ActiveTimerService::class.java))
        Timber.e("stopService: succeeded? $success")
    }
}

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

    /**
     * True once [bindService] has been called and not yet unbound. Tracked
     * separately from [activeTimerService] because a bind can be in-flight
     * (requested but [onServiceConnected] not yet fired); unbinding must still
     * call [Activity.unbindService] in that window or the ServiceConnection
     * leaks and the next bind double-binds.
     */
    private var bindRequested = false

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
        if (existingService != null) {
            callback(existingService)
        } else {
            onConnectedCallback = callback
            if (!bindRequested) {
                bindRequested = true
                with(activity) {
                    bindService(
                        Intent(this, ActiveTimerService::class.java),
                        connection,
                        Context.BIND_AUTO_CREATE,
                    )
                }
            }
        }
    }

    fun unbind() {
        Timber.e("unbind: already bound? ${isBound()}, requested? $bindRequested")
        if (bindRequested) {
            activity.unbindService(connection)
            bindRequested = false
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

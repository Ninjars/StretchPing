package jez.stretchping.service

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class ActiveTimerServiceController(private val activity: Activity) {
    private val serviceMutableState: MutableStateFlow<ActiveTimerService?> = MutableStateFlow(null)
    val serviceState: StateFlow<ActiveTimerService?> = serviceMutableState

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as ActiveTimerService.LocalBinder
            serviceMutableState.value = binder.getService()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceMutableState.value = null
        }
    }

    fun bind() {
        if (!isBound()) {
            with(activity) {
                bindService(
                    Intent(this, ActiveTimerService::class.java),
                    connection,
                    Context.BIND_AUTO_CREATE
                )
            }
        }
    }

    fun unbind() {
        if (isBound()) {
            activity.unbindService(connection)
        }
    }

    private fun isBound(): Boolean = serviceMutableState.value != null
}

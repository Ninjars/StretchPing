package jez.stretchping.service

import dagger.hilt.android.scopes.ActivityRetainedScoped
import timber.log.Timber
import javax.inject.Inject

@ActivityRetainedScoped
class ActiveTimerServiceDispatcher @Inject constructor() {
    private lateinit var controller: ActiveTimerServiceController

    fun setController(controller: ActiveTimerServiceController) {
        Timber.e("setController")
        this.controller = controller
    }

    fun bind(callback: (ActiveTimerService) -> Unit) {
        Timber.e("bind")
        controller.bind(callback)
    }

    fun unbind() {
        Timber.e("unBind")
        controller.unbind()
    }

    fun startService() {
        controller.startService()
    }
}

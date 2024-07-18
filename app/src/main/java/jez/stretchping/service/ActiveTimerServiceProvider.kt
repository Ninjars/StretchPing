package jez.stretchping.service

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@ActivityRetainedScoped
class ActiveTimerServiceProvider @Inject constructor() {
    private val controller: MutableStateFlow<ActiveTimerServiceController?> = MutableStateFlow(null)
    private var shouldBind = false

    fun setController(controller: ActiveTimerServiceController) {
        this.controller.value = controller
        if (shouldBind) controller.bind()
    }

    fun bind() {
        shouldBind = true
        controller.value?.bind()
    }

    fun unbind() {
        shouldBind = false
        controller.value?.unbind()
    }

    @OptIn(FlowPreview::class)
    fun getServiceFlow(): Flow<ActiveTimerService?> =
        controller.flatMapConcat { it?.serviceState ?: flowOf(null) }
}

package jez.stretchping.features.activetimer

import androidx.core.util.Consumer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.utils.toViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ActiveTimerVM @Inject constructor() : Consumer<ActiveTimerVM.Event>, ViewModel() {
    private val stateFlow = MutableStateFlow(State())
    val viewState: StateFlow<ActiveTimerViewState> =
        stateFlow.toViewState(viewModelScope) { ActiveTimerStateToViewState(it) }

    override fun accept(t: Event?) {
        TODO("Not yet implemented")
    }

    sealed class Event {

    }

    data class State(
        val activeSegment: ActiveSegment? = null,
        val repeatCount: Int = -1,
        val queuedSegments: ArrayDeque<SegmentSpec> = ArrayDeque(),
    ) {
        data class ActiveSegment(
            val startedAtTime: Long,
            val startedAtFraction: Float,
            val endAtTime: Long,
            val pausedAtFraction: Float?,
            val mode: SegmentSpec.Mode,
        )

        data class SegmentSpec(
            val durationSeconds: Int,
            val mode: Mode,
        ) {
            enum class Mode {
                Stretch,
                Transition,
            }
        }
    }
}

package jez.stretchping.features.activetimer

import androidx.core.util.Consumer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine
import jez.stretchping.features.activetimer.logic.EventScheduler
import jez.stretchping.features.activetimer.view.ActiveTimerViewState
import jez.stretchping.service.ActiveTimerServiceProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject


@HiltViewModel
class ActiveTimerVM @Inject constructor(
    eventScheduler: EventScheduler,
    navigationDispatcher: NavigationDispatcher,
    serviceProvider: ActiveTimerServiceProvider,
    savedStateHandle: SavedStateHandle,
) : Consumer<ActiveTimerVM.Event>, ViewModel(), DefaultLifecycleObserver {
    private val exerciseConfig = savedStateHandle.get<String>(Route.ActiveTimer.routeConfig)!!
        .let { Json.decodeFromString<ExerciseConfig>(it) }

    private val engine =
        ActiveTimerEngine(eventScheduler, navigationDispatcher, serviceProvider, exerciseConfig)

    val viewState: StateFlow<ActiveTimerViewState> = engine.viewState

    init {
        accept(Event.Start)
    }

    override fun accept(event: Event) {
        engine.accept(event)
        viewModelScope
    }

    override fun onPause(owner: LifecycleOwner) {
        if (!exerciseConfig.playInBackground) {
            accept(Event.Pause)
        }
        super.onPause(owner)
    }

    override fun onCleared() {
        engine.dispose()
        super.onCleared()
    }

    sealed class Event {
        data object Start : Event()
        data object Pause : Event()
        data object BackPressed : Event()
        data object OnSectionCompleted : Event()
    }
}

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
import jez.stretchping.audio.TTSManager
import jez.stretchping.features.activetimer.logic.ActiveTimerEngine
import jez.stretchping.features.activetimer.logic.EventScheduler
import jez.stretchping.features.activetimer.view.ActiveTimerViewState
import jez.stretchping.persistence.EngineSettings
import jez.stretchping.persistence.ExerciseConfig
import jez.stretchping.persistence.SettingsRepository
import jez.stretchping.service.ActiveTimerServiceDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject


@HiltViewModel
class ActiveTimerVM @Inject constructor(
    private val eventScheduler: EventScheduler,
    private val navigationDispatcher: NavigationDispatcher,
    private val serviceDispatcher: ActiveTimerServiceDispatcher,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TTSManager,
    savedStateHandle: SavedStateHandle,
) : Consumer<ActiveTimerVM.Event>, ViewModel(), DefaultLifecycleObserver {
    private val exerciseConfig = savedStateHandle.get<String>(Route.ActiveTimer.routeConfig)!!
        .let { Json.decodeFromString<ExerciseConfig>(it) }

    private var engine: ActiveTimerEngine? = null
    private var engineViewModelJob: Job? = null

    private var cachedEngineSettings: EngineSettings? = null

    private val mutableViewState: MutableStateFlow<ActiveTimerViewState> =
        MutableStateFlow(ActiveTimerViewState(null, null, null))
    val viewState: StateFlow<ActiveTimerViewState> = mutableViewState

    init {
        serviceDispatcher.startService()
        ttsManager.initialise()
        loadEngineSettings()
    }

    private fun loadEngineSettings(callback: ((EngineSettings) -> Unit)? = null) {
        viewModelScope.launch {
            cachedEngineSettings = settingsRepository.engineSettings.first().also {
                callback?.invoke(it)
            }
        }
    }

    override fun accept(event: Event) {
        engine?.accept(event)
    }

    override fun onPause(owner: LifecycleOwner) {
        if (cachedEngineSettings?.playInBackground != true) {
            accept(Event.Pause)
            serviceDispatcher.unbind()
            engineViewModelJob?.cancel()
        }
        super.onPause(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        val currentEngineSettings = cachedEngineSettings
        if (currentEngineSettings == null) {
            loadEngineSettings { bindService(it) }
        } else {
            bindService(currentEngineSettings)
        }
    }

    override fun onCleared() {
        serviceDispatcher.unbind()
        ttsManager.destroy()
        super.onCleared()
    }

    private fun bindService(engineSettings: EngineSettings) {
        serviceDispatcher.bind { boundService ->
            val serviceEngine = boundService.getOrCreateEngine { onEndCallback ->
                ActiveTimerEngine(
                    onEndCallback,
                    eventScheduler,
                    navigationDispatcher,
                    engineSettings,
                    exerciseConfig,
                )
            }

            engine = serviceEngine

            engineViewModelJob?.cancel()
            engineViewModelJob = viewModelScope.launch {
                serviceEngine.viewState.collect {
                    mutableViewState.value = it
                }
            }

            if (!serviceEngine.hasStarted()) {
                accept(Event.Start)
            }
        }
    }

    sealed class Event {
        data object Start : Event()
        data object Pause : Event()
        data object BackPressed : Event()
        data object OnSegmentCompleted : Event()
    }
}

package jez.stretchping.features.activetimer.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jez.stretchping.R
import jez.stretchping.features.activetimer.ActiveTimerVM
import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.features.activetimer.view.ActiveTimerState.Mode
import jez.stretchping.ui.components.ArcProgressBar
import jez.stretchping.ui.components.CountdownTimer
import jez.stretchping.ui.components.SegmentDescriptionUi
import jez.stretchping.ui.components.TimerControls
import jez.stretchping.ui.components.TimerControlsEvent
import jez.stretchping.ui.components.TimerControlsViewState
import jez.stretchping.ui.theme.StretchPingTheme
import jez.stretchping.utils.observeLifecycle
import jez.stretchping.utils.previewState
import jez.stretchping.utils.rememberEventConsumer

@Composable
fun ActiveTimerScreen(
    viewModel: ActiveTimerVM
) {
    viewModel.observeLifecycle(LocalLifecycleOwner.current.lifecycle)
    ActiveTimerScreen(viewModel.viewState.collectAsState(), rememberEventConsumer(viewModel))
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ActiveTimerScreen(
    state: State<ActiveTimerViewState>,
    eventHandler: (Event) -> Unit,
) {
    val localFocusManager = LocalFocusManager.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    localFocusManager.clearFocus()
                })
            },
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = state.value.repCompleteHeading?.let {
                stringResource(id = R.string.rep_count_heading, it)
            } ?: "",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        MainContent(eventHandler) { state.value }
        Spacer(modifier = Modifier.weight(1f))
        TimerControls(
            eventHandler = {
                when (it) {
                    TimerControlsEvent.PauseClicked -> eventHandler(Event.Pause)
                    TimerControlsEvent.PlayClicked -> eventHandler(Event.Start)
                    TimerControlsEvent.BackClicked -> eventHandler(Event.BackPressed)
                    TimerControlsEvent.ResetClicked -> eventHandler(Event.RestartSegmentPressed)
                }
            },
        ) {
            with(state.value) {
                TimerControlsViewState(
                    mainButtonEnabled = true,
                    showMainButton = true,
                    showBackWhenPaused = true,
                    showResetSegment = true,
                    isPaused = activeTimer?.isPaused ?: true,
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun MainContent(
    eventHandler: (Event) -> Unit,
    stateProvider: () -> ActiveTimerViewState,
) {
    val state = stateProvider()

    BackHandler(enabled = state.activeTimer?.isPaused?.not() ?: false) {
        eventHandler(Event.Pause)
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
    ) {
        state.activeTimer?.run {
            CountdownTimer(
                startAtFraction = startAtFraction,
                endTimeMillis = endTimeMillis,
                pausedAtFraction = pausedAtFraction,
            ) { progress ->
                if (maxWidth > maxHeight) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(16.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else {
                    ArcProgressBar(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }
            }
        }

        state.segmentDescription?.let {
            SegmentDescriptionUi(it)
        }
    }
}

@Preview
@Composable
private fun ActiveTimerScreenPreview() {
    StretchPingTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ActiveTimerScreen(
                state = previewState {
                    ActiveTimerViewState(
                        activeTimer = ActiveTimerState(
                            startAtFraction = 0f,
                            endTimeMillis = 100,
                            pausedAtFraction = 0.33f,
                            mode = Mode.Stretch,
                        ),
                        segmentDescription = SegmentDescription(
                            name = "Segment Name",
                            mode = Mode.Stretch,
                            duration = Duration(1, 15),
                            position = "1 / 5",
                        ),
                        repCompleteHeading = "2",
                    )
                }
            ) {}
        }
    }
}

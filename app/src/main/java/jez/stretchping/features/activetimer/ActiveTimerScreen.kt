package jez.stretchping.features.activetimer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jez.stretchping.R
import jez.stretchping.features.activetimer.ActiveTimerState.Mode
import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.ui.components.ActiveSegmentControls
import jez.stretchping.ui.components.CountdownTimer
import jez.stretchping.ui.components.PlanningActivityControls
import jez.stretchping.ui.components.SegmentDescriptionUi
import jez.stretchping.ui.theme.StretchPingTheme
import jez.stretchping.utils.observeLifecycle
import jez.stretchping.utils.previewState
import jez.stretchping.utils.rememberEventConsumer
import kotlinx.coroutines.delay

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
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    localFocusManager.clearFocus()
                })
            },
    ) {
        Spacer(modifier = Modifier.height(36.dp))
        Title(modifier = Modifier.fillMaxWidth())
        LoadingTransitionAnimator(
            modifier = Modifier.weight(1f),
            isVisible = { !state.value.isLoading },
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                MainContent(eventHandler) { state.value }
            }
        }
        LoadingTransitionAnimator(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            isVisible = { !state.value.isLoading },
        ) {
            ActiveSegmentControls(
                eventHandler = eventHandler,
            ) { state.value }
        }
    }
}

@Composable
private fun LoadingTransitionAnimator(
    isVisible: () -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    /*
    * awkward setup to always show the entry animation even if loading is
    * so fast that it's set by the first frame.
    */
    val makeVisible = isVisible()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(makeVisible) {
        delay(500)
        visible = makeVisible
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { it },
        ),
        exit = fadeOut() + slideOutVertically(
            targetOffsetY = { it },
        )
    ) {
        content()
    }
}

@Composable
private fun Title(
    modifier: Modifier = Modifier,
) {
    var visible1 by remember { mutableStateOf(false) }
    var visible2 by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible1 = true
        delay(500)
        visible2 = true
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(
            visible = visible1,
            enter = slideInHorizontally(
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioLowBouncy,
                ),
                initialOffsetX = { -it * 4 }
            ),
        ) {
            Text(
                text = stringResource(id = R.string.title_part_1),
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        AnimatedVisibility(
            visible = visible2,
            enter = expandHorizontally(
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioHighBouncy,
                ),
                expandFrom = Alignment.Start,
                clip = false,
            ),
        ) {
            Text(
                text = stringResource(id = R.string.title_part_2),
                style = MaterialTheme.typography.headlineLarge,
            )
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
    if (state.isLoading) return

    AnimatedContent(
        contentAlignment = Alignment.Center,
        targetState = state.activeTimer != null,
        transitionSpec = {
            fadeIn(animationSpec = tween(250, delayMillis = 45)) with
                    fadeOut(animationSpec = tween(90))
        }
    ) { isRunning ->
        if (isRunning) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                state.activeTimer?.run {
                    CountdownTimer(
                        startAtFraction = startAtFraction,
                        durationMillis = durationMillis,
                        pausedAtFraction = pausedAtFraction,
                        modifier = Modifier.fillMaxWidth(0.9f),
                    )
                }

                state.segmentDescription?.let {
                    SegmentDescriptionUi(it)
                }
            }
        } else {
            state.editTimerState?.let {
                PlanningActivityControls(it, eventHandler)
            }
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
                            durationMillis = 100,
                            pausedAtFraction = 0.33f,
                            mode = Mode.Stretch,
                        ),
                        segmentDescription = SegmentDescription(
                            mode = Mode.Stretch,
                            duration = Duration(1, 15),
                            repsRemaining = "∞",
                        ),
                        editTimerState = null,
                    )
                }
            ) {}
        }
    }
}

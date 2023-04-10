package jez.stretchping.features.activetimer

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jez.stretchping.R
import jez.stretchping.features.activetimer.ActiveTimerState.Mode
import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.ui.components.CountdownTimer
import jez.stretchping.ui.components.TriStateToggle
import jez.stretchping.ui.theme.StretchPingTheme
import jez.stretchping.utils.observeLifecycle
import jez.stretchping.utils.previewState
import jez.stretchping.utils.rememberEventConsumer
import kotlinx.coroutines.delay

private const val SecondaryButtonSize = 60
private const val PrimaryButtonSize = 84

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
            Controls(
                eventHandler = eventHandler,
            ) { state.value.activeTimer }
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

@Composable
private fun Controls(
    eventHandler: (Event) -> Unit,
    modifier: Modifier = Modifier,
    stateProvider: () -> ActiveTimerState?,
) {
    val timerState = stateProvider()
    Row(
        modifier = modifier
            .padding(48.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crossfade(targetState = timerState?.isPaused == true) {
            if (it) {
                CircleButton(
                    onClick = { eventHandler(Event.Reset) },
                    imageVector = Icons.Rounded.Undo,
                    contentDescription = stringResource(id = R.string.timer_resume),
                    modifier = Modifier
                        .size(SecondaryButtonSize.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    )
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .size(SecondaryButtonSize.dp),
                )
            }
        }

        MainButton(
            eventHandler = eventHandler,
            state = timerState,
            modifier = Modifier
                .size(PrimaryButtonSize.dp)
        )

        Spacer(
            modifier = Modifier
                .size(SecondaryButtonSize.dp),
        )
    }
}

@Composable
private fun MainButton(
    eventHandler: (Event) -> Unit,
    state: ActiveTimerState?,
    modifier: Modifier = Modifier,
) {
    val isPlay = state == null || state.isPaused
    CircleButton(
        onClick = if (isPlay) {
            { eventHandler(Event.Start) }
        } else {
            { eventHandler(Event.Pause) }
        },
        imageVector = when {
            isPlay -> Icons.Rounded.PlayArrow
            else -> Icons.Rounded.Pause
        },
        contentDescription = stringResource(id = R.string.timer_start),
        modifier = modifier,
    )
}

@Composable
private fun CircleButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = colors,
        contentPadding = PaddingValues(8.dp),
        modifier = modifier
    ) {
        Crossfade(targetState = imageVector) {
            Icon(
                imageVector = it,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize()
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
                    TextContent(it)
                }
            }
        } else {
            state.editTimerState?.let {
                EditableConfig(it, eventHandler)
            }
        }
    }
}

@Composable
private fun EditableConfig(
    state: EditTimerState,
    eventHandler: (Event) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(0.9f),
    ) {
        val focusManager = LocalFocusManager.current

        // Edit Stretch Duration
        SelectOnFocusTextField(
            text = state.activeDuration,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 30.sp
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions {
                focusManager.moveFocus(FocusDirection.Next)
            },
            label = {
                Text(stringResource(id = R.string.stretch_duration))
            },
        ) {
            eventHandler(Event.SetStretchDuration(it))
        }

        // Edit Transition Duration
        SelectOnFocusTextField(
            text = state.breakDuration,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 30.sp
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions {
                focusManager.moveFocus(FocusDirection.Next)
            },
            label = {
                Text(stringResource(id = R.string.transition_duration))
            },
        ) {
            eventHandler(Event.SetBreakDuration(it))
        }

        // Edit Rep Count
        val repCount = state.repCount
        SelectOnFocusTextField(
            text = repCount,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 30.sp
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            label = {
                Text(stringResource(id = R.string.rep_count))
            },
        ) {
            eventHandler(Event.SetRepCount(it))
        }

        // Theme Selection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(id = R.string.theme_title))
            TriStateToggle(
                states = state.themeState.optionStringResources.map { stringResource(id = it) },
                selectedIndex = state.themeState.selectedIndex,
            ) {
                eventHandler(Event.UpdateTheme(it))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectOnFocusTextField(
    text: String,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    label: @Composable (() -> Unit)? = null,
    onValueChanged: (String) -> Unit,
) {
    val fieldState = remember { mutableStateOf(TextFieldValue(text)) }
    fieldState.value = fieldState.value.copy(text = text)

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) {
        fieldState.value = fieldState.value.copy(
            selection = if (isFocused) {
                TextRange(
                    start = 0,
                    end = fieldState.value.text.length
                )
            } else {
                TextRange.Zero
            }
        )
    }

    TextField(
        interactionSource = interactionSource,
        value = fieldState.value,
        label = label,
        textStyle = textStyle,
        onValueChange = {
            fieldState.value = it
            onValueChanged(it.text)
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        colors = with(MaterialTheme.colorScheme) {
            TextFieldDefaults.textFieldColors(
                containerColor = secondaryContainer,
                textColor = onSecondaryContainer,
            )
        },
    )
}

@Composable
private fun TextContent(
    state: SegmentDescription,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedText(
            text = stringResource(state.mode.toStringRes()),
        ) {
            Text(
                text = it,
                fontSize = 24.sp
            )
        }
        Row {
            AnimatedText(
                text = state.duration.asSeconds().toString(),
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 72.sp)) {
                            append(it)
                        }
                        withStyle(SpanStyle(fontSize = 30.sp)) {
                            append("s")
                        }
                    },
                )
            }
        }
        AnimatedText(
            text = state.repsRemaining,
        ) {
            Text(
                text = it,
                fontSize = 18.sp,
            )
        }
    }
}

@StringRes
private fun Mode.toStringRes(): Int =
    when (this) {
        Mode.Stretch -> R.string.mode_stretch
        Mode.Transition -> R.string.mode_transition
    }

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedText(
    text: String,
    content: @Composable (String) -> Unit
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            fadeIn(animationSpec = tween(250, delayMillis = 45)) with
                    fadeOut(animationSpec = tween(90))
        }
    ) {
        content(it)
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

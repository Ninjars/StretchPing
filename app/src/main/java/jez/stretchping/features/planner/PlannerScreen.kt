package jez.stretchping.features.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jez.stretchping.R
import jez.stretchping.ui.components.SelectOnFocusTextField
import jez.stretchping.ui.components.TimerControls
import jez.stretchping.ui.components.TimerControlsEvent
import jez.stretchping.ui.components.TimerControlsViewState
import jez.stretchping.ui.theme.StretchPingTheme
import jez.stretchping.utils.previewState
import jez.stretchping.utils.rememberEventConsumer
import jez.stretchping.utils.toFlooredInt
import kotlin.math.max

@Composable
fun PlannerScreen(
    viewModel: PlannerVM,
) {
    PlannerScreen(
        viewModel.viewState.collectAsState(),
        rememberEventConsumer(viewModel)
    )
}

@Composable
private fun PlannerScreen(
    viewState: State<PlannerViewState>,
    eventHandler: (PlannerUIEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Content(viewState, eventHandler)
        FloatingActionButton(
            onClick = { eventHandler(PlannerUIEvent.NewSectionClicked) },
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.desc_add_plan_section)
            )
        }
        with(viewState.value.repeat) {
            FloatingActionButton(
                onClick = { eventHandler(PlannerUIEvent.UpdateIsRepeated(!this)) },
                shape = CircleShape,
                containerColor = if (this) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Loop,
                    contentDescription = if (this) {
                        stringResource(R.string.desc_repeat_toggle_enabled)
                    } else {
                        stringResource(R.string.desc_repeat_toggle_disabled)
                    }
                )
                if (!this) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        TimerControls(
            eventHandler = {
                when (it) {
                    TimerControlsEvent.PlayClicked -> eventHandler(PlannerUIEvent.Start)
                    TimerControlsEvent.PauseClicked,
                    TimerControlsEvent.BackClicked -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            with(viewState.value) {
                TimerControlsViewState(
                    mainButtonEnabled = canStart,
                    showMainButton = true,
                    showBackWhenPaused = false,
                    isPaused = true,
                )
            }
        }
    }
}

@Composable
private fun Content(
    viewState: State<PlannerViewState>,
    eventHandler: (PlannerUIEvent) -> Unit,
) {
    val state = viewState.value
    if (state.sections.isEmpty()) {
        EmptyContent(state.planName, eventHandler)
    } else {
        PopulatedContent(state, eventHandler)
    }
}

@Composable
private fun PopulatedContent(
    state: PlannerViewState,
    eventHandler: (PlannerUIEvent) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header", contentType = "header") {
            PlanHeaderView(state.planName, eventHandler)
        }
        items(
            items = state.sections,
            key = { it.id },
            contentType = { "content" }
        ) {
            PlanSectionView(it, eventHandler)
        }
    }
}

@Composable
private fun EmptyContent(
    planName: String,
    eventHandler: (PlannerUIEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PlanHeaderView(planName, eventHandler)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .clickable { eventHandler(PlannerUIEvent.NewSectionClicked) }
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = stringResource(R.string.desc_add_plan_section),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlanHeaderView(
    planName: String,
    eventHandler: (PlannerUIEvent) -> Unit,
) {
    TextField(
        value = planName,
        onValueChange = { eventHandler(PlannerUIEvent.UpdatePlanName(it)) },
        textStyle = LocalTextStyle.current.copy(
            fontSize = 30.sp
        ),
        label = { Text(text = stringResource(R.string.label_plan_name)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PlanSectionView(
    section: PlannerViewState.Section,
    eventHandler: (PlannerUIEvent) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val focusManager = LocalFocusManager.current
            // Name
            SelectOnFocusTextField(
                text = section.name,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 22.sp
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions {
                    focusManager.moveFocus(FocusDirection.Next)
                },
                label = {
                    Text(stringResource(id = R.string.label_section_name))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                eventHandler(PlannerUIEvent.UpdateSectionName(section.id, it))
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Rep Count
                SelectOnFocusTextField(
                    modifier = Modifier.weight(1f),
                    text = section.repCount,
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
                        Text(stringResource(id = R.string.rep_count))
                    },
                ) {
                    it.toFlooredInt()?.let { int ->
                        eventHandler(PlannerUIEvent.UpdateSectionRepCount(section.id, max(0, int)))
                    }
                }

                // Initial Delay
                SelectOnFocusTextField(
                    modifier = Modifier.weight(1f),
                    text = section.entryTransitionDuration,
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
                        Text(stringResource(id = R.string.label_section_start_delay))
                    },
                ) {
                    it.toFlooredInt()?.let { int ->
                        eventHandler(
                            PlannerUIEvent.UpdateSectionEntryTransitionDuration(
                                section.id,
                                max(0, int)
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Stretch Duration
                SelectOnFocusTextField(
                    modifier = Modifier.weight(1f),
                    text = section.repDuration,
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
                        Text(stringResource(id = R.string.active_duration))
                    },
                ) {
                    it.toFlooredInt()?.let { int ->
                        eventHandler(
                            PlannerUIEvent.UpdateSectionRepDuration(
                                section.id,
                                max(0, int)
                            )
                        )
                    }
                }

                // Break Duration
                SelectOnFocusTextField(
                    modifier = Modifier.weight(1f),
                    text = section.repTransitionDuration,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 30.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    label = {
                        Text(stringResource(id = R.string.transition_duration))
                    },
                ) {
                    it.toFlooredInt()?.let { int ->
                        eventHandler(
                            PlannerUIEvent.UpdateSectionRepTransitionDuration(
                                section.id,
                                max(0, int)
                            )
                        )
                    }
                }
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
            PlannerScreen(
                viewState = previewState {
                    PlannerViewState(
                        planName = "",
                        repeat = false,
                        canStart = false,
                        sections = listOf(
                            PlannerViewState.Section(
                                id = "",
                                name = "",
                                repCount = "6",
                                entryTransitionDuration = "12",
                                repDuration = "10",
                                repTransitionDuration = "5",
                            )
                        )
                    )
                }
            ) {}
        }
    }
}

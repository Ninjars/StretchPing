@file:OptIn(ExperimentalFoundationApi::class)

package jez.stretchping.features.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.redfoxstudio.stretchping.R
import jez.stretchping.ui.components.FocusingInputFieldWithPicker
import jez.stretchping.ui.components.SelectOnFocusTextField
import jez.stretchping.ui.components.TimerControls
import jez.stretchping.ui.components.TimerControlsEvent
import jez.stretchping.ui.components.TimerControlsViewState
import jez.stretchping.ui.theme.StretchPingTheme
import jez.stretchping.ui.theme.secondaryTextFieldColors
import jez.stretchping.utils.ExerciseConstants
import jez.stretchping.utils.previewState
import jez.stretchping.utils.rememberEventConsumer
import jez.stretchping.utils.toFlooredInt
import kotlinx.coroutines.job
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
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
                    imageVector = if (this) {
                        Icons.Default.Loop
                    } else {
                        Icons.Default.Start
                    },
                    contentDescription = if (this) {
                        stringResource(R.string.desc_repeat_toggle_enabled)
                    } else {
                        stringResource(R.string.desc_repeat_toggle_disabled)
                    }
                )
            }
        }
        TimerControls(
            eventHandler = {
                when (it) {
                    TimerControlsEvent.PlayClicked -> eventHandler(PlannerUIEvent.StartClicked)
                    TimerControlsEvent.PauseClicked,
                    TimerControlsEvent.ResetClicked,
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
                    showResetSegment = false,
                    isPaused = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    viewState: State<PlannerViewState>,
    eventHandler: (PlannerUIEvent) -> Unit,
) {
    val state = viewState.value

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // offset indexes by 1 to account for title item
        eventHandler(PlannerUIEvent.RepositionSection(from.index - 1, to.index - 1))
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header", contentType = "header") {
            PlanHeaderView(state.isInitialised, state.planName, eventHandler)
        }
        items(
            items = state.sections,
            key = { it.id },
            contentType = { "content" }
        ) { section ->
            ReorderableItem(state = reorderableLazyListState, key = section.id) { isDragging ->
                PlanSectionView(
                    section = section,
                    canStartPlan = state.canStart,
                    isDragging = isDragging,
                    eventHandler = eventHandler,
                )
            }
        }
        item(key = "footer", contentType = "footer") {
            AddSectionItemView { eventHandler(PlannerUIEvent.NewSectionClicked) }
            Spacer(modifier = Modifier.height(112.dp))
        }
    }
}

@Composable
private fun AddSectionItemView(
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = null
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = stringResource(R.string.desc_add_plan_section),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun PlanHeaderView(
    isInitialised: Boolean,
    planName: String,
    eventHandler: (PlannerUIEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = planName,
            onValueChange = { eventHandler(PlannerUIEvent.UpdatePlanName(it)) },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 30.sp
            ),
            label = { Text(text = stringResource(R.string.label_plan_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            colors = secondaryTextFieldColors(),
            modifier = Modifier
                .focusRequester(focusRequester)
                .weight(1f)
        )
        DeleteButton(
            title = stringResource(R.string.delete_plan_title),
            text = stringResource(R.string.delete_plan_text),
            confirm = stringResource(R.string.delete_plan_button_confirm),
            dismiss = stringResource(R.string.delete_plan_button_dismiss),
            contentDescription = stringResource(id = R.string.desc_delete_plan),
        ) {
            eventHandler(PlannerUIEvent.DeletePlanClicked)
        }
    }
    LaunchedEffect(isInitialised) {
        if (isInitialised && planName.isBlank()) {
            coroutineContext.job.invokeOnCompletion {
                focusRequester.requestFocus()
            }
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.PlanSectionView(
    section: PlannerViewState.Section,
    canStartPlan: Boolean,
    isDragging: Boolean,
    eventHandler: (PlannerUIEvent) -> Unit,
) {
    Box {
        Card(
            border = if (isDragging) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else null,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(8.dp)
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(IntrinsicSize.Min)

                ) {
                    DeleteButton(
                        contentDescription = stringResource(id = R.string.desc_delete_section),
                        size = 32.dp,
                        title = stringResource(R.string.delete_section_title),
                        text = stringResource(R.string.delete_section_text),
                        confirm = stringResource(R.string.delete_section_button_confirm),
                        dismiss = stringResource(R.string.delete_section_button_dismiss),
                        modifier = Modifier

                    ) {
                        eventHandler(PlannerUIEvent.DeleteSectionClicked(section.id))
                    }
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = stringResource(id = R.string.desc_drag_section),
                        modifier = Modifier
                            .draggableHandle()
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                PlanSectionViewContent(section, canStartPlan, eventHandler)
            }
        }
    }
}

@Composable
private fun PlanSectionViewContent(
    section: PlannerViewState.Section,
    canStartPlan: Boolean,
    eventHandler: (PlannerUIEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Name
            SelectOnFocusTextField(
                text = section.name,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 22.sp
                ),
                useOutlinedTextField = false,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions {
                    focusManager.moveFocus(FocusDirection.Next)
                },
                label = {
                    Text(stringResource(id = R.string.label_section_name))
                },
                modifier = Modifier.weight(1f)
            ) {
                eventHandler(PlannerUIEvent.UpdateSectionName(section.id, it))
            }
            AnimatedVisibility(
                visible = canStartPlan,
            ) {
                Button(
                    onClick = { eventHandler(PlannerUIEvent.StartFromSectionClicked(section.id)) },
                    shape = CircleShape,
                    contentPadding = PaddingValues(8.dp),
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.play_from_section),
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Rep Count
            PlanSectionNumberInput(
                labelText = stringResource(id = R.string.rep_count),
                text = section.repCount,
                pickerOptions = ExerciseConstants.RepCounts,
                modifier = Modifier.weight(1f)
            ) {
                eventHandler(
                    PlannerUIEvent.UpdateSectionRepCount(section.id, max(0, it))
                )
            }

            // Initial Delay
            PlanSectionNumberInput(
                labelText = stringResource(id = R.string.label_section_start_delay),
                text = section.entryTransitionDuration,
                pickerOptions = ExerciseConstants.TransitionDurations,
                modifier = Modifier.weight(1f)
            ) {
                eventHandler(
                    PlannerUIEvent.UpdateSectionEntryTransitionDuration(
                        section.id,
                        max(0, it)
                    )
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Stretch Duration
            PlanSectionNumberInput(
                labelText = stringResource(id = R.string.active_duration),
                text = section.repDuration,
                pickerOptions = ExerciseConstants.StretchDurations,
                modifier = Modifier.weight(1f)
            ) {
                eventHandler(
                    PlannerUIEvent.UpdateSectionRepDuration(
                        section.id,
                        max(0, it)
                    )
                )
            }

            // Break Duration
            PlanSectionNumberInput(
                labelText = stringResource(id = R.string.transition_duration),
                text = section.repTransitionDuration,
                pickerOptions = ExerciseConstants.TransitionDurations,
                imeAction = ImeAction.Done,
                modifier = Modifier.weight(1f)
            ) {
                eventHandler(
                    PlannerUIEvent.UpdateSectionRepTransitionDuration(
                        section.id,
                        max(0, it)
                    )
                )
            }
        }
    }
}


@Composable
private fun PlanSectionNumberInput(
    labelText: String,
    text: String,
    pickerOptions: List<String>,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onChange: (Int) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    FocusingInputFieldWithPicker(
        modifier = modifier,
        pickerOptions = pickerOptions,
        text = text,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 30.sp
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        keyboardActions = if (imeAction == ImeAction.Next) {
            KeyboardActions {
                focusManager.moveFocus(FocusDirection.Next)
            }
        } else {
            KeyboardActions()
        },
        colors = secondaryTextFieldColors(),
        label = { Text(labelText) },
    ) {
        it.toFlooredInt()?.let { int -> onChange(int) }
    }
}

@Composable
private fun DeleteButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    title: String,
    text: String,
    confirm: String,
    dismiss: String,
    size: Dp = 48.dp,
    onClick: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Button(
        onClick = { showDialog = true },
        shape = CircleShape,
        contentPadding = PaddingValues(size / 6),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = modifier.size(size),
    ) {
        Icon(
            imageVector = Icons.Default.DeleteForever,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(title)
            },
            text = {
                Text(text)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onClick()
                    }) {
                    Text(confirm)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog = false
                    }) {
                    Text(dismiss)
                }
            }
        )
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
                        isInitialised = true,
                        planName = "",
                        repeat = false,
                        canStart = true,
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

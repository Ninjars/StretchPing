package jez.stretchping.features.planslist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import jez.stretchping.R
import jez.stretchping.ui.theme.StretchPingTheme
import jez.stretchping.utils.previewState
import jez.stretchping.utils.rememberEventConsumer

@Composable
fun PlansListScreen(
    viewModel: PlansListVM,
) {
    PlansListScreen(
        viewModel.viewState.collectAsState(),
        rememberEventConsumer(viewModel)
    )
}

@Composable
private fun PlansListScreen(
    viewState: State<PlansListViewState>,
    eventHandler: (PlansListUIEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Content(viewState, eventHandler)
        FloatingActionButton(
            onClick = { eventHandler(PlansListUIEvent.NewPlanClicked) },
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_plan)
            )
        }
    }
}

@Composable
private fun Content(
    viewState: State<PlansListViewState>,
    eventHandler: (PlansListUIEvent) -> Unit,
) {
    val state = viewState.value
    if (state.plans.isEmpty()) {
        EmptyState(eventHandler)
    } else {
        PopulatedState(plans = state.plans, eventHandler = eventHandler)
    }
}

@Composable
private fun PopulatedState(
    plans: List<PlansListViewState.Plan>,
    eventHandler: (PlansListUIEvent) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = plans,
            key = { it.id },
            contentType = { "content" }
        ) {
            PlanSectionView(it, eventHandler)
        }
    }
}

@Composable
private fun EmptyState(
    eventHandler: (PlansListUIEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                    .clickable { eventHandler(PlansListUIEvent.NewPlanClicked) }
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = stringResource(R.string.add_plan),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanSectionView(
    plan: PlansListViewState.Plan,
    eventHandler: (PlansListUIEvent) -> Unit,
) {
    Card(
        modifier = Modifier
            .combinedClickable(
                onClick = { eventHandler(PlansListUIEvent.OpenPlanClicked(plan.id)) },
                onDoubleClick = null
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = plan.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (plan.isLooping) {
                Icon(
                    imageVector = Icons.Default.Loop,
                    contentDescription = stringResource(R.string.desc_repeat_toggle_enabled),
                    modifier = Modifier.size(24.dp)
                )
            }
            Button(
                enabled = plan.canStart,
                onClick = { eventHandler(PlansListUIEvent.StartPlanClicked(plan.id)) },
                shape = CircleShape,
                contentPadding = PaddingValues(8.dp),
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.desc_start_plan),
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}

class PreviewPlansProvider : PreviewParameterProvider<PlansListViewState> {
    override val values: Sequence<PlansListViewState> = sequenceOf(
        PlansListViewState(plans = emptyList()),
        PlansListViewState(
            plans = listOf(
                PlansListViewState.Plan(
                    id = "1",
                    name = "Plan 1",
                    isLooping = true,
                    canStart = true,
                ),
                PlansListViewState.Plan(
                    id = "2",
                    name = "Plan 2",
                    isLooping = false,
                    canStart = false,
                ),
            )
        ),
    )

}

@Preview
@Composable
private fun ActiveTimerScreenPopulatedPreview(
    @PreviewParameter(PreviewPlansProvider::class) planState: PlansListViewState
) {
    StretchPingTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            PlansListScreen(
                viewState = previewState { planState }
            ) {}
        }
    }
}

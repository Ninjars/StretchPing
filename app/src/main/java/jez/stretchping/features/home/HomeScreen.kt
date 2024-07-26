package jez.stretchping.features.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import jez.stretchping.R
import jez.stretchping.features.edittimer.EditTimerScreen
import jez.stretchping.features.edittimer.EditTimerVM
import jez.stretchping.features.planslist.PlansListScreen
import jez.stretchping.features.planslist.PlansListVM
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    editTimerVM: EditTimerVM,
    plansListVM: PlansListVM
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    Column(
        modifier = Modifier.padding(top = 16.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            MarkerBox(pagerState = pagerState)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                PageHeader(
                    text = stringResource(id = R.string.page_simple),
                    pagerState = pagerState,
                    index = 0,
                )
                PageHeader(
                    text = stringResource(id = R.string.page_plan),
                    pagerState = pagerState,
                    index = 1,
                )
            }
        }
        HorizontalPager(
            state = pagerState,
        ) { page ->
            when (page) {
                0 -> EditTimerScreen(viewModel = editTimerVM)
                1 -> PlansListScreen(viewModel = plansListVM)
                else -> throw IllegalStateException("Page $page unsupported")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxWithConstraintsScope.MarkerBox(
    pagerState: PagerState,
) {
    val steps = maxWidth / pagerState.pageCount.toFloat()
    val targetOffset by remember(pagerState.targetPage) {
        mutableFloatStateOf((steps * pagerState.targetPage).value)
    }
    val offsetValue by animateFloatAsState(targetValue = targetOffset)
    Box(
        modifier = Modifier
            .width(width = maxWidth / 2f)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .offset {
                IntOffset(
                    offsetValue
                        .toDp()
                        .roundToPx(), 0
                )
            }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.PageHeader(
    text: String,
    pagerState: PagerState,
    index: Int,
) {
    val scope = rememberCoroutineScope()
    val color =
        animateColorAsState(targetValue = if (pagerState.targetPage == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .weight(1f)
            .background(
                color = color.value
            )
            .clickable(role = Role.Tab) {
                scope.launch { pagerState.scrollToPage(index) }
            }
            .padding(8.dp)
    )
}

package jez.stretchping.features.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
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
    plansListVM: PlansListVM,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    Column(
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
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
private fun RowScope.PageHeader(
    text: String,
    pagerState: PagerState,
    index: Int,
) {
    val scope = rememberCoroutineScope()
    val color =
        animateColorAsState(targetValue = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
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

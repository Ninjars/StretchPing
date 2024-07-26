package jez.stretchping.features.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import jez.stretchping.features.edittimer.EditTimerScreen
import jez.stretchping.features.edittimer.EditTimerVM
import jez.stretchping.features.planslist.PlansListScreen
import jez.stretchping.features.planslist.PlansListVM

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    editTimerVM: EditTimerVM,
    plansListVM: PlansListVM
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
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

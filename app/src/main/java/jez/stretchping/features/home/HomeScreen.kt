package jez.stretchping.features.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
    Column {
        HorizontalPager(
            modifier = Modifier.weight(1f),
            state = pagerState,
        ) { page ->
            when (page) {
                0 -> EditTimerScreen(viewModel = editTimerVM)
                1 -> PlansListScreen(viewModel = plansListVM)
                else -> throw IllegalStateException("Page $page unsupported")
            }
        }
        NavigationBar {
            NavBarItem(
                pagerState = pagerState,
                index = 0,
                label = R.string.page_simple,
                icon = ImageVector.vectorResource(R.drawable.nav_physical_therapy),
            )
            NavBarItem(
                pagerState = pagerState,
                index = 1,
                label = R.string.page_plan,
                icon = Icons.AutoMirrored.Default.List,
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun RowScope.NavBarItem(
    pagerState: PagerState,
    index: Int,
    @StringRes label: Int,
    icon: ImageVector,
) {
    val scope = rememberCoroutineScope()
    val onClick = remember(index) {
        { scope.launch { pagerState.animateScrollToPage(index, animationSpec = tween()) } }
    }
    val text = stringResource(id = label)
    NavigationBarItem(
        selected = pagerState.currentPage == index,
        onClick = { onClick() },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
        },
        label = { Text(text = text) },
    )
}

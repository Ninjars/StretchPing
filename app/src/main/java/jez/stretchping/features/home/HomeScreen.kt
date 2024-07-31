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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import jez.stretchping.features.settings.SettingsScreen
import jez.stretchping.features.settings.SettingsVM
import jez.stretchping.persistence.NavLabelDisplayMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    homeScreenVM: HomeScreenVM,
    editTimerVM: EditTimerVM,
    plansListVM: PlansListVM,
    settingsVM: SettingsVM,
) {
    val homeScreenState = homeScreenVM.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    Column {
        HorizontalPager(
            modifier = Modifier.weight(1f),
            state = pagerState,
        ) { page ->
            when (page) {
                0 -> EditTimerScreen(viewModel = editTimerVM)
                1 -> PlansListScreen(viewModel = plansListVM)
                2 -> SettingsScreen(viewModel = settingsVM)
                else -> throw IllegalStateException("Page $page unsupported")
            }
        }
        Navigation(pagerState) { homeScreenState.value }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Navigation(
    pagerState: PagerState,
    modeProvider: () -> NavLabelDisplayMode,
) {
    val mode = modeProvider()
    NavigationBar {
        NavBarItem(
            pagerState = pagerState,
            index = 0,
            label = R.string.page_simple,
            icon = ImageVector.vectorResource(R.drawable.nav_physical_therapy),
            mode = mode,
        )
        NavBarItem(
            pagerState = pagerState,
            index = 1,
            label = R.string.page_plan,
            icon = Icons.Default.FitnessCenter,
            mode = mode,
        )
        NavBarItem(
            pagerState = pagerState,
            index = 2,
            label = R.string.page_settings,
            icon = Icons.Default.Settings,
            mode = mode,
        )
    }
}

@ExperimentalFoundationApi
@Composable
private fun RowScope.NavBarItem(
    pagerState: PagerState,
    index: Int,
    @StringRes label: Int,
    icon: ImageVector,
    mode: NavLabelDisplayMode,
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
        alwaysShowLabel = when (mode) {
            NavLabelDisplayMode.Always -> true
            else -> false
        },
        label = when (mode) {
            NavLabelDisplayMode.Selected,
            NavLabelDisplayMode.Always -> {
                { Text(text = text) }
            }

            else -> null
        },
    )
}

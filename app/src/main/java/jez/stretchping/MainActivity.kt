package jez.stretchping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import jez.stretchping.features.activetimer.view.ActiveTimerScreen
import jez.stretchping.features.edittimer.EditTimerScreen
import jez.stretchping.persistence.Settings
import jez.stretchping.persistence.ThemeMode
import jez.stretchping.service.ActiveTimerServiceController
import jez.stretchping.service.ActiveTimerServiceDispatcher
import jez.stretchping.ui.theme.StretchPingTheme
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    @Inject
    lateinit var serviceProvider: ActiveTimerServiceDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serviceProvider.setController(ActiveTimerServiceController(this))

        setContent {
            val navController = rememberNavController()
            navigationDispatcher.setNavListener { route ->
                when (route) {
                    is Route.Back -> navController.popBackStack()
                    else -> navController.navigate(route.routeId, route.navOptions)
                }
            }

            val themeModeState = settings.themeMode.collectAsState(initial = ThemeMode.Unset)
            ThemedContent(
                themeMode = themeModeState.value,
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Title(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 36.dp)
                        )
                        NavHost(
                            navController = navController,
                            startDestination = Route.EditTimer.routeId
                        ) {
                            composable(Route.EditTimer.routeId) {
                                EditTimerScreen(hiltViewModel())
                            }
                            composable(
                                Route.ActiveTimer.baseRouteId,
                                arguments = listOf(
                                    navArgument(Route.ActiveTimer.routeConfig) {
                                        type = NavType.StringType
                                    }
                                )
                            ) {
                                ActiveTimerScreen(hiltViewModel())
                            }
                        }
                    }
                }
            }
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
    private fun ThemedContent(
        themeMode: ThemeMode,
        content: @Composable () -> Unit,
    ) {
        val isDarkTheme = when (themeMode) {
            ThemeMode.Unset,
            ThemeMode.System -> isSystemInDarkTheme()

            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }

        if (themeMode != ThemeMode.Unset) {
            StretchPingTheme(
                isDarkTheme = isDarkTheme
            ) {
                content()
            }
        }
    }
}

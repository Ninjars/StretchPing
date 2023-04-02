package jez.stretchping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import jez.stretchping.features.activetimer.ActiveTimerScreen
import jez.stretchping.ui.theme.StretchPingTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            navigationDispatcher.setNavListener { route ->
                when (route) {
                    is Route.Back -> navController.popBackStack()
                    else -> navController.navigate(route.routeId, route.navOptions)
                }
            }

            StretchPingTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Route.ActiveTimer.routeId
                    ) {
                        composable(Route.ActiveTimer.routeId) {
                            ActiveTimerScreen(hiltViewModel())
                        }
                    }
                }
            }
        }
    }
}

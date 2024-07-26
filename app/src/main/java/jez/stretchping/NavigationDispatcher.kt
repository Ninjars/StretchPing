package jez.stretchping

import androidx.navigation.NavOptionsBuilder
import dagger.hilt.android.scopes.ActivityRetainedScoped
import jez.stretchping.persistence.ExerciseConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

sealed class Route {
    open val routeId: String by lazy { this.javaClass.simpleName }
    open val navOptions: NavOptionsBuilder.() -> Unit = { }

    data object Back : Route()
    data object EditTimer : Route()
    data object PlansList : Route()
    data class ActiveTimer(
        val config: ExerciseConfig,
    ) : Route() {
        override val routeId by lazy {
            "${ActiveTimer::class.simpleName}/${
                Json.encodeToString(config)
            }"
        }

        companion object {
            val baseRouteId by lazy { "${ActiveTimer::class.simpleName}/{$routeConfig}" }
            const val routeConfig = "config"
        }
    }

    data class Planner(
        val planId: String
    ) : Route() {
        override val routeId by lazy { "${Planner::class.java}/$planId" }

        companion object {
            val baseRouteId by lazy { "${Planner::class.java}/{$routePlanId}" }
            const val routePlanId = "id"
        }
    }
}

typealias NavListener = (Route) -> Unit

@ActivityRetainedScoped
class NavigationDispatcher @Inject constructor() {
    private var listener: NavListener? = null

    fun navigateTo(route: Route) {
        listener?.invoke(route)
    }

    fun setNavListener(listener: NavListener) {
        this.listener = listener
    }
}

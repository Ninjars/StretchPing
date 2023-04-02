package jez.stretchping

import androidx.navigation.NavOptionsBuilder
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

sealed class Route {
    open val routeId: String by lazy { this.javaClass.simpleName }
    open val navOptions: NavOptionsBuilder.() -> Unit = { }

    object Back : Route()
    object ActiveTimer : Route()
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

package jez.stretchping.features.planner

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.function.Consumer
import javax.inject.Inject

sealed class PlannerUIEvent {

}

@HiltViewModel
class PlannerVM @Inject constructor(
) : Consumer<PlannerUIEvent>, ViewModel() {
    override fun accept(t: PlannerUIEvent) {
        TODO("Not yet implemented")
    }

}

package jez.stretchping.features.planner

data class PlannerViewState(
    val isInitialised: Boolean,
    val planName: String,
    val repeat: Boolean,
    val canStart: Boolean,
    val sections: List<Section>,
) {
    data class Section(
        val id: String,
        val name: String,
        val repCount: String,
        val entryTransitionDuration: String,
        val repDuration: String,
        val repTransitionDuration: String,
    )
}

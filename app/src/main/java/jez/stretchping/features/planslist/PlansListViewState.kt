package jez.stretchping.features.planslist

data class PlansListViewState(
    val plans: List<Plan>,
) {
    data class Plan(
        val id: String,
        val name: String,
        val isLooping: Boolean,
    )
}

package jez.stretchping.features.edittimer
data class EditTimerViewState(
    val activeDuration: String,
    val transitionDuration: String,
    val repCount: String,
    val themeState: ThemeState,
    val activePings: Int,
    val transitionPings: Int,
    val canStart: Boolean,
    val playInBackground: Boolean,
) {
    data class ThemeState(
        val optionStringResources: List<Int>,
        val selectedIndex: Int,
    )
}

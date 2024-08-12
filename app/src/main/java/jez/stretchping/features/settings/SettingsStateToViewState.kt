package jez.stretchping.features.settings

import jez.stretchping.BuildConfig
import jez.stretchping.R
import jez.stretchping.persistence.NavLabelDisplayMode
import jez.stretchping.persistence.ThemeMode

object SettingsStateToViewState : (SettingsVM.State) -> SettingsViewState {
    private val versionCode: String = BuildConfig.VERSION_CODE.toString()
    private val versionName: String = BuildConfig.VERSION_NAME

    override fun invoke(state: SettingsVM.State): SettingsViewState =
        SettingsViewState(
            transitionPings = state.transitionPings,
            activePings = state.activePings,
            playInBackground = state.playInBackground,
            themeState = with(ThemeMode.displayValues) {
                SettingsViewState.TriOptionsState(
                    optionStringResources = this.map { it.toStringResId() },
                    selectedIndex = this.indexOf(state.themeMode),
                )
            },
            navLabelsState = with(NavLabelDisplayMode.displayValues) {
                SettingsViewState.TriOptionsState(
                    optionStringResources = map { it.toStringResId() },
                    selectedIndex = indexOf(state.showNavLabels)
                )
            },
            versionInfo = "- $versionName ($versionCode) -",
        )
}

private fun NavLabelDisplayMode.toStringResId() =
    when (this) {
        NavLabelDisplayMode.Unset -> -1
        NavLabelDisplayMode.Always -> R.string.nav_label_always
        NavLabelDisplayMode.Selected -> R.string.nav_label_selected
        NavLabelDisplayMode.Never -> R.string.nav_label_never
    }

private fun ThemeMode.toStringResId() =
    when (this) {
        ThemeMode.Unset -> -1
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    }

package jez.stretchping.features.activetimer

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseConfig(
    val repCount: Int,
    val activityDuration: Int,
    val transitionDuration: Int,
    val activePingsCount: Int,
    val transitionPingsCount: Int,
    val playInBackground: Boolean,
)

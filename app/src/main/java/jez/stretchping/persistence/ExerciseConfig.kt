package jez.stretchping.persistence

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseConfig(
    val exerciseId: String,
    val exerciseName: String,
    val sections: List<Section>,
    val repeat: Boolean,
) {
    @Serializable
    data class Section(
        val name: String,
        val repCount: Int,
        val introDuration: Int,
        val activityDuration: Int,
        val transitionDuration: Int,
    )
}

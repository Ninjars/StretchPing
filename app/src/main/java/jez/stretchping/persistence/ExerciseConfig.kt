package jez.stretchping.persistence

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseConfigs(
    val exercises: List<ExerciseConfig>
)

@Serializable
data class ExerciseConfig(
    val exerciseId: String,
    val exerciseName: String,
    val repeat: Boolean,
    val sections: List<SectionConfig>,
) {
    @Serializable
    data class SectionConfig(
        val sectionId: String,
        val name: String,
        val repCount: Int,
        val introDuration: Int,
        val activityDuration: Int,
        val transitionDuration: Int,
    )
}

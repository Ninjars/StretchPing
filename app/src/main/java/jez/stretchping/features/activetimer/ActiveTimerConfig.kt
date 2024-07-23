package jez.stretchping.features.activetimer

import jez.stretchping.persistence.EngineSettings
import jez.stretchping.persistence.ExerciseConfig
import kotlinx.serialization.Serializable

@Serializable
data class ActiveTimerConfig(
    val engineSettings: EngineSettings,
    val exerciseConfig: ExerciseConfig,
)

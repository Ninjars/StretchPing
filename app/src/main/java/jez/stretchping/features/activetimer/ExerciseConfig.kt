package jez.stretchping.features.activetimer

import jez.stretchping.persistence.EngineSettings
import jez.stretchping.persistence.TimerConfig
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseConfig(
    val engineSettings: EngineSettings,
    val timerConfig: TimerConfig,
)

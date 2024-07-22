package jez.stretchping.persistence

import kotlinx.serialization.Serializable

@Serializable
class TimerConfig(
    val repCount: Int,
    val activityDuration: Int,
    val transitionDuration: Int,
)

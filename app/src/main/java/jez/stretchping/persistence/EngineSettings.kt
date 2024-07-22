package jez.stretchping.persistence

import kotlinx.serialization.Serializable

@Serializable
data class EngineSettings(
    val activePingsCount: Int,
    val transitionPingsCount: Int,
    val playInBackground: Boolean,
)

package com.programmersbox.koogintegration.integrator

import kotlinx.serialization.Serializable

abstract class KoogIntegrator {
    abstract suspend fun map(): String

    companion object {
        @Serializable
        data object FavoritesAnalyzer
    }
}
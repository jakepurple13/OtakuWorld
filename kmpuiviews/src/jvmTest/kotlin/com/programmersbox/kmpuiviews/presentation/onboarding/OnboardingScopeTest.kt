package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingScopeTest {
    private fun spec(id: String) = OnboardingStepSpec(
        id = id,
        title = id,
        body = "",
        icon = Icons.Default.Check,
        shape = HeroShape.Hidden,
    )

    @Test
    fun steps_keep_registration_order() {
        val scope = buildOnboardingScope {
            step(spec("a")) {}
            step(spec("b")) {}
            step(spec("c")) {}
        }

        assertEquals(3, scope.size)
        assertEquals(listOf("a", "b", "c"), List(scope.size) { scope[it].spec.id })
    }

    @Test
    fun app_contribution_lands_before_sync() {
        val onboardingSettings: OnboardingScope.() -> Unit = { step(spec("app")) {} }

        val scope = buildOnboardingScope {
            step(spec("sources")) {}
            apply(onboardingSettings)
            step(spec("sync")) {}
        }

        assertEquals(listOf("sources", "app", "sync"), List(scope.size) { scope[it].spec.id })
    }
}

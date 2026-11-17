package com.programmersbox.kmpuiviews.testing

import com.programmersbox.kmpuiviews.domain.aitext.AiEnginePreference
import com.programmersbox.kmpuiviews.domain.aitext.AiSettings
import com.programmersbox.kmpuiviews.domain.aitext.AiToggle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** [AiSettings] held in memory, for tests. */
class InMemoryAiSettings(
    enabled: Boolean = true,
    preference: AiEnginePreference = AiEnginePreference.Automatic,
    disabled: Set<AiToggle> = emptySet(),
) : AiSettings {
    override val enabled = MutableStateFlow(enabled)
    override val preference = MutableStateFlow(preference)
    override val disabled = MutableStateFlow(disabled)

    override suspend fun setEnabled(v: Boolean) {
        enabled.value = v
    }

    override suspend fun setPreference(p: AiEnginePreference) {
        preference.value = p
    }

    override suspend fun setFeatureEnabled(t: AiToggle, on: Boolean) {
        disabled.update { if (on) it - t else it + t }
    }
}

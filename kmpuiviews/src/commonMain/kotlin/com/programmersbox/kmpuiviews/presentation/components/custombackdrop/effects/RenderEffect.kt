package com.programmersbox.kmpuiviews.presentation.components.custombackdrop.effects

import androidx.compose.ui.graphics.RenderEffect
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.BackdropEffectScope
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.BackdropRuntimeShader
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.internal.RuntimeShaderEffect
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.internal.chain
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.isRenderEffectSupported
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.isRuntimeShaderSupported
import org.intellij.lang.annotations.Language
import kotlin.contracts.ExperimentalContracts

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (!isRenderEffectSupported()) return

    renderEffect = renderEffect.chain(effect)
}

@OptIn(ExperimentalContracts::class)
fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    @Language("AGSL") shaderString: String,
    uniformShaderName: String,
    block: BackdropRuntimeShader.() -> Unit,
) {
    if (!isRuntimeShaderSupported()) return

    val effect =
        RuntimeShaderEffect(
            runtimeShader = obtainRuntimeShader(key, shaderString).apply(block),
            uniformShaderName = uniformShaderName
        )
    renderEffect = renderEffect.chain(effect)
}
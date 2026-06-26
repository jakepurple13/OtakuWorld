package com.programmersbox.kmpuiviews.presentation.components.custombackdrop.internal

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RenderEffect
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.BackdropRuntimeShader

internal expect fun RenderEffect?.chain(other: RenderEffect): RenderEffect

internal expect fun RuntimeShaderEffect(
    runtimeShader: BackdropRuntimeShader,
    uniformShaderName: String,
): RenderEffect

internal expect fun ColorFilterEffect(
    renderEffect: RenderEffect? = null,
    colorFilter: ColorFilter,
): RenderEffect
package com.programmersbox.kmpuiviews.presentation.components.custombackdrop.internal

import androidx.compose.ui.graphics.Paint
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.BackdropRuntimeShader

internal expect fun Paint.blur(radius: Float)

internal expect fun Paint.setRuntimeShader(runtimeShader: BackdropRuntimeShader?)
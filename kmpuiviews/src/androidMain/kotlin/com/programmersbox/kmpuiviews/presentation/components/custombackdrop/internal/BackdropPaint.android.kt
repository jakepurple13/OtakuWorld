package com.programmersbox.kmpuiviews.presentation.components.custombackdrop.internal

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativePaint
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.BackdropRuntimeShader
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.asAndroidRuntimeShader

internal actual fun Paint.blur(radius: Float) {
    this.nativePaint.maskFilter =
        if (radius > 0f) BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        else null
}

internal actual fun Paint.setRuntimeShader(runtimeShader: BackdropRuntimeShader?) {
    this.nativePaint.shader = runtimeShader?.asAndroidRuntimeShader()
}
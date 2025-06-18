package com.programmersbox.uiviews.utils

import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.core.performance.DefaultDevicePerformance

@ConsistentCopyVisibility
@Immutable
data class PerformanceClass private constructor(
    val canBlur: Boolean,
    val mediaPerformanceClass: Int = DefaultDevicePerformance().mediaPerformanceClass,
) {

    companion object {
        fun create(): PerformanceClass {
            val devicePerformance = DefaultDevicePerformance().mediaPerformanceClass

            return PerformanceClass(
                canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                mediaPerformanceClass = devicePerformance
            )
        }

        fun canBlur() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}
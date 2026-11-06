package com.programmersbox.sharedcomponents.notifier

import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.jvm.JvmInline

enum class BannerKind { Success, Info, Error, Progress }

enum class PauseReason { Expanded, Background }

@JvmInline
value class BannerId(val value: Long)

data class Banner(
    val id: BannerId,
    val kind: BannerKind,
    val title: String,
    val subtitle: String?,
    val icon: ImageVector?,
    val action: String?,
    val key: String?,
    val fraction: Float?,
    val durationMillis: Long?,
    val onAction: suspend () -> Unit,
)

object BannerDefaults {
    const val SuccessMillis = 4_000L
    const val InfoMillis = 5_000L
    const val WithActionMillis = 6_000L
    const val ErrorMillis = 7_000L
    const val MaxBanners = 20
    const val MaxVisible = 3
}

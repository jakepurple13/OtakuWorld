package com.programmersbox.uiviews.utils

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.tooling.preview.Wallpapers.BLUE_DOMINATED_EXAMPLE
import androidx.compose.ui.tooling.preview.Wallpapers.GREEN_DOMINATED_EXAMPLE
import androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE
import androidx.compose.ui.tooling.preview.Wallpapers.YELLOW_DOMINATED_EXAMPLE

@PreviewLightDark
@Preview(name = "Red", wallpaper = RED_DOMINATED_EXAMPLE, group = "dynamic_colors")
@Preview(name = "Blue", wallpaper = BLUE_DOMINATED_EXAMPLE, group = "dynamic_colors")
@Preview(name = "Green", wallpaper = GREEN_DOMINATED_EXAMPLE, group = "dynamic_colors")
@Preview(name = "Yellow", wallpaper = YELLOW_DOMINATED_EXAMPLE, group = "dynamic_colors")
@Preview(name = "Red Dark", wallpaper = RED_DOMINATED_EXAMPLE, uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL, group = "dynamic_colors")
@Preview(name = "Blue Dark", wallpaper = BLUE_DOMINATED_EXAMPLE, uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL, group = "dynamic_colors")
@Preview(name = "Green Dark", wallpaper = GREEN_DOMINATED_EXAMPLE, uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL, group = "dynamic_colors")
@Preview(name = "Yellow Dark", wallpaper = YELLOW_DOMINATED_EXAMPLE, uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL, group = "dynamic_colors")
@PreviewScreenSizes
annotation class PreviewThemeColorsSizes
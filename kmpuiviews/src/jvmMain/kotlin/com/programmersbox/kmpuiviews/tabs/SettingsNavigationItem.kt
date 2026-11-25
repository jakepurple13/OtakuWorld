package com.programmersbox.kmpuiviews.tabs

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupScope
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.LocalNavActions

/**
 * A settings entry (from `composeCustomPreferences`) that opens [key] in the tab it is shown in. The Koin
 * [NavigationActions] is the Home tab's, so navigating with it would jump from the Settings tab to Home.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun CategoryGroupScope.navigationItem(key: NavKey, icon: ImageVector, title: String) {
    // The click handler is not composable: the row's content hands it the tab's LocalNavActions.
    val holder = object {
        var nav: NavigationActions? = null
    }
    segmentedListItem(
        onClick = { holder.nav?.navigate(key) },
        leadingContent = { Icon(icon, null) },
        content = {
            val nav = LocalNavActions.current
            SideEffect { holder.nav = nav }
            Text(title)
        },
    )
}

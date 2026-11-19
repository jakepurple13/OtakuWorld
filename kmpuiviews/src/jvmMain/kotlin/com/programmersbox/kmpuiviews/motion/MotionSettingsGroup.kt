package com.programmersbox.kmpuiviews.motion

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.getKoin
import org.koin.compose.koinInject

/** "Motion" header + one row that opens [MotionSettingsRoute]. Adds nothing when no [DesktopMotionCatalog] is in Koin. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.motionSettingsGroup() {
    item(contentType = "motionHeader") {
        if (getKoin().getOrNull<DesktopMotionCatalog>() == null) return@item
        Text(
            "Motion",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .animateItem()
                .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
        )
    }
    item(contentType = "motionRow") {
        val catalog = getKoin().getOrNull<DesktopMotionCatalog>() ?: return@item
        val settings = koinInject<MangaDesktopSettings>()
        val launchKey by settings.launchAnimation.asState()
        val quitKey by settings.quitAnimation.asState()
        val navActions = LocalNavActions.current
        val launch = catalog.resolve(MotionPhase.Launch, launchKey)
        val quit = catalog.resolve(MotionPhase.Quit, quitKey)
        SegmentedListItem(
            content = { Text("Launch & quit animations") },
            supportingContent = { Text("Launch: ${launch.title} · Quit: ${quit.title}") },
            leadingContent = { Icon(Icons.Default.Animation, null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            onClick = { navActions.navigate(MotionSettingsRoute) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            modifier = Modifier.animateItem(),
        )
    }
}

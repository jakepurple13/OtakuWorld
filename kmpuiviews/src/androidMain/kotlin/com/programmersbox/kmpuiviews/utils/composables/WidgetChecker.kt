package com.programmersbox.kmpuiviews.utils.composables

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.widget.notification.TextOnlyWidgetContentPreview
import com.programmersbox.kmpuiviews.widget.notification.WidgetDataState
import com.programmersbox.kmpuiviews.widget.notification.getWidgetStateFlow
import org.koin.compose.koinInject

@Composable
fun <T> widgetChecker(
    clazz: Class<T>,
): State<WidgetCheckerState> {
    val context = LocalContext.current

    val appWidgetManager = remember(context) {
        AppWidgetManager.getInstance(context)
    }

    val appUsageWidget = remember(context) {
        ComponentName(context, clazz)
    }

    val hasActiveWidgetIds = remember { mutableStateOf(true) }

    LifecycleResumeEffect(Unit) {
        runCatching {
            hasActiveWidgetIds.value = appWidgetManager
                .getAppWidgetIds(appUsageWidget)
                .isNotEmpty()
        }
        onPauseOrDispose {}
    }

    return remember(hasActiveWidgetIds, context) {
        derivedStateOf {
            WidgetCheckerState(
                appWidgetManager = appWidgetManager,
                componentName = appUsageWidget,
                hasActiveWidgetIds = hasActiveWidgetIds.value
            )
        }
    }
}

data class WidgetCheckerState(
    val appWidgetManager: AppWidgetManager,
    val componentName: ComponentName,
    val hasActiveWidgetIds: Boolean,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WidgetAddCard(
    widgetCheckerState: WidgetCheckerState,
    description: String,
    modifier: Modifier = Modifier,
) {
    val itemDao = koinInject<ItemDao>()
    val widgetState by getWidgetStateFlow(itemDao).collectAsStateWithLifecycle(WidgetDataState())
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.animateContentSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Add as widget",
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextOnlyWidgetContentPreview(
                state = widgetState,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .height(140.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    widgetCheckerState
                        .appWidgetManager
                        .requestPinAppWidget(
                            widgetCheckerState.componentName,
                            null,
                            null
                        )
                },
                shapes = ButtonDefaults.shapes()
            ) { Text(text = "Add Widget") }
        }
    }
}

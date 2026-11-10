package com.programmersbox.kmpuiviews.presentation.shortcuts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.quickactions.QuickActionsController
import com.programmersbox.kmpuiviews.presentation.quickactions.isMacOs
import com.programmersbox.kmpuiviews.utils.AppConfig
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.shortcuts_anywhere
import otakuworld.kmpuiviews.generated.resources.shortcuts_close
import otakuworld.kmpuiviews.generated.resources.shortcuts_close_action
import otakuworld.kmpuiviews.generated.resources.shortcuts_global_search
import otakuworld.kmpuiviews.generated.resources.shortcuts_in_quick_actions
import otakuworld.kmpuiviews.generated.resources.shortcuts_list
import otakuworld.kmpuiviews.generated.resources.shortcuts_move
import otakuworld.kmpuiviews.generated.resources.shortcuts_quick_actions
import otakuworld.kmpuiviews.generated.resources.shortcuts_run
import otakuworld.kmpuiviews.generated.resources.shortcuts_subtitle
import otakuworld.kmpuiviews.generated.resources.shortcuts_title

/** Hosts [ShortcutsDialog] driven by [QuickActionsController.shortcutsOpen]; app sections come from Koin. */
@Composable
internal fun ShortcutsDialogHost(controller: QuickActionsController) {
    val open by controller.shortcutsOpen.collectAsState()
    val appSections = getKoin().getAll<ShortcutSection>()
    val builtIn = builtInShortcutSections(
        isMac = isMacOs,
        strings = BuiltInShortcutStrings(
            anywhere = stringResource(Res.string.shortcuts_anywhere),
            inQuickActions = stringResource(Res.string.shortcuts_in_quick_actions),
            quickActions = stringResource(Res.string.shortcuts_quick_actions),
            globalSearch = stringResource(Res.string.shortcuts_global_search),
            shortcutsList = stringResource(Res.string.shortcuts_list),
            move = stringResource(Res.string.shortcuts_move),
            run = stringResource(Res.string.shortcuts_run),
            close = stringResource(Res.string.shortcuts_close_action),
        ),
    )
    AnimatedVisibility(visible = open, enter = fadeIn(), exit = fadeOut()) {
        ShortcutsDialog(
            appName = koinInject<AppConfig>().appName,
            sections = builtIn + appSections,
            onDismiss = controller::closeShortcuts,
        )
    }
}

@Composable
fun ShortcutsDialog(
    appName: String,
    sections: List<ShortcutSection>,
    onDismiss: () -> Unit,
) {
    // Takes focus so keys stop reaching the screen behind (e.g. Space in the player);
    // Esc is still handled by the window.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
    ) {
        AnimatedVisibility(
            visibleState = remember { MutableTransitionState(false).apply { targetState = true } },
            enter = scaleIn(initialScale = 0.94f) + fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 440.dp)
                    .heightIn(max = 560.dp)
                    // Swallow clicks so only the scrim dismisses.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.shortcuts_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(Res.string.shortcuts_subtitle, appName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    sections.forEach { section ->
                        Text(
                            text = section.title.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                        )
                        section.entries.forEach { entry ->
                            ShortcutRow(entry)
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 16.dp),
                    ) {
                        Text(stringResource(Res.string.shortcuts_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(entry: ShortcutEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = entry.listLabel ?: entry.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            entry.keys.forEach { KeyCap(it) }
        }
    }
}

@Composable
private fun KeyCap(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

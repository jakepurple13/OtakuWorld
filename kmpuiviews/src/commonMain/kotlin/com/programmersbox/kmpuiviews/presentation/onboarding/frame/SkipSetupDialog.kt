package com.programmersbox.kmpuiviews.presentation.onboarding.frame

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags

@Composable
fun SkipSetupDialog(
    appName: String,
    onKeepGoing: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepGoing,
        icon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
        title = { Text("Skip setup?") },
        text = {
            Text("$appName will start with default settings. You can run setup again from Settings › About › View Onboarding Again.")
        },
        dismissButton = {
            TextButton(onClick = onKeepGoing, modifier = Modifier.testTag(OnboardingTags.SkipCancel)) {
                Text("Keep going")
            }
        },
        confirmButton = {
            TextButton(onClick = onSkip, modifier = Modifier.testTag(OnboardingTags.SkipConfirm)) {
                Text("Skip")
            }
        },
    )
}

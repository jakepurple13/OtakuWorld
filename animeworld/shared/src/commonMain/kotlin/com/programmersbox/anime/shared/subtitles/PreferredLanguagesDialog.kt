package com.programmersbox.anime.shared.subtitles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Language code to display name, in the order the dialog lists them. */
internal val subtitleLanguages: List<Pair<String, String>> = listOf(
    "en" to "English",
    "ja" to "Japanese",
    "es" to "Spanish",
    "pt" to "Portuguese",
    "fr" to "French",
    "de" to "German",
    "it" to "Italian",
    "ar" to "Arabic",
    "ru" to "Russian",
    "ko" to "Korean",
    "zh" to "Chinese",
)

internal fun languageName(code: String): String =
    subtitleLanguages.firstOrNull { it.first == code }?.second ?: code

/** "English, then Japanese", or "Device language" when nothing is chosen. */
internal fun preferredLanguagesSummary(codes: List<String>): String =
    if (codes.isEmpty()) "Device language" else codes.joinToString(", then ") { languageName(it) }

/** Checkable list of languages; checked ones can be reordered and are returned in priority order. */
@Composable
fun PreferredLanguagesDialog(
    selected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember { mutableStateOf(selected) }
    val unchosen = subtitleLanguages.map { it.first }.filter { it !in chosen }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preferred language") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                chosen.forEachIndexed { index, code ->
                    LanguageRow(
                        name = languageName(code),
                        checked = true,
                        onCheckedChange = { chosen = chosen - code },
                        onMoveUp = if (index > 0) {
                            { chosen = chosen.swap(index, index - 1) }
                        } else null,
                        onMoveDown = if (index < chosen.lastIndex) {
                            { chosen = chosen.swap(index, index + 1) }
                        } else null,
                    )
                }
                unchosen.forEach { code ->
                    LanguageRow(
                        name = languageName(code),
                        checked = false,
                        onCheckedChange = { chosen = chosen + code },
                        onMoveUp = null,
                        onMoveDown = null,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(chosen) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LanguageRow(
    name: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (checked) {
            IconButton(onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move $name up")
            }
            IconButton(onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move $name down")
            }
        }
    }
}

private fun <T> List<T>.swap(a: Int, b: Int): List<T> = toMutableList().also {
    val tmp = it[a]
    it[a] = it[b]
    it[b] = tmp
}

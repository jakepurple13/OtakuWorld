package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.ExceptionItem
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.kmpuiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DateTimeFormat
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExceptionsScreen(
    viewModel: ExceptionViewModel = koinViewModel(),
) {
    HideNavBarWhileOnScreen()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val exceptions by viewModel.exceptions.collectAsStateWithLifecycle(emptyList())

    val dateTimeFormat = LocalSystemDateTimeFormat.current

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete All?") },
            text = { Text("Are you sure you want to delete all exceptions?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAll()
                        showDialog = false
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) { Text("Cancel") }
            }
        )
    }

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exceptions") },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(
                        onClick = { showDialog = true }
                    ) { Icon(Icons.Default.DeleteSweep, null) }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { p ->
        LazyColumn(
            contentPadding = p,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(exceptions) {
                ExceptionItem(
                    exceptionItem = it,
                    dateTimeFormat = dateTimeFormat,
                    onDeleteItem = { viewModel.deleteItem(it) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun ExceptionItem(
    exceptionItem: ExceptionItem,
    dateTimeFormat: DateTimeFormat<LocalDateTime>,
    onDeleteItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEverything by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete?") },
            text = { Text("Are you sure you want to delete this exception?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteItem()
                        showDialog = false
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) { Text("Cancel") }
            }
        )
    }

    OutlinedCard(
        onClick = { showEverything = !showEverything },
        modifier = modifier.animateContentSize()
    ) {
        SelectionContainer {
            Text(
                exceptionItem.message,
                maxLines = if (showEverything) Int.MAX_VALUE else 3,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.background,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp)
            )
        }

        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Text(remember { dateTimeFormat.format(exceptionItem.time.toLocalDateTime()) })

            ElevatedButton(
                onClick = { showDialog = true }
            ) { Text("Delete") }
        }
    }
}
package com.programmersbox.kmpuiviews.presentation.settings.workerinfo

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.utils.LocalSystemDateTimeFormat
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DateTimeFormat
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerInfoScreen(
    viewModel: WorkerInfoViewModel = koinViewModel(),
) {
    val dateFormat = LocalSystemDateTimeFormat.current

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Worker Info") },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    "This screen is to view the background workers. Mostly for debugging purposes, but can be helpful to anyone.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            viewModel.workers
                .groupBy { it.workerName }
                .forEach {
                    item {
                        CategoryGroup {
                            it.value.forEach {
                                item {
                                    WorkerItem(
                                        workerInfoModel = it,
                                        dateFormat = dateFormat,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun WorkerItem(
    workerInfoModel: WorkerInfoModel,
    dateFormat: DateTimeFormat<LocalDateTime>,
    modifier: Modifier = Modifier,
) {
    ListItem(
        overlineContent = {
            if (workerInfoModel.isPeriodic) {
                Text("Next Run: " + dateFormat.format(workerInfoModel.nextScheduleTimeMillis))
            }
        },
        headlineContent = { Text(workerInfoModel.tags.joinToString(", ")) },
        supportingContent = {
            Column(
                modifier = Modifier.animateContentSize()
            ) {
                Text(workerInfoModel.status)
                workerInfoModel.progress.forEach { (t, u) -> Text("$t: $u") }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = modifier
    )
}


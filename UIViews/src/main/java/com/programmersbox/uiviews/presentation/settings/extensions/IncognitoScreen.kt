package com.programmersbox.uiviews.presentation.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.uiviews.presentation.components.OtakuScaffold
import com.programmersbox.uiviews.utils.BackButton
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncognitoScreen(
    viewModel: IncognitoViewModel = koinViewModel(),
) {
    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incognito Sources") },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(viewModel.incognitoModels) {
                Card(
                    onClick = { viewModel.toggleIncognito(it.sourceInformation, !it.incognitoSource.isIncognito) }
                ) {
                    ListItem(
                        overlineContent = { Text(it.sourceInformation.packageName) },
                        headlineContent = { Text(it.sourceInformation.name) },
                        trailingContent = {
                            Switch(
                                checked = it.incognitoSource.isIncognito,
                                onCheckedChange = { value -> viewModel.toggleIncognito(it.sourceInformation, value) }
                            )
                        }
                    )
                }
            }
        }
    }
}
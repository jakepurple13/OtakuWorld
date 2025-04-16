package com.programmersbox.uiviews.presentation.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.programmersbox.uiviews.presentation.components.OtakuScaffold
import com.programmersbox.uiviews.utils.BackButton
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IncognitoScreen(
    viewModel: IncognitoViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incognito Sources") },
                subtitle = { Text("Choose what sources you don't want to have recorded or saved") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListItem(
                    headlineContent = {
                        Text("This affects favoriting, marking a chapter as read, and adding to history.")
                    }
                )
            }

            items(viewModel.incognitoModels) {
                Card(
                    onClick = { viewModel.toggleIncognito(it.sourceInformation, !it.incognitoSource.isIncognito) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    ),
                ) {
                    ListItem(
                        overlineContent = { Text(it.sourceInformation.packageName) },
                        headlineContent = { Text(it.sourceInformation.name) },
                        trailingContent = {
                            Switch(
                                checked = it.incognitoSource.isIncognito,
                                onCheckedChange = { value -> viewModel.toggleIncognito(it.sourceInformation, value) }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
package com.programmersbox.otakuworld.settings

import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.SyncRequest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.programmersbox.otakuworld.BuildConfig
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding)
        ) {
            SetupSyncButton(
                title = "Anime World",
                item = viewModel.animeWorld
            )
            SetupSyncButton(
                title = "Manga World",
                item = viewModel.mangaWorld
            )
            SetupSyncButton(
                title = "Novel World",
                item = viewModel.novelWorld
            )
        }
    }
}

@Composable
private fun SetupSyncButton(
    title: String,
    item: OtakuSettingsItem,
) {
    val context = LocalContext.current

    Button(
        onClick = {
            AccountManager.get(context)
                .getAccountsByType(BuildConfig.ACCOUNT_TYPE)
                .forEach { account ->
                    println(account)
                    ContentResolver.setIsSyncable(
                        account,
                        item.favoritesUri,
                        1
                    )
                    ContentResolver.setIsSyncable(
                        account,
                        item.listsUri,
                        1
                    )
                    ContentResolver.setSyncAutomatically(
                        account,
                        item.favoritesUri,
                        true
                    )
                    ContentResolver.requestSync(
                        SyncRequest.Builder()
                            .setDisallowMetered(true)
                            .setSyncAdapter(
                                account,
                                item.favoritesUri
                            )
                            .setExtras(
                                bundleOf(
                                    "type" to item.app.name
                                )
                            )
                            .syncPeriodic(
                                1.days.inWholeSeconds,
                                1.hours.inWholeSeconds
                            )
                            .build()
                    )

                    ContentResolver.setSyncAutomatically(
                        account,
                        item.listsUri,
                        true
                    )
                    ContentResolver.requestSync(
                        SyncRequest.Builder()
                            .setDisallowMetered(true)
                            .setSyncAdapter(
                                account,
                                item.listsUri
                            )
                            .setExtras(
                                bundleOf(
                                    "type" to item.app.name
                                )
                            )
                            .syncPeriodic(
                                1.days.inWholeSeconds,
                                1.hours.inWholeSeconds
                            )
                            .build()
                    )
                }
        },
    ) { Text("Setup Syncs for $title") }
}
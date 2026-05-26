package com.programmersbox.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.awt.Desktop
import java.io.File

@Serializable
data object PlatformSettings : NavKey

private const val PLATFORM_SETTINGS_COUNT = 3

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JvmSettingsScreen() {
    val settings = koinInject<MangaDesktopSettings>()
    val appDirs = koinInject<AppDirs>()

    val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Desktop Settings") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item(contentType = "downloadPath") {
                var downloadPath by settings
                    .extensionDirectory
                    .asState()

                val directoryPicker = rememberDirectoryPickerLauncher(
                    directory = PlatformFile(downloadPath)
                ) { file -> file?.let { downloadPath = it.absolutePath() } }

                SegmentedListItem(
                    content = { Text("Download Path") },
                    supportingContent = { Text(downloadPath) },
                    leadingContent = { Icon(Icons.Default.Download, null) },
                    onClick = { directoryPicker.launch() },
                    colors = colors,
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = PLATFORM_SETTINGS_COUNT)
                )
            }

            item(contentType = "downloadChapterPath") {
                var downloadPath by settings
                    .downloadsDirectory
                    .asState()

                val directoryPicker = rememberDirectoryPickerLauncher(
                    directory = PlatformFile(downloadPath)
                ) { file -> file?.let { downloadPath = it.absolutePath() } }

                SegmentedListItem(
                    content = { Text("Download Chapter Path") },
                    supportingContent = { Text(downloadPath) },
                    leadingContent = { Icon(Icons.Default.Download, null) },
                    onClick = { directoryPicker.launch() },
                    colors = colors,
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = PLATFORM_SETTINGS_COUNT)
                )
            }

            item(contentType = "useWebView") {
                var useWebView by settings
                    .useWebViewForReader
                    .asState()

                SegmentedListItem(
                    content = { Text("Use WebView") },
                    supportingContent = { Text("Use a webview instead of the built in reader") },
                    leadingContent = { Icon(Icons.Default.OpenInBrowser, null) },
                    checked = useWebView,
                    onCheckedChange = { useWebView = !useWebView },
                    colors = colors,
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = PLATFORM_SETTINGS_COUNT)
                )
            }

            item(contentType = "viewFolders") {
                SegmentedListItem(
                    content = { Text("View Data Directory") },
                    supportingContent = { Text("View the directory where the data is stored") },
                    leadingContent = { Icon(Icons.Default.Dataset, null) },
                    onClick = {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(File(appDirs.getUserDataDir()))
                        }
                    },
                    colors = colors,
                    shapes = ListItemDefaults.segmentedShapes(index = 2, count = PLATFORM_SETTINGS_COUNT)
                )
            }
        }
    }
}
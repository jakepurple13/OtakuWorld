package com.programmersbox.kmpuiviews.presentation.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.BuildKonfig
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.platform
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.DateTimeFormatItem
import com.programmersbox.kmpuiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.kmpuiviews.versionCode
import com.programmersbox.showcase.annotations.ShowcaseComponent
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppInfoScreen() {
    val appConfig: AppConfig = koinInject()
    val appVersion = appVersion()

    SettingsScaffold(
        title = "App Info",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text(appConfig.appName) },
                supportingContent = { Text(appConfig.buildType.name) },
                overlineContent = if (appConfig.isDebug) {
                    {
                        Text("Debug")
                    }
                } else null,
                leadingContent = {
                    Image(
                        painterLogo(),
                        null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                },
                onClick = { },
            )

            segmentedListItem(
                overlineContent = { Text("Version:") },
                content = { Text(appVersion) },
                onClick = { },
            )

            segmentedListItem(
                overlineContent = { Text("Platform:") },
                content = { Text(platform()) },
                onClick = { },
            )

            segmentedListItem(
                overlineContent = { Text("Version code:") },
                content = { Text(versionCode()) },
                onClick = { },
            )

            segmentedListItem(
                overlineContent = { Text("GIT SHA:") },
                content = { Text(BuildKonfig.COMMIT_SHA) },
                onClick = { },
            )

            segmentedListItem(
                overlineContent = { Text("Build Time:") },
                content = {
                    val formatter = LocalSystemDateTimeFormat.current
                    val format = remember(formatter) {
                        runCatching {
                            formatter.format(
                                Instant.parse(BuildKonfig.BUILD_TIME)
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                            )
                        }.getOrDefault(BuildKonfig.BUILD_TIME)
                    }
                    Text(format)
                },
                onClick = { },
            )

            segmentedListItem(
                overlineContent = { Text("Commit Count:") },
                content = { Text(BuildKonfig.COMMIT_COUNT) },
                onClick = { },
            )
        }
    }
}

@ShowcaseComponent(
    name = "Category Group",
    description = "Shows a category group implementation",
    group = "Settings"
)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppInfoSample() {
    CategoryGroupListItem {
        segmentedListItem(
            overlineContent = { Text("Version:") },
            content = { Text(BuildKonfig.VERSION_NAME_KMP) },
            onClick = { },
        )

        segmentedListItem(
            overlineContent = { Text("Platform:") },
            content = { Text(platform()) },
            onClick = { },
        )

        segmentedListItem(
            overlineContent = { Text("Version code:") },
            content = { Text(versionCode()) },
            onClick = { },
        )

        segmentedListItem(
            overlineContent = { Text("GIT SHA:") },
            content = { Text(BuildKonfig.COMMIT_SHA) },
            onClick = { },
        )

        segmentedListItem(
            overlineContent = { Text("Build Time:") },
            content = {
                val formatter = DateTimeFormatItem(isUsing24HourTime = true)
                val format = remember(formatter) {
                    runCatching {
                        formatter.format(
                            Instant.parse(BuildKonfig.BUILD_TIME)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                        )
                    }.getOrDefault(BuildKonfig.BUILD_TIME)
                }
                Text(format)
            },
            onClick = { },
        )

        segmentedListItem(
            overlineContent = { Text("Commit Count:") },
            content = { Text(BuildKonfig.COMMIT_COUNT) },
            onClick = { },
        )
    }
}
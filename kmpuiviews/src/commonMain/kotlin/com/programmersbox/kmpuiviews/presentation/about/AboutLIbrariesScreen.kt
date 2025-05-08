package com.programmersbox.kmpuiviews.presentation.about

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.DialogProperties
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.LibraryPadding
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.programmersbox.kmpuiviews.AboutLibraryBuilder
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@ExperimentalComposeUiApi
@Composable
fun AboutLibrariesScreen() {
    HideNavBarWhileOnScreen()
    val uriHandler = LocalUriHandler.current
    val aboutLibraryBuilder = koinInject<AboutLibraryBuilder>()
    val libraries by aboutLibraryBuilder.buildLibs()

    val libs = libraries?.libraries.orEmpty()

    val topAppBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val appConfig = koinInject<AppConfig>()

    OtakuScaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Libraries Used") },
                navigationIcon = { BackButton() },
                actions = { Text("${libs.size} libraries") },
                scrollBehavior = topAppBarScrollBehavior
            )
        },
    ) { p ->
        OutlinedLibrariesContainer(
            modifier = Modifier.fillMaxSize(),
            contentPadding = p,
            libraries = libraries,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            header = {
                stickyHeader {
                    DefaultHeader(
                        logo = { Image(painterLogo(), null) },
                        version = appVersion(),
                        appName = appConfig.appName
                    )
                }
            },
        ) { library ->
            val openDialog = rememberSaveable { mutableStateOf(false) }

            OutlinedLibrary(
                library = library,
                padding = LibraryDefaults.libraryPadding(
                    badgeContentPadding = PaddingValues(8.dp),
                    badgePadding = PaddingValues(8.dp)
                )
            ) { openDialog.value = true }

            if (openDialog.value) {
                AlertDialog(
                    onDismissRequest = { openDialog.value = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                library.website?.let { uriHandler.openUri(it) }
                                openDialog.value = false
                            }
                        ) { Text("Open In Browser") }
                    },
                    dismissButton = { TextButton(onClick = { openDialog.value = false }) { Text("OK") } },
                    title = { Text(library.name, style = MaterialTheme.typography.titleMedium) },
                    text = { Text(library.website.orEmpty()) },
                    modifier = Modifier.padding(16.dp),
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                )
            }
        }
    }
}

@Composable
private fun OutlinedLibrariesContainer(
    libraries: Libs?,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    itemContentPadding: PaddingValues = padding.contentPadding,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(2.dp),
    header: (LazyListScope.() -> Unit)? = null,
    onLibraryClick: ((Library) -> Unit)? = null,
    customLibraryItem: @Composable (Library) -> Unit = { library ->
        OutlinedLibrary(
            library,
            showAuthor,
            showVersion,
            showLicenseBadges,
            padding,
            itemContentPadding
        ) { onLibraryClick?.invoke(library) }
    },
) {
    LibrariesContainer(
        libraries,
        modifier,
        lazyListState,
        contentPadding,
        showAuthor,
        showVersion,
        showLicenseBadges,
        padding,
        itemContentPadding,
        verticalArrangement,
        header,
        onLibraryClick,
        customLibraryItem
    )
}

@Composable
private fun LibrariesContainer(
    libraries: Libs?,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    itemContentPadding: PaddingValues = padding.contentPadding,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    header: (LazyListScope.() -> Unit)? = null,
    onLibraryClick: ((Library) -> Unit)? = null,
    customLibraryItem: @Composable (Library) -> Unit = { library ->
        Library(
            library,
            showAuthor,
            showVersion,
            showLicenseBadges,
            padding,
            itemContentPadding
        ) { onLibraryClick?.invoke(library) }
    },
) {
    val libs = libraries?.libraries
    if (libs != null) {
        LazyColumn(
            modifier,
            state = lazyListState,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement
        ) {
            header?.invoke(this)
            items(libs) { library -> customLibraryItem(library) }
        }
    }
}

@Composable
internal fun OutlinedLibrary(
    library: Library,
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    contentPadding: PaddingValues = padding.contentPadding,
    onClick: () -> Unit,
) {
    OutlinedCard {
        Library(
            library,
            showAuthor,
            showVersion,
            showLicenseBadges,
            padding,
            contentPadding,
            onClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Library(
    library: Library,
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    contentPadding: PaddingValues = padding.contentPadding,
    onClick: () -> Unit,
) {
    val typography = MaterialTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick.invoke() }
            .padding(contentPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = library.name,
                modifier = Modifier
                    .padding(padding.namePadding)
                    .weight(1f),
                style = typography.titleLarge,
                //color = colors.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val version = library.artifactVersion
            if (version != null && showVersion) {
                Text(
                    version,
                    modifier = Modifier.padding(padding.versionPadding),
                    style = typography.bodyMedium,
                    //color = colors.contentColor,
                    textAlign = TextAlign.Center
                )
            }
        }
        val author = library.author
        if (showAuthor && author.isNotBlank()) {
            Text(
                text = author,
                style = typography.bodyMedium,
                //color = colors.contentColor
            )
        }
        if (showLicenseBadges && library.licenses.isNotEmpty()) {
            Row {
                library.licenses.forEach {
                    Badge(
                        modifier = Modifier.padding(padding.badgePadding),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            modifier = Modifier.padding(padding.badgeContentPadding),
                            text = it.name
                        )
                    }
                }
            }
        }
    }
}

data class Sites(
    val name: String,
    val url: String,
    val onClick: () -> Unit = {},
    val content: @Composable RowScope.() -> Unit = { Text(name) },
)

@Composable
private fun DefaultHeader(
    appName: String?,
    logo: (@Composable () -> Unit)? = null,
    version: String? = null,
    description: String? = null,
    linkSites: List<Sites> = emptyList(),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor = backgroundColor),
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            logo?.invoke()
            appName?.let { Text(it) }
            version?.let { Text(it) }
            if (linkSites.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    linkSites.fastForEach {
                        OutlinedButton(
                            onClick = it.onClick,
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .weight(1f)
                        ) { it.content(this) }
                    }
                }
            }
            description?.let {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp))
                Text(it, textAlign = TextAlign.Center)
            }
        }
    }
}

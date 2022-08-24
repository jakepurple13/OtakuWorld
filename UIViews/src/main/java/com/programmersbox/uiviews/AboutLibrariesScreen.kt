package com.programmersbox.uiviews

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.LibraryPadding
import com.mikepenz.aboutlibraries.ui.compose.data.fakeData
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.util.withContext
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar

@Composable
fun libraryList(librariesBlock: (Context) -> Libs = { context -> Libs.Builder().withContext(context).build() }): State<Libs?> {
    val libraries = remember { mutableStateOf<Libs?>(null) }

    val context = LocalContext.current
    LaunchedEffect(libraries) { libraries.value = librariesBlock(context) }
    return libraries
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@ExperimentalComposeUiApi
@Composable
fun AboutLibrariesScreen(mainLogo: MainLogo) {
    val libraries by libraryList()
    val context = LocalContext.current

    //var searchText by remember { mutableStateOf("") }
    val libs = libraries?.libraries/*?.filter { it.name.contains(searchText, true) }*/.orEmpty()

    val state = rememberTopAppBarState()
    val topAppBarScrollBehavior: TopAppBarScrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(state) }

    Scaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            InsetSmallTopAppBar(
                title = { Text("Libraries Used") },
                navigationIcon = { BackButton() },
                actions = { Text("${libs.size} libraries") },
                scrollBehavior = topAppBarScrollBehavior
            )
        },
        /*bottomBar = {
            BottomAppBar {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search Libraries") },
                    trailingIcon = { IconButton(onClick = { searchText = "" }) { Icon(Icons.Default.Clear, null) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }
        }*/
    ) { p ->
        OutlinedLibrariesContainer(
            modifier = Modifier.fillMaxSize(),
            contentPadding = p,
            libraries = libraries,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            header = {
                stickyHeader {
                    val mainLogoDrawable = remember { AppCompatResources.getDrawable(context, mainLogo.logoId) }

                    DefaultHeader(
                        logo = { Image(rememberDrawablePainter(drawable = mainLogoDrawable), null) },
                        version = context.packageManager.getPackageInfo(context.packageName, 0)?.versionName.orEmpty(),
                    )
                }
            },
        ) { library ->
            val openDialog = rememberSaveable { mutableStateOf(false) }

            OutlinedLibrary(
                library = library
            ) { openDialog.value = true }

            if (openDialog.value) {
                AlertDialog(
                    onDismissRequest = { openDialog.value = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                library.website?.let {
                                    val i = Intent(Intent.ACTION_VIEW)
                                    i.data = Uri.parse(it)
                                    context.startActivity(i)
                                }
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
fun OutlinedLibrariesContainer(
    libraries: Libs?,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    itemContentPadding: PaddingValues = LibraryDefaults.ContentPadding,
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
    }
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
fun LibrariesContainer(
    libraries: Libs?,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    itemContentPadding: PaddingValues = LibraryDefaults.ContentPadding,
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
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OutlinedLibrary(
    library: Library,
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    contentPadding: PaddingValues = LibraryDefaults.ContentPadding,
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
    contentPadding: PaddingValues = LibraryDefaults.ContentPadding,
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
                        //contentColor = colors.badgeContentColor,
                        //containerColor = colors.badgeBackgroundColor
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
    val content: @Composable RowScope.() -> Unit = { Text(name) }
)

@Composable
fun DefaultHeader(
    logo: (@Composable () -> Unit)? = null,
    appName: String? = stringResource(id = R.string.app_name),
    version: String? = null,
    description: String? = null,
    linkSites: List<Sites> = emptyList(),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor = backgroundColor)
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
                Divider(modifier = Modifier.padding(horizontal = 4.dp))
                Text(it, textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
fun DefaultHeaderPreview() {
    Surface {
        Scaffold {
            OutlinedLibrariesContainer(
                libraries = fakeData,
                contentPadding = it,
                modifier = Modifier.fillMaxSize(),
                header = {
                    stickyHeader {
                        DefaultHeader(
                            logo = { Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null) },
                            version = "1.2.3",
                            description = "This is a massive description thasd;" +
                                    "lfjkasd;lkfja;lksdfj;alskdjf;laskdjf;alksjdf;laksdjf;lksjfda",
                            linkSites = listOf(
                                Sites(
                                    name = "Play Store",
                                    url = "asdf",
                                ),
                                Sites(
                                    name = "GitHub",
                                    url = "asdf",
                                )
                            )
                        )
                    }
                }
            )
        }
    }
}
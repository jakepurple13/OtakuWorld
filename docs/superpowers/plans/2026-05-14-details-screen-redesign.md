# DetailsScreen UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (
> recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the DetailsScreen with a cinematic hero layout for mobile and a matching two-pane
layout for larger screens, keeping all dynamic Material theming intact.

**Architecture:** Three targeted file edits — `DetailsHeader.kt` gets a new fixed-height banner with
floating cover art; `DetailsPortrait.kt` removes the `CollapsableColumn` wrapper and scrolls the
header naturally in the `LazyColumn`; `DetailsLandscape.kt` gains parity params and an enhanced
gradient. `DetailsScreen.kt` gets a one-line call-site update to pass the new landscape params. No
new abstractions introduced.

**Tech Stack:** Compose Multiplatform (commonMain), Material3, `Modifier.blur`,
`Brush.verticalGradient`, `ComposableUtils.IMAGE_WIDTH/HEIGHT`

---

### Task 1: Redesign DetailsHeader — cinematic banner + floating cover

**Files:**

- Modify:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsHeader.kt`

The function signature and all imports stay the same. Only the body of the outer `Box` changes.

- [ ] **Step 1: Replace the body of the outer `Box` in `DetailsHeader`**

Find this block in `DetailsHeader`:

```kotlin
Box(
    modifier = modifier
        .fillMaxWidth()
        .animateContentSize()
) {
    ImageLoaderChoice(
        imageUrl = imageUrl,
        name = "",
        headers = model.extras.mapValues { it.value.toString() },
        //placeHolder = { painterLogo() },
        placeHolder = { rememberVectorPainter(Icons.Default.BrokenImage) },
        contentScale = ContentScale.Crop,
        colorFilter = colorFilter,
        modifier = Modifier
            .matchParentSize()
            .composed {
                val brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surface
                    )
                )

                this
                    .blur(4.dp)
                    .drawWithContent {
                        drawContent()
                        drawRect(brush)
                    }
            }
```

Replace the entire `Box { ... }` body (everything between its braces, up to and including the final
`FlowRow` and `possibleDescription()`) with:

```kotlin
Box(
    modifier = modifier
        .fillMaxWidth()
        .animateContentSize()
) {
    // Fixed-height blurred banner — 180.dp gives a cinematic anchor
    ImageLoaderChoice(
        imageUrl = imageUrl,
        name = "",
        headers = model.extras.mapValues { it.value.toString() },
        placeHolder = { rememberVectorPainter(Icons.Default.BrokenImage) },
        contentScale = ContentScale.Crop,
        colorFilter = colorFilter,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .composed {
                val brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.surface
                    )
                )
                this
                    .blur(8.dp)
                    .drawWithContent {
                        drawContent()
                        drawRect(brush)
                    }
            }
    )

    // Content column — padding(top=110.dp) = 180.dp banner − 70.dp overlap
    // This makes the cover art visually "float" over the banner's lower edge
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 110.dp)
            .animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(4.dp)
                    .customSharedElement(
                        OtakuImageElement(
                            origin = model.imageUrl,
                            source = model.title,
                        )
                    )
                    .zoomOverlay()
            ) {
                ImageLoaderChoice(
                    imageUrl = imageUrl,
                    name = "",
                    headers = model.extras.mapValues { it.value.toString() },
                    contentScale = ContentScale.FillBounds,
                    placeHolder = { rememberVectorPainter(Icons.Default.BrokenImage) },
                    onImageSet = onBitmapSet,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                )
            }

            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    model.source.serviceName,
                    style = MaterialTheme.typography.labelSmall,
                )

                var descriptionVisibility by remember { mutableStateOf(false) }
                val clipboard = LocalClipboardManager.current
                Text(
                    model.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .customSharedElement(
                            OtakuTitleElement(
                                origin = model.title,
                                source = model.title
                            )
                        )
                        .combinedClickable(
                            interactionSource = null,
                            indication = ripple(),
                            onClick = { descriptionVisibility = !descriptionVisibility },
                            onLongClick = {
                                scope.launch {
                                    clipboard.setText(
                                        buildAnnotatedString { append(model.title) }
                                    )
                                }
                            }
                        )
                        .fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                )

                Crossfade(targetState = isFavorite, label = "") { target ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(
                                interactionSource = null,
                                indication = ripple()
                            ) { favoriteClick(isFavorite) }
                            .padding(4.dp)
                            .semantics(true) {}
                            .fillMaxWidth()
                    ) {
                        Icon(
                            if (target) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            stringResource(if (target) Res.string.removeFromFavorites else Res.string.addToFavorites),
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 16.sp,
                        )
                    }
                }

                Text(
                    stringResource(Res.string.chapter_count, model.chapters.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            model.genres.forEach {
                AssistChip(
                    onClick = {},
                    modifier = Modifier.fadeInAnimation(),
                    label = { Text(it) }
                )
            }
        }
        possibleDescription()
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid --no-daemon 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsHeader.kt
git commit -m "feat: redesign DetailsHeader with cinematic hero banner and floating cover art"
```

---

### Task 2: Update DetailsPortrait — remove CollapsableColumn, scroll header in LazyColumn

**Files:**

- Modify:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsPortrait.kt`

- [ ] **Step 1: Delete `collapsableBehavior` declaration**

Remove these lines from `DetailsView`:

```kotlin
val collapsableBehavior = rememberCollapsableTopBehavior(
    enterAlways = false,
    canScroll = { !fabMenuExpanded }
)
```

- [ ] **Step 2: Replace the `topBar` lambda — swap `CollapsableColumn { TopAppBar + DetailsHeader }`
  for just `TopAppBar`**

The current `topBar` is a `CollapsableColumn` wrapping both a `TopAppBar` and a `DetailsHeader`.
Replace the entire `topBar = { ... }` block with:

```kotlin
topBar = {
    TopAppBar(
        modifier = Modifier
            .zIndex(2f)
            .setBlurKind(blurKindState)
            .then(fabBlur),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (showBlur)
                Color.Transparent
            else
                Color.Unspecified,
        ),
        title = {
            Text(
                info.title,
                modifier = Modifier.basicMarquee()
            )
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
        },
        actions = {
            DetailActions(
                genericInfo = genericInfo,
                scaffoldState = scaffoldState,
                navController = navController,
                scope = scope,
                info = info,
                isSaved = isSaved,
                dao = dao,
                isFavorite = isFavorite,
                canNotify = canNotify,
                notifyAction = detailsActions.notifyChange,
                onReverseChaptersClick = { reverseChapters = !reverseChapters },
                onShowLists = { showLists = true },
                addToForLater = {
                    scope.launch {
                        val result = AppConfig.forLaterUuid?.let {
                            listDao.addToList(
                                it,
                                info.title,
                                info.description,
                                info.url,
                                info.imageUrl,
                                info.source.serviceName
                            )
                        } == true
                        hostState.showSnackbar(
                            getString(
                                if (result) Res.string.added_to_list else Res.string.already_in_list,
                                getString(Res.string.for_later)
                            ),
                            withDismissAction = true
                        )
                    }
                }
            )
        },
        scrollBehavior = scrollBehavior
    )
},
```

`DetailActions` is now called without a trailing `customActions` lambda — the default `{}` applies.
The `ArrowDropDownCircle` collapse/expand button is removed.

- [ ] **Step 3: Remove `collapsableBehavior.nestedScrollConnection` from `OtakuScaffold` modifier**

Find:

```kotlin
modifier = Modifier
    .nestedScroll(collapsableBehavior.nestedScrollConnection)
    .nestedScroll(scrollBehavior.nestedScrollConnection)
```

Replace with:

```kotlin
modifier = Modifier
    .nestedScroll(scrollBehavior.nestedScrollConnection)
```

- [ ] **Step 4: Add `DetailsHeader` as the first item in `LazyColumn`**

Inside the `LazyColumn { ... }` block, add this as the very first child (before the
`if (info.description.isNotEmpty())` check):

```kotlin
item(key = "header") {
    DetailsHeader(
        model = info,
        isFavorite = isFavorite,
        favoriteClick = { detailsActions.favoriteAction() },
        onPaletteSet = onPaletteSet,
        onBitmapSet = onBitmapSet,
        blurHash = blurHash,
    )
}
```

- [ ] **Step 5: Build to verify**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid --no-daemon 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

If compiler reports unused imports, remove: `CollapsableColumn`, `rememberCollapsableTopBehavior`,
`Icons.Default.ArrowDropDownCircle`, `Modifier.rotate`.

- [ ] **Step 6: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsPortrait.kt
git commit -m "feat: scroll DetailsHeader in LazyColumn, remove CollapsableColumn from portrait"
```

---

### Task 3: Update DetailsLandscape — add missing params + enhance gradient; update call site

**Files:**

- Modify:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsLandscape.kt`
- Modify:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsScreen.kt`

- [ ] **Step 1: Add `blurHash` and `onBitmapSet` to `DetailsViewLandscape` signature**

Find the `fun DetailsViewLandscape(` declaration and add two new parameters after `onPaletteSet`:

```kotlin
fun DetailsViewLandscape(
    info: KmpInfoModel,
    isSaved: Boolean,
    shareChapter: Boolean,
    chapters: List<ChapterWatched>,
    isFavorite: Boolean,
    description: String,
    onTranslateDescription: (MutableState<Boolean>) -> Unit,
    showDownloadButton: () -> Boolean,
    canNotify: Boolean,
    onPaletteSet: (Palette) -> Unit,
    blurHash: BitmapPainter? = null,
    onBitmapSet: (ImageBitmap) -> Unit = {},
    detailsActions: DetailsActions,
)
```

Then pass them into the `DetailsLandscapeContent(...)` call inside this function:

```kotlin
DetailsLandscapeContent(
    info = info,
    shareChapter = shareChapter,
    reverseChapters = reverseChapters,
    onReverse = { reverseChapters = it },
    description = description,
    onTranslateDescription = onTranslateDescription,
    chapters = chapters,
    isFavorite = isFavorite,
    listState = listState,
    isSaved = isSaved,
    showDownloadButton = showDownloadButton,
    canNotify = canNotify,
    onPaletteSet = onPaletteSet,
    blurHash = blurHash,
    onBitmapSet = onBitmapSet,
    scaffoldState = scaffoldState,
    detailsActions = detailsActions,
    modifier = Modifier.padding(p)
)
```

- [ ] **Step 2: Add `blurHash` and `onBitmapSet` to `DetailsLandscapeContent` signature**

Find `private fun DetailsLandscapeContent(` and add the same two parameters after `onPaletteSet`:

```kotlin
private fun DetailsLandscapeContent(
    info: KmpInfoModel,
    shareChapter: Boolean,
    isFavorite: Boolean,
    isSaved: Boolean,
    description: String,
    onTranslateDescription: (MutableState<Boolean>) -> Unit,
    chapters: List<ChapterWatched>,
    reverseChapters: Boolean,
    onReverse: (Boolean) -> Unit,
    scaffoldState: DrawerState,
    listState: LazyListState,
    showDownloadButton: () -> Boolean,
    canNotify: Boolean,
    onPaletteSet: (Palette) -> Unit,
    blurHash: BitmapPainter? = null,
    onBitmapSet: (ImageBitmap) -> Unit = {},
    detailsActions: DetailsActions,
    modifier: Modifier = Modifier,
    notificationRepository: NotificationRepository = koinInject(),
)
```

- [ ] **Step 3: Pass `blurHash` and `onBitmapSet` to `DetailsHeader` inside `listPane`**

Find the `DetailsHeader(...)` call inside the `listPane = { ... }` block and add the new params:

```kotlin
DetailsHeader(
    model = info,
    isFavorite = isFavorite,
    favoriteClick = { detailsActions.favoriteAction() },
    onPaletteSet = onPaletteSet,
    blurHash = blurHash,
    onBitmapSet = onBitmapSet,
    possibleDescription = {
        if (info.description.isNotEmpty()) {
            var descriptionVisibility by remember { mutableStateOf(false) }
            Box {
                val progress = remember { mutableStateOf(false) }

                Text(
                    description,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .combinedClickable(
                            interactionSource = null,
                            indication = ripple(),
                            onClick = { descriptionVisibility = !descriptionVisibility },
                            onLongClick = { onTranslateDescription(progress) }
                        )
                        .padding(horizontal = 4.dp)
                        .fillMaxWidth()
                        .animateContentSize()
                )

                if (progress.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
)
```

- [ ] **Step 4: Enhance the left pane background gradient**

Find this line inside `listPane`:

```kotlin
modifier = Modifier.drawBehind { drawRect(Brush.verticalGradient(listOf(c, b))) }
```

Replace with:

```kotlin
modifier = Modifier.drawBehind { drawRect(Brush.verticalGradient(listOf(c.copy(alpha = 0.85f), b))) }
```

- [ ] **Step 5: Add missing imports to `DetailsLandscape.kt` if not already present**

```kotlin
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
```

- [ ] **Step 6: Update `DetailContent` in `DetailsScreen.kt` — pass new params to landscape branch**

Find the `WindowWidthSizeClass.Expanded` branch inside `DetailContent` and add two params:

```kotlin
WindowWidthSizeClass.Expanded -> {
    DetailsViewLandscape(
        info = state.info,
        isSaved = isSaved,
        shareChapter = shareChapter,
        isFavorite = state.action is DetailFavoriteAction.Remove,
        chapters = details.chapters,
        description = details.description,
        onTranslateDescription = details::translateDescription,
        showDownloadButton = { showDownload },
        canNotify = details.dbModel?.shouldCheckForUpdate == true,
        onPaletteSet = { details.palette = it },
        blurHash = details.blurHash,
        onBitmapSet = { details.imageBitmap = it },
        detailsActions = detailsActions
    )
}
```

- [ ] **Step 7: Build to verify**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid --no-daemon 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsLandscape.kt
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsScreen.kt
git commit -m "feat: add blurHash/onBitmapSet parity to landscape details, enhance gradient"
```

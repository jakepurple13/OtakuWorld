# AccountInfoScreen Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (
> recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign AccountInfoScreen from a flat list of CategoryGroups into a dashboard with a
gradient profile strip, 3 M3-colored hero stat chips, activity heatmap, and 4 semantic sections (
Activity, Collection, Discovery, System).

**Architecture:** All changes are confined to `AccountInfoScreen.kt`. New private composables (
`ProfileStripCard`, `HeroStatChip`, `HeroChipsRow`, `SectionHeader`) are added at the bottom of the
file. The `AccountInfoScreen` body is replaced to use the new layout. No ViewModel, database, or
navigation changes needed.

**Tech Stack:** Compose Multiplatform (commonMain), Material3, Koin, existing `CategoryGroup` DSL,
`HeatMapWrapper`, `ImageLoaderChoice`

---

## File Map

| File                                                                                                                    | Change                                        |
|-------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/accountinfo/AccountInfoScreen.kt` | Full layout rewrite + new private composables |

---

### Task 1: Update `AccountInfoItem` (Int overload) — add `valueColor` param

**Files:**

- Modify:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/accountinfo/AccountInfoScreen.kt:303-315`

- [ ] **Step 1: Replace the Int overload of `AccountInfoItem`**

Find this block (starts ~line 303):

```kotlin
@Composable
private fun AccountInfoItem(
    title: String,
    description: String,
    amount: Int,
    modifier: Modifier = Modifier,
) = ListItem(
    headlineContent = { Text(title) },
    supportingContent = { Text(description) },
    trailingContent = { Text(animateIntAsState(amount).value.toString()) },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = modifier
)
```

Replace with:

```kotlin
@Composable
private fun AccountInfoItem(
    title: String,
    description: String,
    amount: Int,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) = ListItem(
    headlineContent = { Text(title) },
    supportingContent = { Text(description) },
    trailingContent = {
        Text(
            text = animateIntAsState(amount).value.toString(),
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.primary else valueColor,
        )
    },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = modifier
)
```

Also add `import androidx.compose.material3.MaterialTheme` to the imports (it's not in the file
yet).

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL. All existing `AccountInfoItem(amount = …)` call sites are unaffected (
default `valueColor = Color.Unspecified`).

---

### Task 2: Add `SectionHeader`, `ProfileStripCard`, `HeroStatChip`, `HeroChipsRow`

**Files:**

- Modify:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/accountinfo/AccountInfoScreen.kt` —
  append after the String overload of `AccountInfoItem`

- [ ] **Step 1: Add new imports** (add to the imports block at top of file)

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
```

- [ ] **Step 2: Append `SectionHeader` at end of file**

```kotlin
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}
```

- [ ] **Step 3: Append `ProfileStripCard` at end of file**

```kotlin
@Composable
private fun ProfileStripCard(
    profileUrl: String,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(primaryColor, tertiaryColor)))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ImageLoaderChoice(
                    profileUrl,
                    name = "",
                    placeHolder = { rememberVectorPainter(Icons.Default.AccountCircle) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                )
                Column {
                    Text(
                        text = "OtakuWorld",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = "OtakuWorld member",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: Append `HeroStatChip` at end of file**

```kotlin
@Composable
private fun HeroStatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

- [ ] **Step 5: Append `HeroChipsRow` at end of file**

```kotlin
@Composable
private fun HeroChipsRow(
    favorites: Int,
    chapters: Int,
    timeSpent: String,
    modifier: Modifier = Modifier,
) {
    val animatedFavorites by animateIntAsState(favorites)
    val animatedChapters by animateIntAsState(chapters)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeroStatChip(
            label = "Favorites",
            value = animatedFavorites.toString(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        HeroStatChip(
            label = "Chapters",
            value = animatedChapters.toString(),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        HeroStatChip(
            label = "Time",
            value = timeSpent,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
    }
}
```

- [ ] **Step 6: Verify compilation**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL. New composables are unused dead code at this point — that's fine.

---

### Task 3: Replace `AccountInfoScreen` body

**Files:**

- Modify:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/accountinfo/AccountInfoScreen.kt:46-301`

- [ ] **Step 1: Replace the entire `AccountInfoScreen` composable**

Find the whole `AccountInfoScreen` function (lines ~46–301, from
`@OptIn(ExperimentalMaterial3Api::class)` through the closing `}` of the `OtakuScaffold` block).
Replace it entirely with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoScreen(
    profileUrl: String?,
    appConfig: AppConfig = koinInject(),
    viewModel: AccountInfoViewModel = koinViewModel(),
) {
    val state = viewModel.accountInfo
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Info") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
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
            item {
                ProfileStripCard(
                    profileUrl = profileUrl.orEmpty(),
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            item {
                HeroChipsRow(
                    favorites = state.totalFavorites,
                    chapters = state.chapters,
                    timeSpent = state.timeSpentDoing,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            if (state.heatMaps.isNotEmpty()) {
                item {
                    var heatItem by remember { mutableStateOf<KmpHeat<Int>?>(null) }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        SectionHeader("🕐 Activity")
                        CategoryGroup {
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .animateContentSize()
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    HeatMapWrapper(
                                        data = state.heatMaps,
                                        onHeatClick = { heatItem = it },
                                    )
                                    heatItem?.let {
                                        Text(
                                            "Read/Watched ${it.data} on ${DateFormatItem.format(it.date)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    SectionHeader("⭐ Collection")
                    CategoryGroup {
                        if (appConfig.buildType == BuildType.Full) {
                            item {
                                AccountInfoItem(
                                    title = "Cloud Favorites",
                                    description = "Synced to cloud",
                                    amount = state.cloudFavorites,
                                )
                            }
                        }
                        item {
                            AccountInfoItem(
                                title = "Local Favorites",
                                description = "Stored on device",
                                amount = state.localFavorites,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Notifications",
                                description = "Pending update notifications",
                                amount = state.notifications,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Incognito Sources",
                                description = "Sources browsed privately",
                                amount = state.incognitoSources,
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    SectionHeader("🔍 Discovery")
                    CategoryGroup {
                        item {
                            AccountInfoItem(
                                title = "Sources",
                                description = "Installed extensions",
                                amount = state.sourceCount,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Search History",
                                description = "Recent searches",
                                amount = state.history,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Global Search History",
                                description = "Cross-source searches",
                                amount = state.globalSearchHistory,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Saved Recommendations",
                                description = "Suggested titles saved",
                                amount = state.savedRecommendations,
                            )
                        }
                        item {
                            AccountInfoItem(
                                title = "Lists",
                                description = "${state.itemsInLists} items total",
                                amount = state.lists,
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    SectionHeader("⚙️ System")
                    CategoryGroup {
                        item {
                            AccountInfoItem(
                                title = "Blur Hash Cache",
                                description = "Speeds up image loading",
                                amount = state.blurHashes,
                            )
                        }
                        if (appConfig.buildType != BuildType.NoFirebase) {
                            item {
                                AccountInfoItem(
                                    title = "Translation Models",
                                    description = "Downloaded language models",
                                    amount = state.translationModels,
                                )
                            }
                        }
                        item {
                            AccountInfoItem(
                                title = "Logged Exceptions",
                                description = "Errors captured by the app",
                                amount = state.exceptionCount,
                                valueColor = if (state.exceptionCount > 0)
                                    MaterialTheme.colorScheme.error
                                else
                                    Color.Unspecified,
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Remove now-unused imports**

These imports were only used by the old TopAppBar avatar and can be removed if no longer referenced
elsewhere in the file:

- `import androidx.compose.material3.TopAppBarDefaults` — still used (keep)
- `import androidx.compose.foundation.layout.size` — still used in ProfileStripCard (keep)

Check the full import list and remove any that are flagged unused by the IDE. The import
`import androidx.compose.foundation.lazy.LazyColumn` is still used. The actions block import for the
old TopAppBar avatar actions block is gone, but `ImageLoaderChoice` is still used in
`ProfileStripCard`.

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

---

### Task 4: Full build + visual verify + commit

- [ ] **Step 1: Full app build**

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Visual check — install and open AccountInfoScreen**

Navigate to: Settings → Account Info. Verify:

- TopAppBar: back button + "Account Info" title, no avatar
- Gradient profile strip with avatar + "OtakuWorld" + "OtakuWorld member"
- 3 tinted stat chips: Favorites (primary), Chapters (secondary), Time (tertiary)
- Activity section with heatmap (tap a cell → date text appears below)
- Collection section: Local Favorites, Notifications, Incognito Sources (Cloud Favorites visible
  only in Full build)
- Discovery section: Sources, Search History, Global Search History, Saved Recommendations, Lists (
  with "X items total" subtitle)
- System section: Blur Hash Cache, Translation Models (non-NoFirebase only), Logged Exceptions in
  error color if > 0

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/accountinfo/AccountInfoScreen.kt
git commit -m "$(cat <<'EOF'
feat: redesign AccountInfoScreen with dashboard layout

Replace flat list of CategoryGroups with: gradient profile strip,
M3-colored hero stat chips, activity heatmap section, and four
semantic sections (Activity, Collection, Discovery, System).

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

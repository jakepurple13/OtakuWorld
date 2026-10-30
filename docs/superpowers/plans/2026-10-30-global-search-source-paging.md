# Global Search source paging: plan

Spec: `docs/superpowers/specs/2026-10-30-global-search-source-paging-design.md`

1. `SourceSearchViewModel` + jvm tests (page append, dedupe, end detection, error + retry, waits for source)
   → verify: `./gradlew :kmpuiviews:jvmTest --tests "*SourceSearchViewModelTest*"`
2. Nav plumbing: `Screen.SourceSearchScreen`, `NavigationActions.sourceSearch`, `Navigation3Actions`,
   `navigation<Screen.SourceSearchScreen>` entry, `viewModelOf(::SourceSearchViewModel)`, `SearchModel.serviceName`
   → verify: `./gradlew :kmpuiviews:compileKotlinJvm`
3. `SourceSearchScreen` composable (grid, dividers, footer states, FAB, options sheet)
4. `GlobalSearchScreen`: rows open the new screen, bottom sheet removed
   → verify: `./gradlew :mangaworld:assembleNoFirebaseDebug`, then on-device check
5. CHANGELOG entry

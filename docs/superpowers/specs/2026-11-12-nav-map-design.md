# Navigation Map (debug screen) — Design

Approved mockup: https://claude.ai/artifact/Cea3JX4kWDDSson4Hey8Vr (proposal C of
https://claude.ai/artifact/Jibqrr3QBHaNDE7GiT2iq3).

## Goal

A debug/local-only screen that shows the **whole** navigation graph of the running app:
every registered Navigation3 screen and every place in code that navigates to it. No
runtime recording. One graph per app (MangaWorld, AnimeWorld, NovelWorld Android apps).

## Where the graph comes from

1. **Nodes** — every `navigation<Key>` and `xxxEntry<Key>` registration (Koin navigation3 DSL
   plus the private helpers `detailEntry`, `twoPaneEntry`, `dialogEntry`) in the app's source
   modules. The plain `entry<Key>` in the dead `UIViews/.../Nav3Graph.kt` is not a registration.
   Args come from the key's `data class` constructor.
2. **Scan** — a JVM tool (`:navgraph`) finds `navigate(X)` / `homeScreenNavigate(X)` /
   `clearBackStack(X)` calls and `navActions.foo()` / `navActions::foo` calls (method → key
   derived from `Navigation3Actions.kt`) and attributes each call site to a source screen:
   override file mapping → enclosing registration lambda → file holding the entry's composable →
   same-directory fallback → otherwise reported as unresolved.
3. **Overrides** — hand-written line files (`navgraph/overrides/*.navgraph`) add pseudo entry
   points (`@NavBar`, `@QuickActions`, `@DeepLinks`), file→screen attributions, explicit edges,
   ignores, and allowed orphans. Unknown ids in an override fail generation.
4. **Output** — `navgraph.json` is generated at build time into Android assets for the `debug`
   build type only (the noFirebase release APK is published, so it is excluded). MangaWorld desktop
   generates its own graph for local Gradle runs (`run`, `hotRunJvm`, `jvmRun`) and passes the path
   via `-Dotaku.navgraph`; packaged desktop builds get nothing. A JVM test over the real repo fails
   when a screen has no inbound edge and is not listed as an allowed orphan, or when an override no
   longer resolves.

## Screen

Settings › About › Developer › **Navigation map** (row, and the Developer entry itself, visible when
the build has a graph: `NavGraphSource.available`). Key: `Screen.DebugScreen.NavMap`.

- Top bar: back, title, app name. Search field. Filter chips: Entry points, Show args,
  Overrides only, No way in. Stats line: screens · edges · overrides · no way in.
- **Map** mode: pannable/zoomable canvas. Columns = BFS depth from entry points; screens not
  reachable from any entry point go in a last "No way in" column. Edges are curves (dashed for
  overrides). Tapping a node highlights inbound (teal) and outbound (amber) edges, dims the rest,
  and shows a card: key, args, in/out counts, orphan warning, Focus / Open / Close.
- **Focus** mode: trail of visited nodes (max 6, tappable), "Comes from" list, the node card,
  "Goes to" list. Each row shows the first call site file (or "override · note") and slides
  when walking.
- **Open** navigates to the key built by reflection with sample args (String "sample",
  numbers 1, Boolean false, nullable null). Disabled for pseudo nodes and when construction fails.
- No graph in build → empty state "No navigation graph in this build".

## Out of scope

AnimeWorld/NovelWorld desktop, iOS, runtime recording, semantic lanes.

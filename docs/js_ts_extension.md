# Writing a JS/TS Extension

OtakuWorld can load extensions written in plain JavaScript or TypeScript at
runtime, in addition to the existing JAR/APK extension system. This guide
covers everything you need to write one: the function contract, metadata,
sandboxing model, where to put the file, and how updates work.

## The request/parse contract

Each of the five operations an extension implements is split into two pure
functions instead of one:

- **`xRequest(...)`** — describes what to fetch (a URL and optional headers).
  It does **no networking itself** — it just returns a plain object.
- **`xParse(..., responseBody)`** — receives the text the host already
  fetched on your behalf, and turns it into the result.

The host (the app) calls `xRequest`, performs the actual HTTP fetch itself,
then calls `xParse` with the response body. Your code never touches the
network directly — there is no `fetch`, no `XMLHttpRequest`, no filesystem
access, nothing. This is the *only* bridge between your extension and the
outside world, and it's structural: the JavaScript sandbox your extension
runs in simply has nothing else available to call.

This also means every function must be **synchronous** — return a plain
value, not a `Promise`. `async`/`await` are not supported.

## Required functions

An extension must define all ten of these functions. If any are missing,
the extension is rejected at load time with a clear error listing exactly
which ones weren't found:

| Function                                                                | Purpose                                                   |
|-------------------------------------------------------------------------|-----------------------------------------------------------|
| `getPopularRequest(page)` / `getPopularParse(page, responseBody)`       | The "popular"/browse-all listing                          |
| `getLatestRequest(page)` / `getLatestParse(page, responseBody)`         | The "latest"/recently-updated listing                     |
| `searchRequest(query, page)` / `searchParse(query, page, responseBody)` | Search                                                    |
| `getDetailRequest(url)` / `getDetailParse(url, responseBody)`           | Full details + chapter list for one item                  |
| `getContentRequest(url)` / `getContentParse(url, responseBody)`         | The actual readable/playable content URLs for one chapter |

## Data shapes

These are the object shapes your `xRequest`/`xParse` functions consume and
return. A full TypeScript declaration file with all of these (usable for
editor type-checking and autocomplete) ships at
[
`sharedutils/jsextensionloader/samples/otaku-extension.d.ts`](../sharedutils/jsextensionloader/samples/otaku-extension.d.ts).

```typescript
interface ExtensionRequest {
    url: string;
    headers?: Record<string, string>;
}

interface ExtensionItem {
    title: string;
    url: string;
    imageUrl: string | null;
}

interface ExtensionChapter {
    name: string;
    url: string;
    uploaded: string | null;
}

interface ExtensionDetail {
    title: string;
    url: string;
    imageUrl: string | null;
    description: string | null;
    genres: string[];
    chapters: ExtensionChapter[];
}

interface ExtensionContent {
    urls: string[];
    headers?: Record<string, string>;
}
```

- `xRequest` functions return an `ExtensionRequest`.
- `getPopularParse`/`getLatestParse`/`searchParse` return `ExtensionItem[]`.
- `getDetailParse` returns an `ExtensionDetail`.
- `getContentParse` returns an `ExtensionContent`.

## Metadata

Every extension needs metadata: a name, version, and a couple of optional
fields. You provide this one of two ways.

**Option A — a comment header** at the top of the file, one key per line,
before any code:

```javascript
// name: My Extension
// version: 1.0.0
// author: Your Name
// description: What this extension does.
// iconUrl: https://example.com/icon.png
// updateUrl: https://example.com/my-extension/update.json
```

Only `name` and `version` are required; `author`, `description`, `iconUrl`,
and `updateUrl` are optional. The header stops being read at the first
non-comment, non-blank line — put it at the very top of the file.

**Option B — a companion `manifest.json`** file next to your `.js`/`.ts`
file (same base filename, `.manifest.json` suffix), e.g.
`my-extension.js` + `my-extension.manifest.json`:

```json
{
  "name": "My Extension",
  "version": "1.0.0",
  "author": "Your Name",
  "description": "What this extension does.",
  "iconUrl": "https://example.com/icon.png",
  "updateUrl": "https://example.com/my-extension/update.json"
}
```

If a companion `manifest.json` is present, it takes precedence over the
comment header. The extension's unique id defaults to its filename (without
extension) unless the manifest JSON explicitly sets an `"id"` field.

## Writing in TypeScript

TypeScript extensions are transpiled to JavaScript **on-device, at load
time** — there's no build step on your end beyond writing the file. The
on-device transpiler is strip-only: it removes type annotations, `interface`
blocks, `type` aliases, `export`, and `as` casts, but does **not**
type-check your code. Type safety and autocomplete come from the `.d.ts`
file in your own editor, not from anything that happens on-device.

Because it's strip-only, keep TypeScript extensions to straightforward
syntax: function declarations with typed parameters/return types, single-line
`interface`/`type` declarations. Multi-line `type` aliases and unusual
constructs aren't guaranteed to strip cleanly.

## Full example

A complete, working reference extension (stubbed data, no real network
calls) ships at
[
`sharedutils/jsextensionloader/samples/sample-extension.js`](../sharedutils/jsextensionloader/samples/sample-extension.js)
(and the TypeScript equivalent,
[`sample-extension.ts`](../sharedutils/jsextensionloader/samples/sample-extension.ts)).
Here's the JavaScript version in full:

```javascript
// name: Sample Extension
// version: 1.0.0
// author: OtakuWorld
// description: Reference/fixture extension with stubbed implementations of all required functions.
// iconUrl: https://example.com/sample-extension-icon.png
// updateUrl: https://example.com/sample-extension/update.json

function getPopularRequest(page) {
    return { url: "https://example.com/popular?page=" + page, headers: {} };
}

function getPopularParse(page, responseBody) {
    return [
        { title: "Sample Item", url: "https://example.com/item/1", imageUrl: null }
    ];
}

function getLatestRequest(page) {
    return { url: "https://example.com/latest?page=" + page, headers: {} };
}

function getLatestParse(page, responseBody) {
    return [
        { title: "Latest Sample Item", url: "https://example.com/item/2", imageUrl: null }
    ];
}

function searchRequest(query, page) {
    return { url: "https://example.com/search?q=" + query + "&page=" + page, headers: {} };
}

function searchParse(query, page, responseBody) {
    return [
        { title: "Search Result for " + query, url: "https://example.com/item/3", imageUrl: null }
    ];
}

function getDetailRequest(url) {
    return { url: url, headers: {} };
}

function getDetailParse(url, responseBody) {
    return {
        title: "Sample Item",
        url: url,
        imageUrl: null,
        description: "A sample item detail.",
        genres: ["Action"],
        chapters: [
            { name: "Chapter 1", url: "https://example.com/chapter/1", uploaded: null }
        ]
    };
}

function getContentRequest(url) {
    return { url: url, headers: {} };
}

function getContentParse(url, responseBody) {
    return {
        urls: ["https://example.com/content/1.png"],
        headers: {}
    };
}
```

Note that `responseBody` isn't used in this sample's parse functions — it's
returning fixed stub data. In a real extension, you'd parse `responseBody`
(HTML or JSON from the actual site) to build the returned objects.

## Where extensions are discovered

An extension can be loaded from three places:

1. **Bundled with the app** — shipped as an asset (Android) or classpath
   resource (Desktop) under a `js_extensions/` folder. This is how the
   sample extension itself is loaded.
2. **A local directory** — a per-platform extensions folder the app scans
   for `.js`/`.ts` files (plus optional companion `.manifest.json` files).
3. **A remote URL** — fetched over HTTP when an app explicitly downloads an
   extension from a given link.

## Auto-updates

If your extension's metadata includes an `updateUrl`, the app can check that
URL for a newer version (in addition to checking a centralized extension
registry, if the app is configured with one). The `updateUrl` should point
to a small JSON document describing the latest available version and where
to download it. Whether update checks run automatically, prompt the user
first, or are disabled entirely is a setting the app controls — not
something your extension configures itself.

## Sandboxing, in short

Your extension's JavaScript runs in an isolated engine instance with no
ambient capabilities: no network, no filesystem, no access to anything
outside the plain JavaScript values passed into it. The `xRequest`/`xParse`
split is not a suggestion — it's the only path in or out of the sandbox, by
construction. If you need data from the network, describe the request in
`xRequest` and read the result in `xParse`; there's no other way to reach
it.

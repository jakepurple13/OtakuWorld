# Vendored CloudStream code

Source: https://github.com/recloudstream/cloudstream at `d9c55e2e5b020dde8e28c12a3e7802df962265ac`
(`master`, 2026-10-15), module `library`. License: GPL-3.0 (see `LICENSE`); this whole module is
GPL-3.0.

CloudStream `.cs3` plugins are dex files compiled against CloudStream's classes and link to them by
name through the parent classloader, so the host must provide these classes with their original
package names and JVM signatures. Re-sync with `scripts/vendor-cloudstream.sh <sha>`, then
re-apply every edit marked `OTAKU:` (listed below).

Every vendored file starts with `// Copied from recloudstream/cloudstream <sha>, GPL-3.0`.

## Layout: expect/actual collapsed into androidMain

Upstream `library` is KMP (`commonMain`, `jvmCommonMain`, `androidMain`, `jvmMain`, `webMain`).
Everything here lives in this module's `src/androidMain/kotlin`; the module's other targets do not
see it. File names are kept exactly, because Kotlin derives the JVM facade class of top-level
declarations from the file name (`Coroutines.android.kt` -> `Coroutines_androidKt`), and plugins
built against the Android artifact reference those classes.

- `commonMain` files that only hold `expect` declarations are not copied:
  `com/lagradost/api/ContextHelper.kt`, `com/lagradost/api/Log.kt`,
  `com/lagradost/cloudstream3/network/WebViewResolver.kt`,
  `com/lagradost/cloudstream3/utils/SubtitleHelperPlatform.kt`.
- `commonMain` files that mix `expect` with other code have the `expect` lines removed.
- The `actual` modifier is removed from the `androidMain` / `jvmCommonMain` actuals.
- `jvmMain`, `webMain` and the test source sets are not copied.

## Dropped

- `commonMain/.../extractors/YoutubeExtractor.kt` and
  `jvmCommonMain/.../extractors/YoutubeExtractor.jvmCommon.kt` (`YoutubeExtractor`,
  `YoutubeShortLinkExtractor`, `YoutubeMobileExtractor`, `YoutubeNoCookieExtractor`). They are the
  only users of NewPipeExtractor; dropping them avoids that dependency. A plugin that calls them
  directly fails to load (the catalog handles that); `loadExtractor` simply finds no YouTube match.

## Local edits (`OTAKU:`)

- `com/lagradost/api/BuildConfig.kt` — **added**. Upstream generates this object with the
  buildkonfig plugin from `local.properties` (`MDL_API_KEY`, `TRAKT_CLIENT_ID`). We ship no keys, so
  both are `"null"`, the value upstream bakes in when no key is configured.
- `com/lagradost/cloudstream3/utils/Coroutines.kt` — removed the `expect` declarations of
  `runOnMainThreadNative`, `workerDispatcher` and `WorkerThread`.
- `com/lagradost/cloudstream3/mvvm/ArchComponentExt.kt` — removed the `expect` of
  `platformThrowAbleToResource`.
- `com/lagradost/cloudstream3/utils/ExtractorApi.kt` — removed the four YouTube extractor imports
  and their entries in `extractorApis`.
- `com/lagradost/api/Log.kt`, `com/lagradost/api/ContextHelper.android.kt`,
  `com/lagradost/cloudstream3/utils/Coroutines.android.kt`,
  `com/lagradost/cloudstream3/utils/Coroutines.jvmCommon.kt`,
  `com/lagradost/cloudstream3/utils/SubtitleHelperPlatform.android.kt`,
  `com/lagradost/cloudstream3/mvvm/ArchComponentExt.jvmCommon.kt` — `actual` modifiers removed.
- `com/lagradost/cloudstream3/network/WebViewResolver.android.kt` — `actual` modifiers removed, and
  the default argument values of the (not vendored) `expect class` moved onto the constructor and
  the three `resolveUsingWebView` overloads, so the JVM `$default` methods plugins call still exist.
  Adds the `USER_AGENT` import the defaults need.

## Dependencies

From upstream `gradle/libs.versions.toml` (catalog aliases `androidLibs.cloudstream*` in
`gradle/android.versions.toml`): NiceHttp `0.4.18`, jackson-module-kotlin `2.13.1` (required, not strict),
rhino strictly `1.8.1` (upstream pins both strictly for minSdk 26; see below for jackson), ksoup `0.2.6`, whyoleg cryptography
`0.6.0` (core + provider-optimal), atomicfu `0.33.0`, kotlinx-io-core `0.9.1`, androidx.annotation
`1.10.0`. jsoup, okhttp, ktor (`ktor-http` via `ktor-client-core`), kotlinx-datetime,
kotlinx-serialization-json, coroutines and kotlin-reflect use the project's existing versions.

Jackson is not pinned strictly: koog (via `:kmpuiviews:koogintegration`, on every app's classpath)
needs jackson 2.21.x, so in AnimeWorld it resolves to 2.21.3. Upstream's strict 2.13.1 pin avoids
crashes on API 25 and lower; AnimeWorld's minSdk is 28. Jackson 2.x stays binary compatible, so
plugins built against 2.13.1 run on 2.21.

## Files (copied verbatim unless marked edited)

All paths are under `src/androidMain/kotlin/`.

From `library/src/commonMain/kotlin`:

- `com/lagradost/cloudstream3/MainAPI.kt`
- `com/lagradost/cloudstream3/MainActivity.kt`
- `com/lagradost/cloudstream3/ParCollections.kt`
- `com/lagradost/cloudstream3/extractors/Acefile.kt`
- `com/lagradost/cloudstream3/extractors/Blogger.kt`
- `com/lagradost/cloudstream3/extractors/ByseSX.kt`
- `com/lagradost/cloudstream3/extractors/Cda.kt`
- `com/lagradost/cloudstream3/extractors/CineMMRedirect.kt`
- `com/lagradost/cloudstream3/extractors/CloudMailRuExtractor.kt`
- `com/lagradost/cloudstream3/extractors/ContentXExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Dailymotion.kt`
- `com/lagradost/cloudstream3/extractors/DoodExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Embedgram.kt`
- `com/lagradost/cloudstream3/extractors/EmturbovidExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Evolaod.kt`
- `com/lagradost/cloudstream3/extractors/Fastream.kt`
- `com/lagradost/cloudstream3/extractors/Filegram.kt`
- `com/lagradost/cloudstream3/extractors/Filemoon.kt`
- `com/lagradost/cloudstream3/extractors/Filesim.kt`
- `com/lagradost/cloudstream3/extractors/Firestream.kt`
- `com/lagradost/cloudstream3/extractors/Flyfile.kt`
- `com/lagradost/cloudstream3/extractors/GDMirrorbot.kt`
- `com/lagradost/cloudstream3/extractors/GUpload.kt`
- `com/lagradost/cloudstream3/extractors/GamoVideo.kt`
- `com/lagradost/cloudstream3/extractors/Gdriveplayer.kt`
- `com/lagradost/cloudstream3/extractors/GenericM3U8.kt`
- `com/lagradost/cloudstream3/extractors/Gofile.kt`
- `com/lagradost/cloudstream3/extractors/GoodstreamExtractor.kt`
- `com/lagradost/cloudstream3/extractors/HDMomPlayerExtractor.kt`
- `com/lagradost/cloudstream3/extractors/HDPlayerSystemExtractor.kt`
- `com/lagradost/cloudstream3/extractors/HDStreamAbleExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Hexload.kt`
- `com/lagradost/cloudstream3/extractors/HotlingerExtractor.kt`
- `com/lagradost/cloudstream3/extractors/HubCloud.kt`
- `com/lagradost/cloudstream3/extractors/HubuCloud.kt`
- `com/lagradost/cloudstream3/extractors/Hxfile.kt`
- `com/lagradost/cloudstream3/extractors/InternetArchive.kt`
- `com/lagradost/cloudstream3/extractors/JWPlayer.kt`
- `com/lagradost/cloudstream3/extractors/Jeniusplay.kt`
- `com/lagradost/cloudstream3/extractors/Krakenfiles.kt`
- `com/lagradost/cloudstream3/extractors/Linkbox.kt`
- `com/lagradost/cloudstream3/extractors/LuluStream.kt`
- `com/lagradost/cloudstream3/extractors/M3u8Manifest.kt`
- `com/lagradost/cloudstream3/extractors/MailRuExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Maxstream.kt`
- `com/lagradost/cloudstream3/extractors/Mediafire.kt`
- `com/lagradost/cloudstream3/extractors/MixDrop.kt`
- `com/lagradost/cloudstream3/extractors/Moviehab.kt`
- `com/lagradost/cloudstream3/extractors/Mp4Upload.kt`
- `com/lagradost/cloudstream3/extractors/Mvidoo.kt`
- `com/lagradost/cloudstream3/extractors/OdnoklassnikiExtractor.kt`
- `com/lagradost/cloudstream3/extractors/OkRuExtractor.kt`
- `com/lagradost/cloudstream3/extractors/PeaceMakerstExtractor.kt`
- `com/lagradost/cloudstream3/extractors/PixelDrainExtractor.kt`
- `com/lagradost/cloudstream3/extractors/PlayLtXyz.kt`
- `com/lagradost/cloudstream3/extractors/PlayerVoxzer.kt`
- `com/lagradost/cloudstream3/extractors/Playmate.kt`
- `com/lagradost/cloudstream3/extractors/Rabbitstream.kt`
- `com/lagradost/cloudstream3/extractors/RapidVidExtractor.kt`
- `com/lagradost/cloudstream3/extractors/SBPlay.kt`
- `com/lagradost/cloudstream3/extractors/SecvideoOnline.kt`
- `com/lagradost/cloudstream3/extractors/Sendvid.kt`
- `com/lagradost/cloudstream3/extractors/SibNetExtractor.kt`
- `com/lagradost/cloudstream3/extractors/SobreatsesuypExtractor.kt`
- `com/lagradost/cloudstream3/extractors/StreamEmbed.kt`
- `com/lagradost/cloudstream3/extractors/StreamSB.kt`
- `com/lagradost/cloudstream3/extractors/StreamSilk.kt`
- `com/lagradost/cloudstream3/extractors/StreamTape.kt`
- `com/lagradost/cloudstream3/extractors/StreamWishExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Streamcash.kt`
- `com/lagradost/cloudstream3/extractors/Streamhub.kt`
- `com/lagradost/cloudstream3/extractors/Streamlare.kt`
- `com/lagradost/cloudstream3/extractors/StreamoUpload.kt`
- `com/lagradost/cloudstream3/extractors/Streamplay.kt`
- `com/lagradost/cloudstream3/extractors/Supervideo.kt`
- `com/lagradost/cloudstream3/extractors/TRsTXExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Tantifilm.kt`
- `com/lagradost/cloudstream3/extractors/TauVideoExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Up4Stream.kt`
- `com/lagradost/cloudstream3/extractors/UpstreamExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Uqload.kt`
- `com/lagradost/cloudstream3/extractors/Userload.kt`
- `com/lagradost/cloudstream3/extractors/Userscloud.kt`
- `com/lagradost/cloudstream3/extractors/Uservideo.kt`
- `com/lagradost/cloudstream3/extractors/Vicloud.kt`
- `com/lagradost/cloudstream3/extractors/VidHidePro.kt`
- `com/lagradost/cloudstream3/extractors/VidMoxyExtractor.kt`
- `com/lagradost/cloudstream3/extractors/VidStack.kt`
- `com/lagradost/cloudstream3/extractors/Vidara.kt`
- `com/lagradost/cloudstream3/extractors/Videa.kt`
- `com/lagradost/cloudstream3/extractors/VideoSeyredExtractor.kt`
- `com/lagradost/cloudstream3/extractors/VidhideExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Vidmoly.kt`
- `com/lagradost/cloudstream3/extractors/Vido.kt`
- `com/lagradost/cloudstream3/extractors/Vidoza.kt`
- `com/lagradost/cloudstream3/extractors/Vids.kt`
- `com/lagradost/cloudstream3/extractors/Vidsonic.kt`
- `com/lagradost/cloudstream3/extractors/Vinovo.kt`
- `com/lagradost/cloudstream3/extractors/VkExtractor.kt`
- `com/lagradost/cloudstream3/extractors/Voe.kt`
- `com/lagradost/cloudstream3/extractors/Vtbe.kt`
- `com/lagradost/cloudstream3/extractors/WatchSB.kt`
- `com/lagradost/cloudstream3/extractors/Wibufile.kt`
- `com/lagradost/cloudstream3/extractors/XStreamCdn.kt`
- `com/lagradost/cloudstream3/extractors/YourUpload.kt`
- `com/lagradost/cloudstream3/extractors/Zplayer.kt`
- `com/lagradost/cloudstream3/extractors/helper/AesHelper.kt`
- `com/lagradost/cloudstream3/extractors/helper/AsianEmbedHelper.kt`
- `com/lagradost/cloudstream3/extractors/helper/CryptoJSHelper.kt`
- `com/lagradost/cloudstream3/extractors/helper/GogoHelper.kt`
- `com/lagradost/cloudstream3/extractors/helper/JWPlayerHelper.kt`
- `com/lagradost/cloudstream3/extractors/helper/NineAnimeHelper.kt`
- `com/lagradost/cloudstream3/extractors/helper/VstreamhubHelper.kt`
- `com/lagradost/cloudstream3/extractors/helper/WcoHelper.kt`
- `com/lagradost/cloudstream3/metaproviders/CrossTmdbProvider.kt`
- `com/lagradost/cloudstream3/metaproviders/MyDramaList.kt`
- `com/lagradost/cloudstream3/metaproviders/SyncRedirector.kt`
- `com/lagradost/cloudstream3/metaproviders/TmdbProvider.kt`
- `com/lagradost/cloudstream3/metaproviders/TraktProvider.kt`
- `com/lagradost/cloudstream3/mvvm/ArchComponentExt.kt` — **edited**
- `com/lagradost/cloudstream3/plugins/BasePlugin.kt`
- `com/lagradost/cloudstream3/plugins/CloudstreamPlugin.kt`
- `com/lagradost/cloudstream3/syncproviders/SyncAPI.kt`
- `com/lagradost/cloudstream3/utils/AppDebug.kt`
- `com/lagradost/cloudstream3/utils/AppUtils.kt`
- `com/lagradost/cloudstream3/utils/AtomicList.kt`
- `com/lagradost/cloudstream3/utils/Coroutines.kt` — **edited**
- `com/lagradost/cloudstream3/utils/ExtractorApi.kt` — **edited**
- `com/lagradost/cloudstream3/utils/HlsPlaylistParser.kt`
- `com/lagradost/cloudstream3/utils/JsHunter.kt`
- `com/lagradost/cloudstream3/utils/JsInterpreter.kt`
- `com/lagradost/cloudstream3/utils/JsUnpacker.kt`
- `com/lagradost/cloudstream3/utils/Levenshtein.kt`
- `com/lagradost/cloudstream3/utils/M3u8Helper.kt`
- `com/lagradost/cloudstream3/utils/StringUtils.kt`
- `com/lagradost/cloudstream3/utils/SubtitleHelper.kt`
- `com/lagradost/cloudstream3/utils/UnshortenUrl.kt`
- `com/lagradost/cloudstream3/utils/serializers/FloatAsIntSerializer.kt`
- `com/lagradost/cloudstream3/utils/serializers/FloatAsLongSerializer.kt`
- `com/lagradost/cloudstream3/utils/serializers/JsonTransformSerializer.kt`
- `com/lagradost/cloudstream3/utils/serializers/NonEmptySerializer.kt`
- `com/lagradost/cloudstream3/utils/serializers/NullableStringSerializer.kt`
- `com/lagradost/cloudstream3/utils/serializers/WriteOnlySerializer.kt`

From `library/src/jvmCommonMain/kotlin`:

- `com/lagradost/cloudstream3/mvvm/ArchComponentExt.jvmCommon.kt` — **edited**
- `com/lagradost/cloudstream3/utils/Coroutines.jvmCommon.kt` — **edited**

From `library/src/androidMain/kotlin`:

- `com/lagradost/api/ContextHelper.android.kt` — **edited**
- `com/lagradost/api/Log.kt` — **edited**
- `com/lagradost/cloudstream3/network/WebViewResolver.android.kt` — **edited**
- `com/lagradost/cloudstream3/utils/Coroutines.android.kt` — **edited**
- `com/lagradost/cloudstream3/utils/SubtitleHelperPlatform.android.kt` — **edited**

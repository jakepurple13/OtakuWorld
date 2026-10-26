# vlcj + bundled libVLC in Compose Desktop (macOS arm64): spike report

Date: 2026-10-23. Machine: Apple Silicon (`uname -m` = arm64), 18 cores, JDK Corretto 21.0.7.
`/Applications/VLC.app` is **absent**. Nothing is installed system-wide.

## Verdict

| Bar item | Result |
|---|---|
| 1. 1080p HLS through a header-injecting localhost proxy, no stutter | **PASS** |
| 2. Compose controls and subtitle `Text` drawn on top of the video | **PASS** |
| 3. Reasonable CPU, frame rate logged | **PASS** (about 55-65% of one core at 1080p**60**) |
| 4. Bonus: DASH `.mpd` through the same proxy | **PASS**. The rewrite was easy: about 15 lines. |

**Recommendation: use vlcj 4.x with a bundled libVLC 3.0.x and callback rendering into Skia.** No bar
item failed, so I did not investigate libmpv.

## libVLC

- Version: `libvlc version=3.0.24 Vetinari changeset=3.0.24-rc1-0-g6de05adcba` (logged at runtime).
- Source: the official `https://get.videolan.org/vlc/3.0.24/macosx/vlc-3.0.24-arm64.dmg`, downloaded
  with curl into `downloads/`. I mounted it with `hdiutil attach -nobrowse -readonly -mountpoint downloads/mnt`,
  copied `VLC.app/Contents/MacOS/{lib,plugins}` to `natives/macos-arm64/`, then detached it.
- Bundle size: **90 MB** in total. `lib/` is 1.1 MB (libvlc and libvlccore). `plugins/` is 88 MB (337 entries).
  The plugins can be pruned a lot (see below).
- vlcj: `uk.co.caprica:vlcj:4.12.1`. This is the latest 4.x on Maven Central. 5.0.0-M6 also exists but is a milestone.
- Discovery uses a custom `BaseNativeDiscoveryStrategy` (`BundledDiscovery` in `Main.kt`). It searches **only**
  `natives/macos-arm64/lib`, preloads `libvlccore` by absolute path, and calls `setenv("VLC_PLUGIN_PATH", lib/../plugins)`
  through vlcj's `LibC`. No ServiceLoader directory providers are used, so `/Applications/VLC.app` is never probed.
  Log evidence:
  ```
  [vlc] loaded libvlc from .../player-spike/natives/macos-arm64/lib/libvlc.dylib
  [vlc] loaded libvlccore from .../player-spike/natives/macos-arm64/lib/libvlccore.dylib
  ```

## Rendering approach and its cost

- I used vlcj `CallbackVideoSurface` (`factory.videoSurfaces().newVideoSurface(BufferFormatCallback, RenderCallback, true)`)
  with `RV32BufferFormat`, which is BGRA in memory.
- One Skia `Bitmap` (`BGRA_8888`, `OPAQUE`) is allocated per format change. In `display()`, running on the VLC thread,
  VLC's direct `ByteBuffer` is memcpy'd straight into the bitmap's native pixels
  (`Pointer(bitmap.peekPixels().addr).getByteBuffer(...).put(src)`). This needs no JVM heap byte array.
- A `mutableLongStateOf` tick is bumped for each frame. The `Canvas` reads the tick **only in the draw phase**, so frames
  cause no recomposition. In the draw phase, `Image.makeFromBitmap()` takes a second copy under a lock, and then
  `nativeCanvas.drawImageRect(...)` draws it, letterboxed.
- Cost: two 8 MB memcpys per frame plus a texture upload by Skia. At 1080p60 this totals about 55-65% of one core
  for the whole JVM process. That figure includes VideoToolbox decode setup, demux, audio, the proxy and Compose.
- Decoding is hardware: the log shows `videotoolbox decoder: Using Video Toolbox to decode 'h264'`.
- The decoded buffer is 1920x**1088** (macroblock padding). The real app should crop to the display size reported by
  `newFormatSize` or by track info.
- An embedded AWT Canvas (`EmbeddedMediaPlayerComponent` or `SwingPanel`) was **not** tried. On macOS it is a
  heavyweight NSView that would draw above Compose and hide the overlay. Callback rendering made it unnecessary.

## Evidence

### Bar 1: HLS, 1080p60, through the proxy (`run-hls.log`)

The stream is `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`. Its 1080p variant is `url_8`, which ffprobe
reports as h264 1920x1080 at **60/1 fps**. `--adaptive-logic=highest --adaptive-maxheight=1080` pins it to 1080p.
```
[proxy] #1 200 HLS https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8 [Referer=https://spike.example/player, User-Agent=OtakuSpike/1.0, X-Spike=1] 301ms
[proxy] #2 200 HLS https://test-streams.mux.dev/x36xhzz/url_8/193039199_mp4_h264_aac_fhd_7.m3u8 [Referer=..., X-Spike=1] 100ms
[proxy] #3 200 SEG https://test-streams.mux.dev/x36xhzz/url_8/url_590/193039199_mp4_h264_aac_fhd_7.ts [Referer=..., X-Spike=1] len=5312128 ttfb=113ms
[render] buffer format 1920x1088 RV32
[stats] vlcFrames/s=60 composeDraws/s=64 video=1920x1088 t=1204ms ...
```
- All 7 upstream requests carried `X-Spike=1` and the `Referer` (`grep -c X-Spike=1 run-hls.log` gives 7 out of 7).
  The `[...]` list is read back from OkHttp's final request headers, so these are the headers actually sent.
- The fps was steady for the whole run. VLC delivered 60-61 frames/s every second. Compose drew 61-65 times per second
  (Compose draws are vsync-driven, so they can run slightly above the frame rate). The only dips (44 and 22) came in the
  same seconds as my in-app PNG screenshot, which encodes on the UI thread. That is a measurement artifact.
- I could not watch the screen myself. "No visible stutter" is inferred from the steady 60 fps delivery and draw counts,
  plus no buffering events after startup. **Please eyeball it once.**

### Bar 2: overlay (screenshots)

- `shots/hls-1.png` (t=15.2 s) and `shots/hls-2.png` (t=18.2 s) are different frames, so the video is advancing.
  Both show the following on top of the video: the yellow fps label at top left, the subtitle `Text` with its
  translucent background, and the control bar (Pause button, seek Slider, `0:14 / 10:34` and then `0:18 / 10:34`).
- `shots/dash-1.png` and `shots/dash-2.png` show the same for DASH. The burned-in text reads
  "1920x1080 / 8000 kbps / 30 fps" and "Frame 537".
- `screencapture` failed with "could not create image from display", because the terminal lacks Screen Recording
  permission. The screenshots were therefore taken **inside the app** with skiko's `SkiaLayer.screenshot()` on the
  window's real render layer. They show exactly what Compose composited into the window, not an offscreen re-render.

### Bar 3: CPU and fps

`ps -o %cpu` was sampled for the java process every 5 s over about 35 s of playback. 100% equals one core.

| Stream | Samples (%cpu) | Typical |
|---|---|---|
| HLS 1080p**60** | 70.0, 58.4, 54.9, 101.0*, 64.3, 52.5, 55.3 | about 55-65% |
| DASH 1080p**30** | 40.8, 42.8, 46.8, 46.5, 39.7, 41.0, 38.6 | about 40-45% |

\* The 101% sample coincides with the in-app PNG screenshot encode.

- RSS is about 440-560 MB.
- Rendered frames per second (frames delivered to Compose and drawn): about 60 for HLS and about 30 for DASH
  (`[stats]` lines in the logs).

### Bar 4: DASH (`run-dash.log`)

The stream is `https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd`. Playback started and VLC chose
`bbb_30fps_1920x1080_8000k`. It ran at a steady 30 vlcFrames/s (33-35 Compose draws/s). All 43 upstream requests,
including the init segments, the `.m4v` and `.m4a` segments, and the MPD itself, carried `X-Spike=1`.

There is one benign log line at startup, before the first frame:
`main decoder error: Timestamp conversion failed ... no reference clock`

## Proxy design notes (`src/main/kotlin/Proxy.kt`)

- The server is NanoHTTPD 2.3.1, bound to `127.0.0.1` on port 0 (a random port). Each session has a random 96-bit URL
  token. Requests without the token get a 403. Upstream requests use OkHttp 5.4.0, with custom headers set on every
  request.
- **Path-based routing:** `/{token}/r/{base64url(upstream directory URL)}/{file}?{query}`. The upstream directory is
  carried as a path segment. Because of this, any *relative* URI the player resolves against a proxied URL
  automatically lands back on the proxy, under the right upstream directory. This is what makes DASH almost free.
- **HLS:** every URI line is resolved against the playlist URL (handling absolute, root-relative and relative forms)
  and rewritten to the proxy. Every `URI="..."` attribute is rewritten the same way (`EXT-X-KEY`, `EXT-X-MAP`,
  `EXT-X-MEDIA`, `EXT-X-I-FRAME-STREAM-INF`).
- **DASH:** each `<BaseURL>` is resolved and rewritten to the proxied form. If there is no BaseURL, one is injected
  after `<MPD>`. Absolute `media`, `initialization` and `sourceURL` attributes are rewritten. `$Number$` and `$Time$`
  templates expand on the client side against the proxied base, so they need no work.
  - Not handled: BaseURLs nested at Period or AdaptationSet level that are relative to a parent BaseURL. These are
    still correct because of path routing, but multi-CDN `<BaseURL>` lists are not handled specially. SegmentBase
    and byte-range addressing works because `Range` is forwarded.
  - Overall difficulty: low.
- **Gotcha found and fixed:** VLC sends `Range: bytes=0-` even for playlists. On the first run the upstream returned
  206 and the rewritten body went back with a stale range, so VLC logged `access stream error: HTTP connection failure`
  and retried. The fix: do not forward `Range` for manifests, and always answer 200 with the rewritten body.
  `Range`, `Content-Range` and 206 are still passed through for segments.

## Gotchas and notes for productionizing

1. **Quarantine and codesign:** the dylibs are signed by VideoLAN (TeamIdentifier `75GAHG3SZQ`, hardened runtime).
   No `com.apple.quarantine` xattr was present because curl and `cp` from a mounted dmg do not add one. A
   browser-downloaded dmg *would* add it, so run `xattr -dr com.apple.quarantine`.
   - For a notarized, packaged app, re-sign all 300+ dylibs with your own identity, or keep VideoLAN's signatures and
     add `com.apple.security.cs.disable-library-validation` (needed to load third-party-signed dylibs into a
     hardened-runtime JVM). This also needs `allow-jit` and `allow-unsigned-executable-memory` for the JVM anyway.
     Running via `./gradlew run` (an unsigned `java`) needed none of this.
2. **@rpath:** `libvlc.dylib` links `@rpath/libvlccore.dylib`. Preloading libvlccore by absolute path before
   libvlc (done in `BundledDiscovery.onFound`) makes dyld resolve it. Keep the `lib/` and `plugins/` sibling layout.
3. **Plugin cache:** the arm64 dmg ships no `plugins.dat`, so libVLC scans all 337 plugins on first
   `MediaPlayerFactory` creation. Startup was still sub-second-ish here.
   - Options for the real app: generate the cache once at first launch into a writable dir, or prune the plugins.
     For HLS, DASH, mp4 and mkv with h264, hevc or aac you need roughly these: adaptive, ts, mp4, mkv, es, avcodec,
     videotoolbox, cvpx, swscale, the audio output (auhal), http/https access, vmem, and the subtitle/freetype
     plugins. You can drop bluray (including two `.jar` files), dvd, skins, lua, the qt/macosx GUI, upnp and others.
     This could plausibly cut the bundle to about 25-35 MB (not measured).
4. **`VLC_PLUGIN_PATH`** must be set *natively*, with `setenv` before `libvlc_new`. Java's `System.getenv` will still
   print `null`, which is expected.
5. **Buffer is 1920x1088**, not 1080: crop when drawing.
6. Screenshots or other UI-thread work cost frames, so keep the draw path cheap. A future optimization: make the
   Skia `Image` directly from VLC's buffer (zero-copy, or a GPU texture via `DirectContext`). This is not needed at
   current CPU levels.
7. VLC's adaptive logic will pick 4K on DASH unless capped. Use `--adaptive-maxheight` or `--adaptive-maxwidth`, or
   expose a quality setting.
8. Not tested: Intel macs, Windows or Linux bundling, seek and scrub latency (the slider is wired to
   `setPosition`, but I did not drive it), audio/video sync quality by ear, and HLS AES keys. Key URIs are rewritten,
   but the test stream has no keys.

## Files

- `build.gradle.kts`, `settings.gradle.kts`: Kotlin 2.4.20, Compose MPP 1.13.0-alpha02 (same as OtakuWorld),
  vlcj 4.12.1, NanoHTTPD 2.3.1, OkHttp 5.4.0.
- `src/main/kotlin/Main.kt`: discovery, frame sink, UI and stats.
- `src/main/kotlin/Proxy.kt`: the proxy.
- `natives/macos-arm64/`: the bundled libVLC.
- `downloads/`: the dmg.
- `run-hls.log`, `run-dash.log`, `shots/*.png`: evidence.
- `scripts/run-and-sample.sh`: runs the spike, samples CPU, then stops it.

## Run it yourself

```bash
cd /private/tmp/claude-501/-Users-jacobrein-StudioProjects-OtakuWorld/e43d7130-2ab5-4574-8557-db8f2ecfbddb/scratchpad/player-spike
./gradlew run                                                            # 1080p60 HLS via proxy
./gradlew run -Pstream=https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd   # DASH
# optional: -PshotDir=$PWD/shots -PshotName=foo  -> in-app screenshots at ~15s and ~18s
```
Close the window to quit. `[proxy]` and `[stats]` lines are printed to the console.

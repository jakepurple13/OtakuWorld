# Test fixtures

`tiny clip ü.mp4` (3 s, 320x180 h264 + aac + one `mov_text` subtitle track in English, ~43 KB) is used by
`DesktopPlayerTest`. The space and the `ü` in the name are deliberate: downloaded files with such names must play.
The subtitle track comes from `fixture-subs.srt`.

Regenerate it from this directory with:

```bash
ffmpeg -y -f lavfi -i testsrc=size=320x180:rate=25 -f lavfi -i sine -i fixture-subs.srt -t 3 \
  -map 0:v -map 1:a -map 2:s -c:v libx264 -pix_fmt yuv420p -c:a aac -c:s mov_text \
  -metadata:s:s:0 language=eng -shortest "tiny clip ü.mp4"
```

`hls-download.ts` (3 s, 320x180 h264 + aac, ~60 KB) is an HLS download as `EpisodeDownloader` writes it: three
1 s MPEG-TS segments concatenated byte for byte. `DesktopPlayerTest` plays it with a subtitle sidecar beside it.

Regenerate it from an empty scratch directory with:

```bash
ffmpeg -y -f lavfi -i testsrc=size=320x180:rate=25 -f lavfi -i sine -t 3 -c:v libx264 -pix_fmt yuv420p -g 25 \
  -c:a aac -f hls -hls_time 1 -hls_list_size 0 -hls_segment_filename 'seg%d.ts' index.m3u8
cat seg0.ts seg1.ts seg2.ts > hls-download.ts
```

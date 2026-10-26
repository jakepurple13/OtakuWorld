# DASH fixture

A static 3 s DASH stream (320x180 h264 video and 44.1 kHz mono aac audio in separate AdaptationSets,
`SegmentTemplate` with `$Number%05d$`, 1 s segments, ~84 KB) used by `DashDownloaderTest`,
`AnimeDownloadManagerTest` and the desktop `DashDownloadPlaybackTest`.

ffmpeg also writes a fourth, 189-byte audio segment (`chunk-1-00004.m4s`) for the encoder tail. A DASH
client counts 3 s / 1 s = 3 segments per track from the template and never requests it.

Regenerate it from this directory with:

```bash
ffmpeg -y -f lavfi -i testsrc=size=320x180:rate=25 -f lavfi -i sine=frequency=440 -t 3 \
  -map 0:v -map 1:a -c:v libx264 -pix_fmt yuv420p -g 25 -keyint_min 25 -sc_threshold 0 -b:v 200k \
  -c:a aac -b:a 64k \
  -f dash -seg_duration 1 -use_template 1 -use_timeline 0 \
  -adaptation_sets "id=0,streams=v id=1,streams=a" \
  -init_seg_name 'init-$RepresentationID$.m4s' -media_seg_name 'chunk-$RepresentationID$-$Number%05d$.m4s' \
  manifest.mpd
```

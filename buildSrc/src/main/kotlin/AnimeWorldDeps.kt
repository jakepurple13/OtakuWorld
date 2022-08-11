object AnimeWorldDeps {

}

object Media3Deps {

    // For media playback using ExoPlayer
    private const val exoplayer = "androidx.media3:media3-exoplayer:${Deps.media3Version}"

    // For DASH playback support with ExoPlayer
    private const val exoplayerDash = "androidx.media3:media3-exoplayer-dash:${Deps.media3Version}"

    // For HLS playback support with ExoPlayer
    private const val exoplayerHls = "androidx.media3:media3-exoplayer-hls:${Deps.media3Version}"

    // For RTSP playback support with ExoPlayer
    private const val exoplayerRtsp = "androidx.media3:media3-exoplayer-rtsp:${Deps.media3Version}"

    // For ad insertion using the Interactive Media Ads SDK with ExoPlayer
    private const val exoplayerIma = "androidx.media3:media3-exoplayer-ima:${Deps.media3Version}"

    // For loading data using the Cronet network stack
    private const val datasourceCronet = "androidx.media3:media3-datasource-cronet:${Deps.media3Version}"

    // For loading data using the OkHttp network stack
    private const val datasourceOkhttp = "androidx.media3:media3-datasource-okhttp:${Deps.media3Version}"

    // For loading data using librtmp
    private const val datasourceRtmp = "androidx.media3:media3-datasource-rtmp:${Deps.media3Version}"

    // For building media playback UIs
    private const val ui = "androidx.media3:media3-ui:${Deps.media3Version}"

    // For exposing and controlling media sessions
    private const val session = "androidx.media3:media3-session:${Deps.media3Version}"

    // For extracting data from media containers
    private const val extractor = "androidx.media3:media3-extractor:${Deps.media3Version}"

    // For integrating with Cast
    private const val cast = "androidx.media3:media3-cast:${Deps.media3Version}"

    // For scheduling background operations using Jetpack Work"s WorkManager with ExoPlayer
    private const val exoplayerWorkmanager = "androidx.media3:media3-exoplayer-workmanager:${Deps.media3Version}"

    // For transforming media files
    private const val transformer = "androidx.media3:media3-transformer:${Deps.media3Version}"

    // Utilities for testing media components (including ExoPlayer components)
    const val testUtils = "androidx.media3:media3-test-utils:${Deps.media3Version}"

    // Utilities for testing media components (including ExoPlayer components) via Robolectric
    const val testUtilsRobolectric = "androidx.media3:media3-test-utils-robolectric:${Deps.media3Version}"

    // Common functionality for media database components
    private const val database = "androidx.media3:media3-database:${Deps.media3Version}"

    // Common functionality for media decoders
    private const val decoder = "androidx.media3:media3-decoder:${Deps.media3Version}"

    // Common functionality for loading data
    private const val datasource = "androidx.media3:media3-datasource:${Deps.media3Version}"

    // Common functionality used across multiple media libraries
    private const val common = "androidx.media3:media3-common:${Deps.media3Version}"

    val exoplayerLibs = arrayOf(
        exoplayer,
        exoplayerDash, exoplayerHls, exoplayerRtsp, exoplayerIma,
        datasourceCronet, datasource, datasourceRtmp, datasourceOkhttp,
        ui,
        session,
        extractor,
        cast,
        exoplayerWorkmanager,
        transformer,
        database,
        decoder,
        common
    )
}

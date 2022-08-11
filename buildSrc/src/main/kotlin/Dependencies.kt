object Deps {

    const val kotlinVersion = "1.7.10"
    const val latestAboutLibsRelease = "10.3.1"

    private const val jakepurple13 = "10.6.5"

    const val flowutils = "com.github.jakepurple13.HelpfulTools:flowutils:$jakepurple13"
    const val gsonutils = "com.github.jakepurple13.HelpfulTools:gsonutils:$jakepurple13"
    const val helpfulutils = "com.github.jakepurple13.HelpfulTools:helpfulutils:$jakepurple13"
    const val loggingutils = "com.github.jakepurple13.HelpfulTools:loggingutils:$jakepurple13"
    const val dragswipe = "com.github.jakepurple13.HelpfulTools:dragswipe:$jakepurple13"
    const val funutils = "com.github.jakepurple13.HelpfulTools:funutils:$jakepurple13"
    const val thirdpartyutils = "com.github.jakepurple13.HelpfulTools:thirdpartyutils:$jakepurple13"

    const val palette = "androidx.palette:palette-ktx:1.0.0"
    const val junit = "junit:junit:4.+"
    const val androidJunit = "androidx.test.ext:junit:1.1.3"
    const val androidEspresso = "androidx.test.espresso:espresso-core:3.4.0"

    const val coroutinesVersion = "1.6.4"

    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"

    const val gson = "com.google.code.gson:gson:2.9.1"

    const val glideVersion = "4.13.2"
    const val glide = "com.github.bumptech.glide:glide:$glideVersion"
    const val glideCompiler = "com.github.bumptech.glide:compiler:$glideVersion"

    const val pagingVersion = "3.1.1"

    const val androidCore = "androidx.core:core-ktx:1.8.0"
    const val appCompat = "androidx.appcompat:appcompat:1.4.2"
    const val material = "com.google.android.material:material:1.7.0-alpha03"

    const val preference = "androidx.preference:preference-ktx:1.2.0"

    const val recyclerview = "androidx.recyclerview:recyclerview:1.2.1"
    const val constraintlayout = "androidx.constraintlayout:constraintlayout:2.1.4"
    const val swiperefresh = "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"

    const val jsoup = "org.jsoup:jsoup:1.15.2"

    const val crashlytics = "com.google.firebase:firebase-crashlytics:18.2.12"
    const val analytics = "com.google.firebase:firebase-analytics:21.1.0"
    const val playServices = "com.google.android.gms:play-services-auth:20.1.0"

    const val media3Version = "1.0.0-beta02"
    const val roomVersion = "2.5.0-alpha02"

    const val navVersion = "2.5.1"

    const val koinVersion = "3.2.0"

    // Koin main features for Android (Scope,ViewModel ...)
    const val koinAndroid = "io.insert-koin:koin-android:$koinVersion"
    const val koinCompose = "io.insert-koin:koin-androidx-compose:$koinVersion"

    const val lottieVersion = "5.2.0"

    const val coil = "2.1.0"

    const val lifecycle = "2.5.1"

    const val jetpack = "1.3.0-alpha03"
    const val jetpackCompiler = "1.3.0"

    const val accompanist = "0.26.1-alpha"

    const val composeUi = "androidx.compose.ui:ui:$jetpack"

    // Tooling support (Previews, etc.)
    const val composeUiTooling = "androidx.compose.ui:ui-tooling:$jetpack"

    // Foundation (Border, Background, Box, Image, Scroll, shapes, animations, etc.)
    const val composeFoundation = "androidx.compose.foundation:foundation:$jetpack"

    // Material Design
    const val composeMaterial = "androidx.compose.material:material:$jetpack"
    const val materialYou = "androidx.compose.material3:material3:1.0.0-alpha16"

    // Material design icons
    const val composeMaterialIconsCore = "androidx.compose.material:material-icons-core:$jetpack"
    const val composeMaterialIconsExtended = "androidx.compose.material:material-icons-extended:$jetpack"

    // Integration with activities
    const val composeActivity = "androidx.activity:activity-compose:1.5.1"

    // Integration with ViewModels
    const val composeLifecycle = "androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle"
    const val composeLifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-compose:2.6.0-alpha01"

    // Integration with observables
    const val composeRuntimeLivedata = "androidx.compose.runtime:runtime-livedata:$jetpack"
    const val composeMaterialThemeAdapter = "com.google.android.material:compose-theme-adapter:1.1.15"
    const val composeMaterial3ThemeAdapter = "com.google.android.material:compose-theme-adapter-3:1.0.15"
    const val landscapistGlide = "com.github.skydoves:landscapist-glide:1.6.1"
    const val composeConstraintLayout = "androidx.constraintlayout:constraintlayout-compose:1.0.1"
    const val composeAnimation = "androidx.compose.animation:animation:$jetpack"
    const val materialPlaceholder = "com.google.accompanist:accompanist-placeholder-material:$accompanist"
    const val drawablePainter = "com.google.accompanist:accompanist-drawablepainter:$accompanist"
    const val permissions = "com.google.accompanist:accompanist-permissions:$accompanist"
    const val uiUtil = "androidx.compose.ui:ui-util:$jetpack"
    const val coilCompose = "io.coil-kt:coil-compose:$coil"
    const val navCompose = "androidx.navigation:navigation-compose:$navVersion"
    const val navMaterial = "com.google.accompanist:accompanist-navigation-material:$accompanist"
    const val navAnimation = "com.google.accompanist:accompanist-navigation-animation:$accompanist"
    const val flowLayout = "com.google.accompanist:accompanist-flowlayout:$accompanist"

    const val swipeRefresh = "com.google.accompanist:accompanist-swiperefresh:$accompanist"
    const val systemUiController = "com.google.accompanist:accompanist-systemuicontroller:$accompanist"

    const val datastore = "androidx.datastore:datastore:1.0.0"
    const val datastorePref = "androidx.datastore:datastore-preferences:1.0.0"

    const val okhttpVersion = "4.10.0"
    const val okhttpLib = "com.squareup.okhttp3:okhttp:$okhttpVersion"
    const val okhttpDns = "com.squareup.okhttp3:okhttp-dnsoverhttps:$okhttpVersion"

    const val kotlinxSerialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0-RC"

    const val ktorVersion = "2.0.3"

    const val ziplineVersion = "0.9.0"

    val jakepurple13Libs = arrayOf(
        gsonutils,
        helpfulutils,
        loggingutils,
        dragswipe,
        funutils
    )

    val roomLibs = arrayOf(
        "androidx.room:room-runtime:$roomVersion",
        "androidx.room:room-ktx:$roomVersion",
    )

    val okHttpLibs = arrayOf(okhttpLib, okhttpDns)

    val koinLibs = arrayOf(koinAndroid, koinCompose)

    val composeLibs = arrayOf(
        composeUi, composeUiTooling, composeFoundation, composeMaterial,
        composeMaterialIconsCore, composeMaterialIconsExtended,
        composeActivity, composeLifecycle, composeLifecycleRuntime,
        composeRuntimeLivedata,
        composeMaterialThemeAdapter, composeMaterial3ThemeAdapter,
        landscapistGlide, coilCompose,
        composeConstraintLayout, permissions,
        materialPlaceholder, drawablePainter, uiUtil,
        materialYou,
        navCompose, navMaterial, navAnimation,
        swipeRefresh, systemUiController,
        flowLayout
    )

    val firebaseCrashLibs = arrayOf(crashlytics, analytics)

    val datastoreLibs = arrayOf(datastore, datastorePref)

    val ktorLibs = arrayOf(
        "io.ktor:ktor-client-core:$ktorVersion",
        "io.ktor:ktor-client-auth:$ktorVersion",
        "io.ktor:ktor-client-android:$ktorVersion",
        "io.ktor:ktor-client-logging:$ktorVersion",
        "io.ktor:ktor-client-serialization:$ktorVersion",
        "io.ktor:ktor-serialization-kotlinx-json:$ktorVersion",
        "io.ktor:ktor-client-content-negotiation:$ktorVersion",
        "io.ktor:ktor-client-okhttp:$ktorVersion",
        "io.ktor:ktor-serialization-gson:$ktorVersion",
        "com.tfowl.ktor:ktor-jsoup:2.0.0"
    )

    val ziplineLibs = arrayOf(
        "app.cash.zipline:zipline-loader:$ziplineVersion",
        "app.cash.zipline:zipline-profiler:$ziplineVersion"
    )
}

fun org.gradle.kotlin.dsl.DependencyHandlerScope.implementation(item: Array<String>) = item.forEach { add("implementation", it) }

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
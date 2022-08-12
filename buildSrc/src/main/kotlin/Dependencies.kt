object Deps {

    //TODO: create files/objects for each app
    // these will contain the specific items
    // MAYBE that will include a list so each implementation can just be the list

    const val kotlinVersion = "1.7.10"
    const val latestAboutLibsRelease = "10.3.1"

    const val kotlinStLib = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"

    private const val jakepurple13 = "10.6.5"

    const val flowutils = "com.github.jakepurple13.HelpfulTools:flowutils:$jakepurple13"
    const val gsonutils = "com.github.jakepurple13.HelpfulTools:gsonutils:$jakepurple13"
    const val helpfulutils = "com.github.jakepurple13.HelpfulTools:helpfulutils:$jakepurple13"
    const val loggingutils = "com.github.jakepurple13.HelpfulTools:loggingutils:$jakepurple13"
    const val dragswipe = "com.github.jakepurple13.HelpfulTools:dragswipe:$jakepurple13"
    const val funutils = "com.github.jakepurple13.HelpfulTools:funutils:$jakepurple13"
    const val thirdpartyutils = "com.github.jakepurple13.HelpfulTools:thirdpartyutils:$jakepurple13"

    const val palette = "androidx.palette:palette-ktx:1.0.0"

    const val coroutinesVersion = "1.6.4"

    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"

    const val gson = "com.google.code.gson:gson:2.9.1"

    const val glideVersion = "4.13.2"
    const val glide = "com.github.bumptech.glide:glide:$glideVersion"
    const val glideCompiler = "com.github.bumptech.glide:compiler:$glideVersion"
    const val glideRecyclerview = "com.github.bumptech.glide:recyclerview-integration:$glideVersion"

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

    const val roomVersion = "2.5.0-alpha02"

    const val navVersion = "2.5.1"

    const val koinVersion = "3.2.0"

    // Koin main features for Android (Scope,ViewModel ...)
    const val koinAndroid = "io.insert-koin:koin-android:$koinVersion"
    const val koinCompose = "io.insert-koin:koin-androidx-compose:$koinVersion"

    const val lottieVersion = "5.2.0"
    const val lottie = "com.airbnb.android:lottie:$lottieVersion"
    const val lottieCompose = "com.airbnb.android:lottie-compose:$lottieVersion"

    const val coil = "2.1.0"

    const val lifecycle = "2.5.1"

    const val lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"
    const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle"
    const val lifecycleLivedata = "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle"
    const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle"

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

    const val composePager = "com.google.accompanist:accompanist-pager:$accompanist"

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

    val jakepurple13Libs = arrayOf(
        gsonutils,
        helpfulutils,
        loggingutils,
        dragswipe,
        funutils
    )

    const val roomCompiler = "androidx.room:room-compiler:$roomVersion"

    val roomLibs = arrayOf(
        "androidx.room:room-runtime:$roomVersion",
        "androidx.room:room-ktx:$roomVersion",
    )

    val okHttpLibs = arrayOf(okhttpLib, okhttpDns)

    val koinLibs = arrayOf(koinAndroid, koinCompose)

    val composeLibs = arrayOf(
        composeUi, composeUiTooling, composeFoundation, composeMaterial,
        composeMaterialIconsCore, composeMaterialIconsExtended,
        composeAnimation,
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

    const val reactiveNetwork = "ru.beryukhov:flowreactivenetwork:1.0.4"

    const val fragmentKtx = "androidx.fragment:fragment-ktx:1.5.2"

    const val stetho = "com.facebook.stetho:stetho:1.6.0"

    const val workVersion = "2.7.1"

    // Kotlin + coroutines
    const val workRuntime = "androidx.work:work-runtime-ktx:$workVersion"

    const val iconicsCore = "com.mikepenz:iconics-core:5.3.4"
    const val materialTypeface = "com.mikepenz:google-material-typeface:4.0.0.2-kotlin@aar"
    const val fontawesomeTypeface = "com.mikepenz:fontawesome-typeface:5.13.3.0-kotlin@aar"

    const val androidxLegacySupport = "androidx.legacy:legacy-support-v4:1.0.0"

    const val firebaseAuth = "com.google.firebase:firebase-auth:21.0.7"
    const val googlePlayAds = "com.google.android.gms:play-services-ads:21.1.0"

    val pagingLibs = arrayOf(
        "androidx.paging:paging-runtime-ktx:$pagingVersion",
        "androidx.room:room-paging:2.4.3"
    )

    const val pagingCompose = "androidx.paging:paging-compose:1.0.0-alpha16"

    const val fileChooser = "com.github.hedzr:android-file-chooser:1.2.0"

    const val storage = "com.anggrayudi:storage:1.4.1"

    const val androidxWindow = "androidx.window:window:1.1.0-alpha03"
    const val androidBrowserHelper = "com.google.androidbrowserhelper:androidbrowserhelper:2.4.0"
    const val androidxBrowser = "androidx.browser:browser:1.4.0"

    const val fastScroll = "me.zhanghai.android.fastscroll:library:1.1.8"
    const val showMoreLess = "com.github.noowenz:ShowMoreLess:1.0.3"
    const val aboutLibrariesCore = "com.mikepenz:aboutlibraries-core:$latestAboutLibsRelease"
    const val aboutLibrariesCompose = "com.mikepenz:aboutlibraries-compose:$latestAboutLibsRelease"
    const val navFragment = "androidx.navigation:navigation-fragment-ktx:$navVersion"
    const val navUiKtx = "androidx.navigation:navigation-ui-ktx:$navVersion"
    const val navTesting = "androidx.navigation:navigation-testing:$navVersion"

    const val toolbarCompose = "me.onebone:toolbar-compose:2.3.4"
    const val lazyColumnScrollbar = "com.github.nanihadesuka:LazyColumnScrollbar:1.5.1"

}

object SharedDeps {
    val fullImplementation = arrayOf(
        "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:${Deps.coroutinesVersion}",
        "com.google.mlkit:translate:17.0.0", "com.google.mlkit:language-id:17.0.4",
        "com.google.firebase:firebase-database-ktx:20.0.5", "com.google.firebase:firebase-firestore-ktx:24.2.2",
        "com.firebaseui:firebase-ui-auth:8.0.1", Deps.firebaseAuth, Deps.playServices
    )
}

object SourceDeps {
    const val duktape = "com.squareup.duktape:duktape-android:1.4.0"
    const val rhino = "org.mozilla:rhino:1.7.14"

    const val kotson = "com.github.salomonbrys.kotson:kotson:2.5.0"
    const val karnKhttp = "io.karn:khttp-android:0.1.2"

    const val ziplineVersion = "0.9.0"

    val ziplineLibs = arrayOf(
        "app.cash.zipline:zipline-loader:$ziplineVersion",
        "app.cash.zipline:zipline-profiler:$ziplineVersion"
    )

    const val retrofit = "com.squareup.retrofit2:retrofit:2.9.0"
    const val retrofitGson = "com.squareup.retrofit2:converter-gson:2.9.0"

    const val kotlinxJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3"

}
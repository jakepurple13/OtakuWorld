@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-benchmark")
}

android {
    namespace = "com.programmersbox.mangaworldbaselineprofile"
    targetProjectPath = ":mangaworld"
}

dependencies {

}
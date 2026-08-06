pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        //maven { url "https://dl.bintray.com/piasy/maven" }
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://storage.googleapis.com/r8-releases/raw") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }
        maven("https://www.jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jogamp.org/deployment/maven")
    }

    versionCatalogs {
        create("androidx") {
            from("androidx.gradle:gradle-version-catalog:2026.05.00")
        }
        create("commonLibs") {
            from(files("gradle/common.versions.toml"))
        }
        create("androidLibs") {
            from(files("gradle/android.versions.toml"))
        }
        create("desktopLibs") {
            from(files("gradle/desktop.versions.toml"))
        }
        create("iosLibs") {
            from(files("gradle/ios.versions.toml"))
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

/*plugins {
    id("com.gradle.develocity") version ("3.18.1")
}

develocity {
    if (System.getenv("CI") != null) {
        buildScan {
            termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
            termsOfUseAgree.set("yes")
            publishing { onlyIf { true } }
        }
    }
}*/

fun includeIfLocal(block: () -> Unit) {
    if (System.getenv("CI") == null) block()
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    ":favoritesdatabase",
    ":favoritesdatabase:supabase-integration",
    //":anime_sources",
    //":manga_sources",
    //":novel_sources",
    //":app",
    ":Models",
    ":UIViews",
    ":animeworld",
    ":mangaworld",
    ":novelworld",
    //":animeworldtv",
    ":sharedutils",
    ":source_utilities",
    //":imageloader",
    //":otakumanager",
)

rootProject.name = "OtakuWorld"
/*include(
    ":novel_sources:novelupdates",
    ":novel_sources:bestlightnovel"
)*/
//include(":manga_sources:defaultmangasources")
//include(":anime_sources:defaultanimesources")
include(":MangaWorldbaselineprofile")
include(":AnimeWorldbaselineprofile")
include(":NovelWorldbaselineprofile")
include(":datastore")
include(":datastore:mangasettings")
include(":kmpuiviews")
include(":kmpmodels")
include(":kmpmodels:extensioninterfaces")
include(":sharedutils:kmpextensionloader")
include(":sharedutils:jsextensionloader")
include(":mangaworld:desktop")
include(":mangaworld:shared")
include(":novelworld:shared")
include(":novelworld:desktop")
include(":animeworld:shared")
include(":animeworld:desktop")
include(":kmpuiviews:koogintegration")
include(":sharedtools")
include(":sharedcomponents")
include(":showcase:annotations")
include(":showcase:processor")
includeIfLocal {
    include(":showcase")
    include(":kmpuiviews:koogintegration:customscraper")
}

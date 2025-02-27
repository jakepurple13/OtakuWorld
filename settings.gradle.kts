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
        maven("https://www.jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    versionCatalogs {
        create("androidx") {
            from("androidx.gradle:gradle-version-catalog:2025.02.01")
        }
    }
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    ":favoritesdatabase",
    ":anime_sources",
    //":manga_sources",
    //":novel_sources",
    //":app",
    ":Models",
    ":UIViews",
    ":animeworld",
    ":mangaworld",
    ":novelworld",
    ":animeworldtv",
    ":sharedutils",
    ":source_utilities",
    //":imageloader",
    ":sharedutils:extensionloader"
    //":otakumanager",
)

rootProject.name = "OtakuWorld"
/*include(
    ":novel_sources:novelupdates",
    ":novel_sources:bestlightnovel"
)*/
//include(":manga_sources:defaultmangasources")
//include(":anime_sources:defaultanimesources")
//include(":MangaWorldbaselineprofile")
include(":gemini")

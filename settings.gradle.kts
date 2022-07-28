dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
        //maven { url "https://dl.bintray.com/piasy/maven" }
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

include(
    ":favoritesdatabase",
    ":anime_sources",
    ":manga_sources",
    ":Models",
    ":UIViews",
    ":animeworld",
    ":mangaworld",
    ":app",
    ":novelworld",
    ":novel_sources",
    //":animeworldtv",
    ":sharedutils",
    //":otakumanager",
)

rootProject.name = "OtakuWorld"

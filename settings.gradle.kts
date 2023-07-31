enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
    ":animeworldtv",
    ":sharedutils",
    ":source_utilities",
    ":imageloader"
    //":otakumanager",
)

rootProject.name = "OtakuWorld"
include(
    ":novel_sources:novelupdates",
    ":novel_sources:bestlightnovel"
)
include(":sharedutils:extensionloader")
include(":manga_sources:defaultmangasources")
include(":anime_sources:defaultanimesources")

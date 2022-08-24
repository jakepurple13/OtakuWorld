enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

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
    //":otakumanager",
)

rootProject.name = "OtakuWorld"

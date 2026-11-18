import plugins.ProductFlavorTypes

plugins {
    id("otaku-library")
}

android {
    setFlavorDimensions(listOf(ProductFlavorTypes.dimension))
    productFlavors {
        ProductFlavorTypes.NoFirebase(this) {
            isDefault = true
        }
        ProductFlavorTypes.Full(this)
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
        ProductFlavorTypes.entries.forEach {
            getByName(it.nameType) {
                java.srcDirs("src/${it.nameType}/java")
            }
        }
    }
    namespace = "com.programmersbox.telemetry.android"
}

dependencies {
    api(projects.telemetry)
    implementation(platform(commonLibs.koin.bom))
    implementation(commonLibs.koinCores)

    fullImplementation(platform(androidLibs.firebasePlatform))
    fullImplementation(androidLibs.crashlytics)
    fullImplementation(androidLibs.analytics)
}

fun DependencyHandlerScope.fullImplementation(item: Any) =
    add("fullImplementation", item)

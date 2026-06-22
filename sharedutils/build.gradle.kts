import plugins.ProductFlavorTypes

plugins {
    id("otaku-library")
    id("kotlinx-serialization")
}

android {
    setFlavorDimensions(listOf(ProductFlavorTypes.dimension))
    productFlavors {
        ProductFlavorTypes.NoFirebase(this)
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
    namespace = "com.programmersbox.sharedutils"
}

dependencies {
    implementation(androidLibs.material)
    testImplementation(TestDeps.junit)
    testImplementation("com.jakewharton.picnic:picnic:0.7.0")
    testImplementation("com.lordcodes.turtle:turtle:0.10.0")
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    fullImplementation(androidLibs.mlkitTranslate)
    fullImplementation(androidLibs.mlkitLanguage)
    fullImplementation(platform(androidLibs.firebasePlatform))

    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.coroutinesAndroid)

    implementation(commonLibs.bundles.ktorLibs)

    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(platform(commonLibs.koin.bom))
    implementation(androidLibs.bundles.koinLibs)
    implementation(Deps.jakepurple13Libs)
    implementation(androidLibs.uiUtil)
}

fun DependencyHandlerScope.fullImplementation(item: Provider<MinimalExternalModuleDependency>) =
    add("fullImplementation", item)


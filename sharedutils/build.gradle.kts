import plugins.ProductFlavorTypes

plugins {
    id("otaku-library")
    id("kotlinx-serialization")
}

android {
    setFlavorDimensions(listOf(ProductFlavorTypes.dimension))
    productFlavors {
        ProductFlavorTypes.NoFirebase(this)
        ProductFlavorTypes.NoCloudFirebase(this)
        ProductFlavorTypes.Full(this)
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
        ProductFlavorTypes.values().forEach {
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

    noCloudFirebaseImplementation(androidLibs.mlkitTranslate)
    noCloudFirebaseImplementation(androidLibs.mlkitLanguage)
    noCloudFirebaseImplementation(androidLibs.playServices)
    noCloudFirebaseImplementation(androidLibs.coroutinesPlayServices)

    fullImplementation(androidLibs.mlkitTranslate)
    fullImplementation(androidLibs.mlkitLanguage)
    fullImplementation(platform(androidLibs.firebasePlatform))
    fullImplementation(androidLibs.firebaseDatabase)
    fullImplementation(androidLibs.firebaseFirestore)
    fullImplementation(androidLibs.firebaseAuth)
    fullImplementation(androidLibs.firebaseUiAuth)
    fullImplementation(androidLibs.playServices)
    fullImplementation(androidLibs.coroutinesPlayServices)

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

fun DependencyHandlerScope.noCloudFirebaseImplementation(item: Provider<MinimalExternalModuleDependency>) =
    add("noCloudFirebaseImplementation", item)

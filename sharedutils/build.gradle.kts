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
    implementation(libs.material)
    testImplementation(TestDeps.junit)
    testImplementation("com.jakewharton.picnic:picnic:0.7.0")
    testImplementation("com.lordcodes.turtle:turtle:0.10.0")
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    fullImplementation(libs.mlkitTranslate)
    fullImplementation(libs.mlkitLanguage)
    fullImplementation(platform(libs.firebasePlatform))
    fullImplementation(libs.firebaseDatabase)
    fullImplementation(libs.firebaseFirestore)
    fullImplementation(libs.firebaseAuth)
    fullImplementation(libs.firebaseUiAuth)
    fullImplementation(libs.playServices)
    fullImplementation(libs.coroutinesPlayServices)
    fullImplementation(libs.firebase.database.ktx)

    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)

    implementation(libs.bundles.ktorLibs)

    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koinLibs)
    implementation(Deps.jakepurple13Libs)
    implementation(libs.uiUtil)
}

fun DependencyHandlerScope.fullImplementation(item: Provider<MinimalExternalModuleDependency>) = add("fullImplementation", item)

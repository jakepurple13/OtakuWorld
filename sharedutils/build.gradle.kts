import plugins.ProductFlavorTypes

plugins {
    id("otaku-library")
    kotlin("kapt")
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
        getByName("full") {
            java.srcDirs("src/full/java")
        }
        getByName("noFirebase") {
            java.srcDirs("src/noFirebase/java")
        }
    }
    namespace = "com.programmersbox.sharedutils"
}

dependencies {
    implementation(libs.material)
    testImplementation(TestDeps.junit)
    testImplementation("com.jakewharton.picnic:picnic:0.7.0")
    testImplementation("com.lordcodes.turtle:turtle:0.9.0")
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    fullImplementation(libs.mlkitTranslate)
    fullImplementation(libs.mlkitLanguage)
    fullImplementation(libs.firebaseDatabase)
    fullImplementation(libs.firebaseFirestore)
    fullImplementation(libs.firebaseAuth)
    fullImplementation(libs.firebaseUiAuth)
    fullImplementation(libs.playServices)
    fullImplementation(libs.coroutinesPlayServices)

    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)

    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(libs.bundles.koinLibs)
    implementation(Deps.jakepurple13Libs)
    implementation(libs.uiUtil)
}

fun DependencyHandlerScope.fullImplementation(item: Provider<MinimalExternalModuleDependency>) = add("fullImplementation", item)

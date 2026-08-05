// bare id(...) — buildSrc convention plugins already apply this plugin unversioned; alias(libs.plugins.*) conflicts with that.
plugins {
    id("kotlin-multiplatform")
}

kotlin {
    jvmToolchain(21)
    jvm()
}

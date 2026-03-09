plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "stub"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

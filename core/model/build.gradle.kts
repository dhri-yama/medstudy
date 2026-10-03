plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.medstudy.core.model"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
}

dependencies {
    testImplementation(libs.junit)
}

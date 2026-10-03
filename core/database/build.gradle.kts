plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.medstudy.core.database"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":core:model"))
}

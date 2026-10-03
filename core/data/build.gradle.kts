plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.medstudy.core.data"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    testImplementation(libs.junit)
}

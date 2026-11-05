import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Version comes from version.properties (see tools/bump-version.sh); the code is derived so it can never be forgotten.
val versionProps = Properties().apply { rootProject.file("version.properties").reader().use(::load) }
val appVersionName: String = versionProps.getProperty("VERSION_NAME")
val appVersionCode: Int = appVersionName.split("-").first().split(".").map(String::toInt)
    .let { (major, minor, patch) -> major * 10000 + minor * 100 + patch }
val gitSha: String = runCatching {
    providers.exec { commandLine("git", "rev-parse", "--short", "HEAD"); isIgnoreExitValue = true }.standardOutput.asText.get().trim()
}.getOrDefault("").ifEmpty { "unknown" }

android {
    namespace = "com.medstudy.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.medstudy.app"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")
        val local = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use(::load)
        }
        buildConfigField("String", "SUPABASE_URL", "\"${local.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${local.getProperty("SUPABASE_ANON_KEY", "")}\"")
    }

    buildTypes {
        // Previous-year papers are flagged REVIEW_REQUIRED (recalled content from a coaching provider); release builds leave them out until that is settled.
        // DEV_SKIP_AUTH=true in local.properties skips sign-up/sign-in in debug builds (no account, no server trial, full access). Never honoured in release.
        debug {
            buildConfigField("boolean", "INCLUDE_UNREVIEWED_PYQ", "true")
            val local = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use(::load) }
            buildConfigField("boolean", "DEV_SKIP_AUTH", (local.getProperty("DEV_SKIP_AUTH", "false") == "true").toString())
        }
        release {
            buildConfigField("boolean", "INCLUDE_UNREVIEWED_PYQ", "false")
            buildConfigField("boolean", "DEV_SKIP_AUTH", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:progress"))
    implementation(project(":feature:practice"))
    implementation(project(":feature:progress"))
    
    // Gson for JSON parsing (MedMCQA data loading)
    implementation("com.google.code.gson:gson:2.10.1")
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation(libs.androidx.compose.icons)
    implementation(libs.android.billing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}

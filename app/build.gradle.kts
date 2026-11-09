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
        // The Google Cloud *Web* OAuth client id (the same one pasted into Supabase > Auth > Providers > Google). Blank = Google sign-in disabled.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${local.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
    }

    buildTypes {
        // Previous-year papers are flagged REVIEW_REQUIRED (recalled content from a coaching provider); release builds leave them out until that is settled.
        // Sign-in is required by default. For local testing only, build with  ./gradlew :app:installDebug -PDEV_SKIP_AUTH=true
        // to skip sign-up/sign-in (no account, no server trial, full access). It is deliberately NOT read from local.properties,
        // so an APK built normally can never have it switched on by accident. Never honoured in release.
        debug {
            buildConfigField("boolean", "INCLUDE_UNREVIEWED_PYQ", "true")
            buildConfigField("boolean", "DEV_SKIP_AUTH", (providers.gradleProperty("DEV_SKIP_AUTH").orNull == "true").toString())
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
    
    // Friendly blob avatars (profile picture)
    implementation("com.github.dhri-yama.blobmates:blobmates-android:v0.3.0")

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
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}

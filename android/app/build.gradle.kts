import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun ensureTrailingSlash(url: String): String =
    if (url.endsWith("/")) url else "$url/"

fun readApiBaseUrl(): String {
    val local = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use {
        local.load(it)
    }
    local.getProperty("DEMO_API_URL")?.trim()?.takeIf { it.isNotEmpty() }?.let {
        return ensureTrailingSlash(it)
    }

    val defaults = Properties()
    rootProject.file("api.defaults.properties").takeIf { it.exists() }?.inputStream()?.use {
        defaults.load(it)
    }
    defaults.getProperty("PRODUCTION_API_URL")?.trim()?.takeIf { it.isNotEmpty() }?.let {
        if (!it.contains("your-project") && !it.contains("CHANGE-ME")) {
            return ensureTrailingSlash(it)
        }
    }

    return "http://10.0.2.2:3000/"
}

android {
    namespace = "com.athlete.monitoring"
    compileSdk = 35

    val apiBaseUrl = readApiBaseUrl()

    defaultConfig {
        applicationId = "com.athlete.monitoring"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.1-demo"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

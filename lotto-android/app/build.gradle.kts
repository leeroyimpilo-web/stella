plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "za.co.lottoinsight"
    compileSdk = 35
    defaultConfig {
        applicationId = "za.co.lottoinsight"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        val baseUrl = System.getenv("LOTTO_LICENSE_SERVER_URL") ?: "https://example.invalid"
        val publicKey = System.getenv("LOTTO_LICENSE_PUBLIC_KEY_B64") ?: ""
        buildConfigField("String", "LICENSE_SERVER_URL", "\\\"${baseUrl}\\\"")
        buildConfigField("String", "LICENSE_PUBLIC_KEY_B64", "\\\"${publicKey}\\\"")
    }
    signingConfigs {
        create("retail") {
            storeFile = file(System.getenv("LOTTO_KEYSTORE_FILE") ?: "signing-not-configured.jks")
            storePassword = System.getenv("LOTTO_STORE_PASSWORD")
            keyAlias = System.getenv("LOTTO_KEY_ALIAS")
            keyPassword = System.getenv("LOTTO_KEY_PASSWORD")
        }
    }
    buildTypes {
        debug {
            // Development APK remains unlocked for internal testing only.
            buildConfigField("boolean", "LICENSE_REQUIRED", "false")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "LICENSE_REQUIRED", "true")
            signingConfig = signingConfigs.getByName("retail")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

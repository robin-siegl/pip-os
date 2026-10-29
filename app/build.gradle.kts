plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseVersion = providers.gradleProperty("releaseVersion").orNull
val releaseVersionCode = providers.gradleProperty("releaseVersionCode").orNull?.toInt()

require(releaseVersion == null || Regex("[0-9]+\\.[0-9]+\\.[0-9]+").matches(releaseVersion)) {
    "releaseVersion must be a semantic version such as 0.2.0"
}
require((releaseVersion == null) == (releaseVersionCode == null)) {
    "releaseVersion and releaseVersionCode must be supplied together"
}

android {
    namespace = "dev.pipos.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.pipos.launcher"
        minSdk = 29
        targetSdk = 35
        versionCode = releaseVersionCode ?: 2
        versionName = releaseVersion ?: "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("production") {
            val keyStorePath = System.getenv("PIPOS_KEYSTORE_PATH")
            if (!keyStorePath.isNullOrBlank()) {
                storeFile = file(keyStorePath)
                storePassword = System.getenv("PIPOS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PIPOS_KEY_ALIAS")
                keyPassword = System.getenv("PIPOS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Local release builds may be unsigned. The release workflow requires the signing secrets.
            if (!System.getenv("PIPOS_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("production")
            }
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

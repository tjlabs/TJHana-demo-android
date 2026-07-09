import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}


val versionMajor = 1
val versionMinor = 0
val versionPatch = 3
val computedVersionCode = (versionMajor * 100) + (versionMinor * 10) + versionPatch
val computedVersionName = "$versionMajor.$versionMinor.$versionPatch"

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val authAccessKey = (
        providers.gradleProperty("AUTH_ACCESS_KEY").orNull
            ?: localProperties.getProperty("AUTH_ACCESS_KEY", "")
        ).trim()

val authSecretAccessKey = (
        providers.gradleProperty("AUTH_SECRET_ACCESS_KEY").orNull
            ?: localProperties.getProperty("AUTH_SECRET_ACCESS_KEY", "")
        ).trim()


fun String.toBuildConfigString(): String = this.replace("\\", "\\\\").replace("\"", "\\\"")

val releaseStoreFilePath: String = providers.gradleProperty("RELEASE_STORE_FILE").orNull
    ?: localProperties.getProperty("RELEASE_STORE_FILE", "")
val releaseStorePassword: String = providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
    ?: localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
val releaseKeyAlias: String = providers.gradleProperty("RELEASE_KEY_ALIAS").orNull
    ?: localProperties.getProperty("RELEASE_KEY_ALIAS", "")
val releaseKeyPassword: String = providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull
    ?: localProperties.getProperty("RELEASE_KEY_PASSWORD", "")


val releaseStoreFile = if (releaseStoreFilePath.isNotBlank()) rootProject.file(releaseStoreFilePath) else null
val canUseReleaseSigning = releaseStoreFile?.exists() == true &&
        releaseStorePassword.isNotBlank() &&
        releaseKeyAlias.isNotBlank() &&
        releaseKeyPassword.isNotBlank()


val jupiterSdkVersion = "2.0.14"

val syncReadmeVersions by tasks.registering {
    group = "documentation"
    description = "Synchronize README SDK/AAR/dependency snippets with app/build.gradle.kts values."

    doLast {
        val readme = rootProject.file("README.md")
        if (!readme.exists()) return@doLast

        var updated = readme.readText()

        updated = updated.replace(
            Regex("(?s)(<!-- JUPITER_SDK_VERSION_START -->\\s*).*?(\\s*<!-- JUPITER_SDK_VERSION_END -->)"),
            "$1Jupiter SDK version: $jupiterSdkVersion$2"
        )

        readme.writeText(updated)
    }
}

android {
    namespace = "com.tjlabs.tjhana_demo_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tjlabs.tjhana_demo_android"
        minSdk = 29
        targetSdk = 36
        versionCode = computedVersionCode
        versionName = computedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AUTH_ACCESS_KEY", "\"${authAccessKey.toBuildConfigString()}\"")
        buildConfigField("String", "AUTH_SECRET_ACCESS_KEY", "\"${authSecretAccessKey.toBuildConfigString()}\"")

    }

    signingConfigs {
        create("release") {
            val storeFilePath = releaseStoreFilePath.trim()
            storeFile = when {
                storeFilePath.isBlank() -> null
                storeFilePath.startsWith("/") -> file(storeFilePath)
                else -> rootProject.file(storeFilePath)
            }
            storePassword = releaseStorePassword.trim().ifEmpty { null }
            keyAlias = releaseKeyAlias.trim().ifEmpty { null }
            keyPassword = releaseKeyPassword.trim().ifEmpty { null }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (canUseReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
    }


}

dependencies {
    implementation("com.github.tjlabs:TJHana-sdk-android:1.0.6")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.named("preBuild") {
    dependsOn(syncReadmeVersions)
}

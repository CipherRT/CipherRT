import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Release signing is read from GymPlanner/keystore.properties (never committed).
// See README.md → "Release build" for how to create it.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.gymplanner.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gymplanner.app"
        minSdk = 26
        targetSdk = 36
        // v2 must install over v1: same applicationId, same signing key, higher versionCode.
        versionCode = 2
        versionName = "2.0"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        // Robolectric runs Room's MigrationTestHelper on the JVM, reading the exported schemas.
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric downloads its Android runtime from Maven Central; a machine that
            // can't reach it can set robolectricRepo=<mirror> in ~/.gradle/gradle.properties.
            it.systemProperty(
                "robolectric.dependency.repo.url",
                providers.gradleProperty("robolectricRepo").getOrElse("https://repo1.maven.org/maven2"),
            )
            // ./gradlew testDebugUnitTest -Pscreenshots renders screens to app/build/screenshots
            it.systemProperty("screenshots", providers.gradleProperty("screenshots").isPresent.toString())
            if (providers.gradleProperty("screenshots").isPresent) it.forkEvery = 1
            it.systemProperty("screenshotDir", layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
        }
    }
    sourceSets {
        // MigrationTest (Robolectric) reads Room's exported schemas from the debug assets.
        // Release builds don't include them.
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    // Room writes each database version's schema here so migrations can be checked.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
}

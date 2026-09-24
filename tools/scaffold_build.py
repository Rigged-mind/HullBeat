#!/usr/bin/env python3
"""Emit Gradle build config and the manifest. Run once from the repo root."""
import io
import os

F = {}

F["settings.gradle.kts"] = """
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "SailboatLog"
include(":app")
""".lstrip()

F["gradle.properties"] = """
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
""".lstrip()

F["build.gradle.kts"] = """
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
""".lstrip()

F["gradle/libs.versions.toml"] = """
# Versions are plausible for late 2026 but were NOT resolved offline.
# Run `./gradlew dependencies` once online and bump anything Gradle rejects.
[versions]
agp = "8.9.0"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
room = "2.7.0"
composeBom = "2025.01.00"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
navigation = "2.8.5"
workManager = "2.10.0"
serialization = "1.7.3"
documentfile = "1.0.1"
mlkitText = "16.0.1"
mlkitBarcode = "17.3.0"
junit = "4.13.2"
androidxTestJunit = "1.2.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }
androidx-documentfile = { group = "androidx.documentfile", name = "documentfile", version.ref = "documentfile" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }
mlkit-text-recognition = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkitText" }
mlkit-barcode-scanning = { group = "com.google.mlkit", name = "barcode-scanning", version.ref = "mlkitBarcode" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-test-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestJunit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
""".lstrip()

F["app/build.gradle.kts"] = """
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.hullbeat"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.hullbeat"
        minSdk = 26          // SAF, notification actions and java.time all available
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

ksp {
    // Schemas are committed so every migration can be tested against the real one.
    arg("room.schemaLocation", "$projectDir/schemas")
}

// The catalog is authored in /catalog and copied into assets at build time.
// Never edit the copy under src/main/assets - it is overwritten.
val copyCatalog = tasks.register<Copy>("copyCatalog") {
    from(rootProject.file("catalog/catalog.json"))
    into(layout.projectDirectory.dir("src/main/assets/catalog"))
}
tasks.named("preBuild") { dependsOn(copyCatalog) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.barcode.scanning)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.room.testing)
}
""".lstrip()

F["app/src/main/AndroidManifest.xml"] = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Android 13+ requires runtime consent before any reminder can be shown. -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera.any" android:required="false" />

    <!--
      No INTERNET permission, deliberately. The app has no backend and this makes
      that provable to a suspicious owner: Play shows the permission list, and
      "no network access" is the claim our whole positioning rests on.
      Backups are written to a folder the owner picks through SAF.
    -->

    <application
        android:name=".SailboatApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.SailboatLog">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.SailboatLog">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!--
          Writes a service record straight from the notification action, without
          opening the app. This is the two-tap logging path from ui-spec.md p.4.
        -->
        <receiver
            android:name=".notify.MarkDoneReceiver"
            android:exported="false" />

    </application>
</manifest>
""".lstrip()

if __name__ == "__main__":
    for path, body in F.items():
        directory = os.path.dirname(path)
        if directory:
            os.makedirs(directory, exist_ok=True)
        io.open(path, "w", encoding="utf-8", newline="\n").write(body)
        print(f"{len(body):6d}B  {path}")

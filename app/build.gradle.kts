plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val releaseStoreFile = providers.gradleProperty("FALLASLEEP_STORE_FILE").orNull
val releaseStorePassword = providers.gradleProperty("FALLASLEEP_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.gradleProperty("FALLASLEEP_KEY_ALIAS").orNull
val releaseKeyPassword = providers.gradleProperty("FALLASLEEP_KEY_PASSWORD").orNull
val previewRelease = providers.gradleProperty("FALLASLEEP_PREVIEW").orNull == "true"
val hasReleaseSigning = releaseStoreFile != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

android {
    namespace = "io.github.fallasleeptimetable"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.tukinokilyowsoo.fallasleeptimetable"
        minSdk = 26
        targetSdk = 36
        versionCode = 29
        versionName = "1.2.2"

        ndk {
            abiFilters += listOf("arm64-v8a")
            if (previewRelease) abiFilters += listOf("x86_64")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (previewRelease) {
                applicationIdSuffix = ".hbvtcfix"
                versionNameSuffix = "-beta1"
                resValue("string", "app_name", "FallAsleep 课表修复试用版")
                signingConfig = signingConfigs.getByName("debug")
            } else if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.02.00"))

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation("org.jsoup:jsoup:1.18.3")

    implementation("androidx.glance:glance-appwidget:1.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

plugins {
    id ("com.android.application")
    id ("com.google.gms.google-services")
    id ("kotlin-android")
}

val envFile = rootProject.file(".env").readText().trim().split("\n")
val env = LinkedHashMap<String, String>()
for (i in envFile) {
    env[i.trim().split("=")[0]] = i.trim().split("=")[1]
}

android {
    compileSdk = 34
    buildToolsVersion = "33.0.2"

    defaultConfig {
        applicationId = "com.vineelsai.rootchecker"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        env["DECRYPTION_KEY"]?.let { resValue("string", "DECRYPTION_KEY", it) }
        env["VERIFICATION_KEY"]?.let { resValue("string", "VERIFICATION_KEY", it) }
    }
    buildFeatures {
        viewBinding = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.6"
    }
    bundle {
        storeArchive {
            enable = true
        }
    }
    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }

    namespace ="com.vineelsai.rootchecker"
}

dependencies {
    // UI
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase
    implementation("com.google.firebase:firebase-bom:32.2.0")
    implementation("com.google.firebase:firebase-analytics-ktx:21.3.0")

    // 3rd Party
    implementation("eu.chainfire:libsuperuser:1.1.0")
    implementation(group="org.bitbucket.b_c", name="jose4j", version="0.7.12")

    // Google
    implementation("com.google.android.gms:play-services-safetynet:18.0.1")
    implementation("com.google.android.play:integrity:1.1.0")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}


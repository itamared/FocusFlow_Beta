plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.focusflow_beta"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.focusflow_beta"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}


dependencies {
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.material)


    implementation(platform("com.google.firebase:firebase-bom:32.1.0")) // בדוק גרסה עדכנית
    implementation("com.google.firebase:firebase-auth")    // Authentication
    implementation("com.google.firebase:firebase-firestore") // Firestore DB

    // 📊 ספרייה לגרפים
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    // Add the Google services Gradle plugin
//    id("com.google.gms.google-services")
}

android {
    namespace = "kr.co.supermakers.rms"
    compileSdk = 34

    defaultConfig {
        applicationId = "kr.co.supermakers.rms"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            applicationIdSuffix = ".prod"
            versionNameSuffix = "-prod"
            buildConfigField("String","MAIN_URL","\"https://retail.superkitchen.co.kr\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }
        debug {
            isMinifyEnabled = false
//            applicationIdSuffix = ".dev"
            versionNameSuffix = "-demo"
            buildConfigField("String","MAIN_URL","\"https://dev-retail.superkitchen.kr\"")

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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //WebView
    implementation("androidx.webkit:webkit:1.11.0")

    //Barcode scanner
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    //Permission
    implementation("io.github.ParkSangGwon:tedpermission-normal:3.3.0")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-inappmessaging-ktx")
    implementation("com.google.firebase:firebase-inappmessaging-display-ktx")

    //FCM
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation ("com.google.firebase:firebase-messaging-directboot:20.2.0")
}
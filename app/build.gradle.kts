plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

val tomtomApiKey: String by project

android {
    namespace = "com.meat.meatdash"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.meat.meatdash"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }


        buildFeatures {
            buildConfig = true
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
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        kotlinOptions {
            jvmTarget = "17"
        }
        buildFeatures {
            viewBinding = true
        }
    }

    dependencies {
        // Core Android
        implementation("androidx.core:core-ktx:1.12.0")
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("com.google.android.material:material:1.10.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.activity:activity-ktx:1.8.0")
        implementation("androidx.fragment:fragment-ktx:1.6.2")

        // Lifecycle
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

        // Navigation
        implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
        implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

        // Firebase
        implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.google.firebase:firebase-firestore-ktx")

        // Google Sign-In
        implementation("com.google.android.gms:play-services-auth:20.7.0")

        // UI Libraries
        implementation("com.github.vedraj360:DesignerToast:0.1.3")
        implementation("com.github.denzcoskun:ImageSlideshow:0.1.0")
        implementation("com.github.bumptech.glide:glide:4.15.1")
        implementation(libs.androidx.activity)
        implementation(libs.support.annotations)
        kapt("com.github.bumptech.glide:compiler:4.15.1")

        // Data
        implementation("com.google.code.gson:gson:2.10.1")
        implementation("androidx.room:room-runtime:2.6.1")
        implementation("androidx.room:room-ktx:2.6.1")
        kapt("androidx.room:room-compiler:2.6.1")

        // DI
        implementation("com.google.dagger:hilt-android:2.48")
        kapt("com.google.dagger:hilt-compiler:2.48")

        // Payment gateway razorpay
        implementation("com.razorpay:checkout:1.6.40")

        // Testing
        testImplementation("junit:junit:4.13.2")
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

        // location
        implementation("com.google.android.gms:play-services-location:21.0.1")

        implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
        implementation("de.hdodenhof:circleimageview:3.1.0")

        // country code picker
        implementation("com.hbb20:ccp:2.6.1")

        // circular imageview
        implementation("de.hdodenhof:circleimageview:3.1.0")

        implementation("com.google.android.gms:play-services-base:18.4.0")
        implementation("com.google.android.gms:play-services-basement:18.4.0")

        // retrofit
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")


        // Kotlin Coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")


        // Google Play services for Maps & Location
        implementation("com.google.android.gms:play-services-maps:18.1.0")

        // (Optional) Google Maps Utils for polyline decoding, markers, etc.
        implementation("com.google.maps.android:android-maps-utils:2.3.0")

        // Retrofit–Moshi converter
        implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

        // Moshi JSON library
        implementation("com.squareup.moshi:moshi:1.14.0")
        implementation("com.squareup.moshi:moshi-kotlin:1.14.0")

        // (Optional, for codegen of adapters)
        kapt("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")


    }

    //  Java 17 compatibility
    kapt {
        correctErrorTypes = true
        javacOptions {
            option("-Adagger.hilt.android.internal.disableAndroidSuperclassValidation=true")
        }
    }
}
dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
}

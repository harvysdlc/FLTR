plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.fltr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fltr"
        minSdk = 24
        targetSdk = 34
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
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") // <== includes FileUtil + TensorBuffer
    implementation("org.tensorflow:tensorflow-lite-gpu:2.13.0") // optional for GPU inference
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.13.0")
    implementation("com.github.wendykierp:JTransforms:3.1") // MFCC support

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

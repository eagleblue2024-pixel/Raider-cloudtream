plugins {
    id("com.android.library") version "8.2.2"
    id("kotlin-android") version "1.9.22"
    id("com.lagradost.cloudstream3.gradle") version "0.1.0"
}

android {
    namespace = "com.raiderlatino"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    val cloudstreamVersion = "pre-release"
    implementation("com.lagradost:cloudstream3:$cloudstreamVersion")
    implementation("org.jsoup:jsoup:1.17.2")
}

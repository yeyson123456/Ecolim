plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.ecolim"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ecolim"
        minSdk = 29
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

    // Evita clases duplicadas de xmlbeans (Apache POI)
    configurations.all {
        exclude(group = "org.apache.xmlbeans", module = "xmlbeans")
    }

    // ✅ NECESARIO para que Apache POI compile sin errores
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
        }
    }
}

dependencies {

    // AndroidX base
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // UI Components
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.fragment:fragment:1.6.2")

    // Graficos (HomeFragment / ReportesFragment)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Excel export (ReportesFragment)
    implementation("org.apache.poi:poi-ooxml:3.17") {
        exclude(group = "org.apache.xmlbeans", module = "xmlbeans")
    }

    // Biometria (ConfiguracionFragment)
    implementation("androidx.biometric:biometric:1.1.0")
}
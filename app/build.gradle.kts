import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.io.FileInputStream

// ============================================================
// app/build.gradle.kts  —  MÓDULO DO APP
// ============================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun generateVersionCode(): Int {
    val formatter = DateTimeFormatter.ofPattern("yyMMddHH")
    return LocalDateTime.now().format(formatter).toInt()
}

android {
    namespace  = "com.krono.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.krono.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = generateVersionCode()
        versionName = "3.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // Carrega as propriedades do keystore.properties na raiz do projeto
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            // Prioridade 1: Variáveis de Ambiente (CI/CD no GitHub)
            // Prioridade 2: keystore.properties (Desenvolvimento Local no Windows)

            val envKeyPath = System.getenv("KEYSTORE_PATH")
            // Modificação pontual: limpa aspas e trata nulos para evitar erros de path no Windows
            val propKeyPath = keystoreProperties.getProperty("storeFile")?.replace("\"", "")

            val storeFilePath = envKeyPath ?: propKeyPath

            if (!storeFilePath.isNullOrEmpty()) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProperties.getProperty("storePassword")
                keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProperties.getProperty("keyAlias")
                keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true

            // Aplica a assinatura configurada acima
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled     = false
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
            .forEach { output ->
                val name = "krono-v${variant.versionName}.apk"
                output.outputFileName = name
            }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.savedstate)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.skydoves.colorpicker.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    implementation(libs.androidx.navigation.compose)
}
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ============================================================
// app/build.gradle.kts  —  MÓDULO DO APP
// ============================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ── versionCode dinâmico ─────────────────────────────────────
// Formato: yyMMddHHmm
// Exemplo: 25 04 03 14 30 → 2504031430
// Sempre cresce com o tempo — nunca precisa ser incrementado manualmente.
// Cabe dentro do limite de Int da Play Store (máx: 2.100.000.000).
// O maior valor possível em 2099 seria: 9912312359 → excede Int!
// Por isso usamos apenas ano com 2 dígitos (yy), garantindo
// que o valor máximo seja 9912312359... mas espera:
// 99_12_31_23_59 = 9912312359 > 2.147.483.647 (Int.MAX_VALUE)
// Solução: removemos os minutos e usamos yyMMddHH (8 dígitos)
// Exemplo: 25 04 03 14 → 25040314 — sempre dentro do limite.
fun generateVersionCode(): Int {
    val formatter = DateTimeFormatter.ofPattern("yyMMddHH")
    return LocalDateTime.now().format(formatter).toInt()
}

android {
    namespace  = "com.gustavo.cronometro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gustavo.cronometro"
        minSdk        = 26
        targetSdk     = 35

        // ── versionCode: gerado automaticamente ──────────────
        // Não edite manualmente — cresce a cada build.
        versionCode = generateVersionCode()

        // ── versionName: gerenciado pelo commit-and-tag-version
        // Para atualizar, use: npm run release
        // O script scripts/update-version.js substitui este valor.
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
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
                val name = "cronometro-flutuante-v${variant.versionName}.apk"
                output.outputFileName = name
            }
    }
}

// ── Substitui kotlinOptions { jvmTarget } deprecado ──────────
// kotlinOptions foi deprecado no Kotlin 2.0.
// A forma correta agora é kotlin { compilerOptions { } }.
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
}
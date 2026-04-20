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

fun generateVersionCode(): Int {
    val formatter = DateTimeFormatter.ofPattern("yyMMddHH")
    return LocalDateTime.now().format(formatter).toInt()
}

// ── Extrai o changelog da versão atual para res/raw/changelog.md ──
tasks.register("extractChangelog") {
    group       = "build"
    description = "Extrai o changelog da versão atual do CHANGELOG.md para res/raw/changelog.md"

    doLast {
        val changelogFile = rootProject.file("CHANGELOG.md")
        val rawDir        = file("src/main/res/raw")
        val outputFile    = file("src/main/res/raw/changelog.md")

        if (!changelogFile.exists()) {
            println("⚠️  CHANGELOG.md não encontrado na raiz do projeto — pulando extração.")
            return@doLast
        }

        // Lê a versão atual do build.gradle.kts
        val currentVersion = android.defaultConfig.versionName
            ?: run {
                println("⚠️  versionName não definido — pulando extração.")
                return@doLast
            }

        rawDir.mkdirs()

        val lines   = changelogFile.readLines()
        val content = StringBuilder()
        var inside  = false

        for (line in lines) {
            // Detecta cabeçalho da versão: ## [2.3.0] ou ## [v2.3.0]
            if (line.trimStart().startsWith("## [")) {
                if (inside) break          // chegou na versão anterior — para
                val lineVersion = line
                    .removePrefix("## [").removePrefix("v")
                    .substringBefore("]").trim()
                if (lineVersion == currentVersion ||
                    lineVersion == currentVersion.removePrefix("v")) {
                    inside = true
                    continue               // pula o cabeçalho da versão
                }
            } else if (inside) {
                content.appendLine(line)
            }
        }

        val result = content.toString().trim()

        if (result.isEmpty()) {
            println("⚠️  Nenhuma entrada encontrada para versão $currentVersion no CHANGELOG.md")
            outputFile.writeText("- Sem notas para esta versão.")
        } else {
            outputFile.writeText(result)
            println("✅ changelog.md gerado para versão $currentVersion (${result.lines().size} linhas)")
        }
    }
}

// Garante que o changelog é extraído antes de qualquer build
tasks.whenTaskAdded {
    if (name.startsWith("generate") && name.contains("ResValues")) {
        dependsOn("extractChangelog")
    }
    if (name == "preBuild") {
        dependsOn("extractChangelog")
    }
}

android {
    namespace  = "com.krono.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.krono.app"
        minSdk        = 26
        targetSdk     = 35

        // ── versionCode: gerado automaticamente ──────────────
        // Não edite manualmente — cresce a cada build.
        versionCode = generateVersionCode()

        // ── versionName: gerenciado pelo commit-and-tag-version
        // Para atualizar, use: npm run release
        // O script scripts/update-version.js substitui este valor.
        versionName = "2.4.7"

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
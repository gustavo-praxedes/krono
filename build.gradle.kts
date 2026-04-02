// ============================================================
// build.gradle.kts — Raiz do Projeto
// Declara os plugins do projeto. Não adicione dependências
// de bibliotecas aqui — isso vai em app/build.gradle.kts.
// ============================================================

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    // Kotlin 2.0+: plugin separado para o compilador Compose
    alias(libs.plugins.kotlin.compose)      apply false
}
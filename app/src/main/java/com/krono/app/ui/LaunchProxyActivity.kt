package com.krono.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.krono.app.data.OverlayDataStore
import com.krono.app.service.MainService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// ============================================================
// LaunchProxyActivity.kt — GIT 6  (versão final com DataStore)
// ============================================================

class LaunchProxyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val autoLaunch = runBlocking {
            OverlayDataStore(this@LaunchProxyActivity).configFlow.first().autoLaunch
        }

        if (autoLaunch && Settings.canDrawOverlays(this)) {
            // "Abrir Diretamente" ativo + permissão OK → só o overlay
            startForegroundService(Intent(this, MainService::class.java))
        } else {
            // Qualquer outro caso → abre o app normalmente
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
        }

        finish()
    }
}
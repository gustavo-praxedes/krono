package com.krono.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.krono.app.ACTION_SHOW_OVERLAY
import com.krono.app.data.OverlayConfig
import com.krono.app.data.OverlayDataStore
import com.krono.app.service.MainService
import com.krono.app.ui.theme.KronoTheme
import kotlinx.coroutines.launch

class DonationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dataStore = OverlayDataStore(this)
        val isManual = intent?.getBooleanExtra("manual_trigger", false) == true
        val restoreOverlay = intent?.getBooleanExtra("restore_overlay", false) == true

        setContent {
            val config by dataStore.configFlow.collectAsState(initial = OverlayConfig())

            KronoTheme(selectedTheme = config.selectedTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    DonationDialog(
                        onDismiss = {
                            lifecycleScope.launch {
                                dataStore.resetDonationCycle()
                                if (!isManual && restoreOverlay) {
                                    startService(Intent(this@DonationActivity, MainService::class.java).apply { action = ACTION_SHOW_OVERLAY })
                                }
                                finish()
                            }
                        },
                        onDonate = {
                            lifecycleScope.launch {
                                dataStore.resetDonationCycle()
                                if (!isManual && restoreOverlay) {
                                    startService(Intent(this@DonationActivity, MainService::class.java).apply { action = ACTION_SHOW_OVERLAY })
                                }
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
}

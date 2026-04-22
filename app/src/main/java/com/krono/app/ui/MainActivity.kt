package com.krono.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.krono.app.ACTION_RESET
import com.krono.app.ACTION_SHOW_OVERLAY
import com.krono.app.BuildConfig
import com.krono.app.EXTRA_SHOW_DONATION
import com.krono.app.KronoApp
import com.krono.app.data.OverlayConfig
import com.krono.app.data.OverlayDataStore
import com.krono.app.service.MainService
import com.krono.app.ui.theme.KronoTheme
import com.krono.app.util.UpdateInfo
import com.krono.app.util.UpdateResult
import com.krono.app.util.checkForUpdate
import com.krono.app.viewmodel.TimerViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val navigationEvents              = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val permissionsDialogEvents       = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private lateinit var dataStore: OverlayDataStore
    private val timerViewModel: TimerViewModel
        get() = (application as KronoApp).timerViewModel

    private val pendingUpdateInfo = MutableStateFlow<UpdateInfo?>(null)

    // Lançador para permissão de notificação (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* AppNavigation relê o estado via hasNotificationPermission */ }

    // Lançador para permissão de overlay — ao retornar, minimiza se concedida
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            startServiceAndMinimize()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("open_settings", false)) {
            navigationEvents.tryEmit(AppRoutes.SETTINGS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStore = OverlayDataStore(this)

        val shouldOpenSettings = intent?.getBooleanExtra("open_settings", false) == true
        val showDonation       = intent.getBooleanExtra(EXTRA_SHOW_DONATION, false)

        onBackPressedDispatcher.addCallback(this) {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }

        setContent {
            val config by dataStore.configFlow.collectAsState(initial = OverlayConfig())

            KronoTheme(selectedTheme = config.selectedTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        dataStore                 = dataStore,
                        timerViewModel            = timerViewModel,
                        pendingUpdateInfo         = pendingUpdateInfo.collectAsState().value,
                        navigationEvents          = navigationEvents,
                        permissionsDialogEvents   = permissionsDialogEvents,
                        isTaskRoot                = isTaskRoot,
                        showDonationDialog        = showDonation,
                        startInSettings           = shouldOpenSettings,
                        onTryStartService         = { tryStartService() },
                        onRequestNotification     = { requestNotificationPermission() },
                        onRequestOverlay          = { openOverlayPermissionSettings() },
                        onStartFocusMode          = { startFocusMode() },
                        onShowOverlay             = { showOverlay() },
                        onReset                   = { sendResetToService() },
                        isServiceRunning          = { isServiceRunning() }
                    )
                }
            }
        }

        // Abre o dialog unificado se faltar qualquer permissão
        val lacksOverlay       = !Settings.canDrawOverlays(this)
        val lacksNotification  = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED

        if (lacksOverlay || lacksNotification) {
            permissionsDialogEvents.tryEmit(Unit)
        }

        checkForUpdateIfNeeded { info -> pendingUpdateInfo.value = info }
    }

    private fun tryStartService() {
        if (!Settings.canDrawOverlays(this)) {
            permissionsDialogEvents.tryEmit(Unit)
            return
        }
        startServiceAndMinimize()
    }

    private fun startServiceAndMinimize() {
        lifecycleScope.launch {
            val config = dataStore.configFlow.first()
            val intent = Intent(this@MainActivity, MainService::class.java).apply {
                if (config.focusModeEnabled) action = com.krono.app.ACTION_START_FOCUS
            }
            startForegroundService(intent)
            moveTaskToBack(true)
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun openOverlayPermissionSettings() {
        overlayPermissionLauncher.launch(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun sendResetToService() {
        startForegroundService(
            Intent(this, MainService::class.java).apply { action = ACTION_RESET }
        )
    }

    private fun startFocusMode() {
        startForegroundService(
            Intent(this, MainService::class.java).apply { action = com.krono.app.ACTION_START_FOCUS }
        )
    }

    private fun showOverlay() {
        startForegroundService(
            Intent(this, MainService::class.java).apply { action = ACTION_SHOW_OVERLAY }
        )
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == MainService::class.java.name }
    }

    private fun checkForUpdateIfNeeded(onUpdateAvailable: (UpdateInfo) -> Unit) {
        lifecycleScope.launch {
            val config  = dataStore.configFlow.first()
            val now     = System.currentTimeMillis()
            if (now - config.lastUpdateCheck < 24 * 60 * 60 * 1000L) return@launch
            dataStore.saveLastUpdateCheck(now)
            val result = checkForUpdate(BuildConfig.VERSION_NAME)
            if (result is UpdateResult.UpdateAvailable) onUpdateAvailable(result.info)
        }
    }
}
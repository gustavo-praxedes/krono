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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.krono.app.R
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
    private val permissionsRefreshTrigger     = MutableStateFlow(0)

    private lateinit var dataStore: OverlayDataStore
    private val timerViewModel: TimerViewModel
        get() = (application as KronoApp).timerViewModel

    private val pendingUpdateInfo = MutableStateFlow<UpdateInfo?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionsRefreshTrigger.value++ }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { permissionsRefreshTrigger.value++ }

    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { permissionsRefreshTrigger.value++ }

    override fun onResume() {
        super.onResume()
        permissionsRefreshTrigger.value++

        val lacksOverlay      = !Settings.canDrawOverlays(this)
        val lacksNotification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED

        if (lacksOverlay || lacksNotification) {
            lifecycleScope.launch { permissionsDialogEvents.emit(Unit) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_settings", false)) {
            navigationEvents.tryEmit(AppRoutes.SETTINGS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStore = OverlayDataStore(this)

        onBackPressedDispatcher.addCallback(this) {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }

        setContent {
            val config by dataStore.configFlow.collectAsState(initial = OverlayConfig())

            KronoTheme(selectedTheme = config.selectedTheme) {
                val surfaceColor = MaterialTheme.colorScheme.background
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = surfaceColor
                ) {
                    AppNavigation(
                        dataStore                 = dataStore,
                        timerViewModel            = timerViewModel,
                        pendingUpdateInfo         = pendingUpdateInfo.collectAsState().value,
                        navigationEvents          = navigationEvents,
                        permissionsDialogEvents   = permissionsDialogEvents,
                        permissionsRefreshTrigger = permissionsRefreshTrigger.collectAsState().value,
                        isTaskRoot                = isTaskRoot,
                        startInSettings           = intent?.getBooleanExtra("open_settings", false) == true,
                        onTryStartService         = { tryStartService() },
                        onRequestNotification     = { requestNotificationPermission() },
                        onRequestOverlay          = { openOverlayPermissionSettings() },
                        onRequestInstall          = { openInstallPermissionSettings() },
                        onStartFocusMode          = { startFocusMode() },
                        onShowOverlay             = { showOverlay() },
                        onReset                   = { sendResetToService() },
                        isServiceRunning          = { isServiceRunning() }
                    )
                }
            }
        }
    }

    private fun tryStartService() {
        if (!Settings.canDrawOverlays(this)) {
            lifecycleScope.launch { permissionsDialogEvents.emit(Unit) }
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
        overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    fun openInstallPermissionSettings() {
        installPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
    }

    private fun sendResetToService() {
        startForegroundService(Intent(this, MainService::class.java).apply { action = ACTION_RESET })
    }

    private fun startFocusMode() {
        startForegroundService(Intent(this, MainService::class.java).apply { action = com.krono.app.ACTION_START_FOCUS })
    }

    private fun showOverlay() {
        startForegroundService(Intent(this, MainService::class.java).apply { action = ACTION_SHOW_OVERLAY })
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == MainService::class.java.name }
    }
}

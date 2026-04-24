package com.krono.app.ui

import android.app.Activity
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.krono.app.data.OverlayDataStore
import com.krono.app.data.OverlayConfig
import com.krono.app.util.UpdateInfo
import com.krono.app.viewmodel.TimerViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AppRoutes {
    const val TIMER    = "timer"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(
    dataStore                 : OverlayDataStore,
    timerViewModel            : TimerViewModel,
    pendingUpdateInfo         : UpdateInfo?,
    navigationEvents          : SharedFlow<String>,
    permissionsDialogEvents   : SharedFlow<Unit>,
    permissionsRefreshTrigger : Int,
    isTaskRoot                : Boolean,
    startInSettings           : Boolean,
    onTryStartService         : () -> Unit,
    onRequestNotification     : () -> Unit,
    onRequestOverlay          : () -> Unit,
    onRequestInstall          : () -> Unit,
    onStartFocusMode          : () -> Unit,
    onShowOverlay             : () -> Unit,
    onReset                   : () -> Unit,
    isServiceRunning          : () -> Boolean
) {
    val navController = rememberNavController()
    val timerState    by timerViewModel.timerState.collectAsState()
    val context       = LocalContext.current
    val activity      = context as? Activity
    val scope         = rememberCoroutineScope()
    
    val config by dataStore.configFlow.collectAsState(initial = OverlayConfig())

    var showPermissionsDialog by remember { mutableStateOf(false) }

    // Relê permissões
    val hasOverlayPermission = remember(permissionsRefreshTrigger) { Settings.canDrawOverlays(context) }
    val hasNotificationPermission = remember(permissionsRefreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }
    val hasInstallPermission = remember(permissionsRefreshTrigger) { context.packageManager.canRequestPackageInstalls() }

    LaunchedEffect(Unit) {
        launch { navigationEvents.collect { route -> navController.navigate(route) { launchSingleTop = true } } }
        launch { permissionsDialogEvents.collect { showPermissionsDialog = true } }
        
        val cfg = dataStore.configFlow.first()
        if (cfg.autoLaunch && !isTaskRoot) {
            onTryStartService()
        }
    }

    // Exibe a navegação normal
    NavHost(
            navController    = navController,
            startDestination = if (startInSettings) AppRoutes.SETTINGS else AppRoutes.TIMER
        ) {
            composable(AppRoutes.TIMER) {
                TimerScreen(
                    timerState     = timerState,
                    onStart        = { timerViewModel.start() },
                    onPause        = { timerViewModel.pause() },
                    onReset        = onReset,
                    onOpenOverlay  = onTryStartService,
                    onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) }
                )
            }

            composable(AppRoutes.SETTINGS) {
                SettingsScreen(
                    dataStore          = dataStore,
                    pendingUpdateInfo  = pendingUpdateInfo,
                    isServiceRunning   = isServiceRunning,
                    onStartFocusMode   = onStartFocusMode,
                    onShowOverlay      = onShowOverlay,
                    onBack             = { navController.popBackStack() }
                )
            }
        }

    if (showPermissionsDialog) {
        PermissionsDialog(
            hasNotificationPermission = hasNotificationPermission,
            hasOverlayPermission      = hasOverlayPermission,
            hasInstallPermission      = hasInstallPermission,
            onRequestNotification     = onRequestNotification,
            onRequestOverlay          = onRequestOverlay,
            onRequestInstall          = onRequestInstall,
            onDismiss                 = { showPermissionsDialog = false }
        )
    }
}

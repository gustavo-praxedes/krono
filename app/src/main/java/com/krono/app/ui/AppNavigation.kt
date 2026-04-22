package com.krono.app.ui

import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.krono.app.data.OverlayDataStore
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
    dataStore              : OverlayDataStore,
    timerViewModel         : TimerViewModel,
    pendingUpdateInfo      : UpdateInfo?,
    navigationEvents       : SharedFlow<String>,
    permissionsDialogEvents: SharedFlow<Unit>,
    isTaskRoot             : Boolean,
    showDonationDialog     : Boolean,
    startInSettings        : Boolean,
    onTryStartService      : () -> Unit,
    onRequestNotification  : () -> Unit,
    onRequestOverlay       : () -> Unit,
    onStartFocusMode       : () -> Unit,
    onShowOverlay          : () -> Unit,
    onReset                : () -> Unit,
    isServiceRunning       : () -> Boolean
) {
    val navController = rememberNavController()
    val timerState    by timerViewModel.timerState.collectAsState()
    val context       = LocalContext.current

    var showPermissionsDialog by remember { mutableStateOf(false) }

    // Relê o estado das permissões a cada recomposição (volta do Settings do Android)
    val hasOverlayPermission by remember {
        derivedStateOf { Settings.canDrawOverlays(context) }
    }
    val hasNotificationPermission by remember {
        derivedStateOf {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        }
    }

    LaunchedEffect(Unit) {
        launch {
            navigationEvents.collect { route ->
                navController.navigate(route) { launchSingleTop = true }
            }
        }
        launch {
            permissionsDialogEvents.collect {
                showPermissionsDialog = true
            }
        }
        val cfg = dataStore.configFlow.first()
        if (cfg.autoLaunch && !isTaskRoot) {
            onTryStartService()
        }
    }

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
                showDonationDialog = showDonationDialog,
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
            onRequestNotification     = onRequestNotification,
            onRequestOverlay          = onRequestOverlay,
            onDismiss                 = { showPermissionsDialog = false }
        )
    }
}
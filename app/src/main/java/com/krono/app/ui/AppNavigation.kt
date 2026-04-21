package com.krono.app.ui

import androidx.compose.runtime.*
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
    dataStore                    : OverlayDataStore,
    timerViewModel               : TimerViewModel,
    pendingUpdateInfo            : UpdateInfo?,
    navigationEvents             : SharedFlow<String>,
    overlayPermissionDialogEvents: SharedFlow<Unit>,
    isTaskRoot                   : Boolean,
    showDonationDialog           : Boolean,
    startInSettings              : Boolean, // <── ADICIONADO AQUI
    onTryStartService            : () -> Unit,
    onConfirmPermission          : () -> Unit,
    onStartFocusMode             : () -> Unit,
    onShowOverlay                : () -> Unit,
    onReset                      : () -> Unit,
    isServiceRunning             : () -> Boolean
) {
    val navController = rememberNavController()
    val timerState    by timerViewModel.timerState.collectAsState()

    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            navigationEvents.collect { route ->
                navController.navigate(route) { launchSingleTop = true }
            }
        }
        launch {
            overlayPermissionDialogEvents.collect {
                showOverlayPermissionDialog = true
            }
        }
        val cfg = dataStore.configFlow.first()
        if (cfg.autoLaunch && !isTaskRoot) {
            onTryStartService()
        }
    }

    NavHost(
        navController    = navController,
        // Define se o app começa no Timer ou direto nas Configurações
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

    if (showOverlayPermissionDialog) {
        OverlayPermissionDialog(
            onConfirm = {
                showOverlayPermissionDialog = false
                onConfirmPermission()
            },
            onDismiss = {
                showOverlayPermissionDialog = false
            }
        )
    }
}
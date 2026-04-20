package com.krono.app.ui

import android.content.Intent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.krono.app.ACTION_RESET
import com.krono.app.data.OverlayConfig
import com.krono.app.data.OverlayDataStore
import com.krono.app.service.MainService
import com.krono.app.util.UpdateInfo
import com.krono.app.viewmodel.TimerViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ============================================================
// AppNavigation.kt
// Rotas e NavHost da aplicação.
// Extraído da MainActivity para manter o ciclo de vida da
// Activity focado em permissões e setup do sistema.
// ============================================================

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
    onTryStartService            : () -> Unit,
    onStartFocusMode             : () -> Unit,
    onShowOverlay                : () -> Unit,
    onReset                      : () -> Unit,
    isServiceRunning             : () -> Boolean
) {
    val navController = rememberNavController()
    val timerState    by timerViewModel.timerState.collectAsState()
    val scope         = rememberCoroutineScope()

    // Dialog de permissão de overlay — acionado pela Activity via Flow
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Escuta eventos de navegação vindos da Activity (ex: open_settings via Intent)
        launch {
            navigationEvents.collect { route ->
                navController.navigate(route) { launchSingleTop = true }
            }
        }
        // Escuta sinal para exibir o dialog de permissão
        launch {
            overlayPermissionDialogEvents.collect {
                showOverlayPermissionDialog = true
            }
        }
        // AutoLaunch: se configurado e não é a task raiz, abre overlay e minimiza
        val cfg = dataStore.configFlow.first()
        if (cfg.autoLaunch && !isTaskRoot) {
            onTryStartService()
        }
    }

    NavHost(
        navController    = navController,
        startDestination = AppRoutes.TIMER
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

    // Dialog de permissão renderizado fora do NavHost para
    // garantir que aparece sobre qualquer rota ativa
    if (showOverlayPermissionDialog) {
        OverlayPermissionDialog(
            onConfirm = {
                showOverlayPermissionDialog = false
                onTryStartService()         // Activity trata o resultado do launcher
            },
            onDismiss = {
                showOverlayPermissionDialog = false
            }
        )
    }
}

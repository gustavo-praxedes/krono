package com.krono.app.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.view.WindowManager
import com.krono.app.ACTION_HIDE_OVERLAY
import com.krono.app.ACTION_PAUSE
import com.krono.app.ACTION_PLAY
import com.krono.app.ACTION_RESET
import com.krono.app.ACTION_SHOW_OVERLAY
import com.krono.app.ACTION_START_FOCUS
import com.krono.app.ACTION_STOP_SERVICE
import com.krono.app.EXTRA_SHOW_DONATION
import com.krono.app.KronoApp
import com.krono.app.NOTIFICATION_ID
import com.krono.app.data.OverlayConfig
import com.krono.app.data.OverlayDataStore
import com.krono.app.data.TimerPreferences
import com.krono.app.ui.FocusActivity
import com.krono.app.ui.MainActivity
import com.krono.app.viewmodel.TimerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

const val ACTION_FOCUS_DISMISSED = "com.krono.app.ACTION_FOCUS_DISMISSED"

class MainService : Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private lateinit var dataStore: OverlayDataStore
    private lateinit var timerPrefs: TimerPreferences
    
    // Módulos Delegados
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var overlayManager: OverlayManager
    private lateinit var feedbackManager: FeedbackManager

    private val viewModel: TimerViewModel
        get() = (application as KronoApp).timerViewModel
        
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationJob: Job? = null
    private var currentConfig: OverlayConfig = OverlayConfig()

    // ========================================================
    // CICLO DE VIDA
    // ========================================================

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        dataStore = OverlayDataStore(this)
        timerPrefs = TimerPreferences(this)
        
        // Inicializa Módulos
        notificationHelper = NotificationHelper(this)
        feedbackManager = FeedbackManager(this)
        overlayManager = OverlayManager(
            context = this,
            windowManager = windowManager,
            dataStore = dataStore,
            viewModel = viewModel,
            serviceScope = serviceScope,
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this
        )

        startForegroundWithNotification()
        timerPrefs.setServiceActive(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        when (intent?.action) {
            ACTION_PLAY -> {
                viewModel.start()
                feedbackManager.triggerFeedback(currentConfig)
            }
            ACTION_PAUSE -> {
                viewModel.pause()
                feedbackManager.triggerFeedback(currentConfig)
            }
            ACTION_RESET -> handleReset()
            ACTION_STOP_SERVICE -> {
                closeAndStop()
                return START_NOT_STICKY
            }
            ACTION_SHOW_OVERLAY -> overlayManager.showOverlayIfHidden()
            ACTION_HIDE_OVERLAY -> hideOverlay()
            ACTION_START_FOCUS -> {
                serviceScope.launch {
                    if (!overlayManager.overlayVisible) {
                        // Serviço iniciado com modo foco mas overlay ainda não existe.
                        // Precisa criar o overlay primeiro — showOverlay() chama
                        // onFocusModeStarted() automaticamente quando focusModeEnabled=true.
                        currentConfig = dataStore.configFlow.first()
                        showOverlay()
                        observeConfig()
                        observeScreenState()
                        observeTimerRunning()
                        startNotificationUpdater()
                        observeTimerLimit()
                    } else {
                        // Overlay já visível: apenas ativa o modo foco.
                        startFocusMode()
                    }
                }
            }
            ACTION_FOCUS_DISMISSED -> {}
            else -> {
                serviceScope.launch {
                    if (!overlayManager.overlayVisible) {
                        currentConfig = dataStore.configFlow.first()
                        showOverlay()
                    }
                    observeConfig()
                    observeScreenState()
                    observeTimerRunning()
                    startNotificationUpdater()
                    observeTimerLimit()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (viewModel.timerState.value.isRunning) viewModel.pause()
        timerPrefs.setServiceActive(false)
        applyScreenOn(false)
        overlayManager.removeOverlay()
        releaseWakeLock()
        feedbackManager.release()
        notificationJob?.cancel()
        serviceScope.cancel()
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ========================================================
    // LÓGICA DE NEGÓCIO (DELEGADA)
    // ========================================================

    private fun showOverlay() {
        overlayManager.showOverlay(
            currentConfig = currentConfig,
            onStart = { viewModel.start(); feedbackManager.triggerFeedback(currentConfig) },
            onPause = { viewModel.pause(); feedbackManager.triggerFeedback(currentConfig) },
            onReset = { handleReset() },
            onClose = {
                val state = viewModel.timerState.value
                if (state.isRunning || state.elapsedMs > 0) {
                    // Timer ativo ou pausado: apenas esconde o overlay
                    hideOverlay()
                } else {
                    // Timer zerado: encerra tudo
                    closeAndStop()
                }
            },
            onSettings = { openMainActivity() },
            onFocusModeStarted = { startFocusMode() }
        )
    }

    private fun hideOverlay() {
        overlayManager.hideOverlay {
            sendBroadcast(Intent(ACTION_FOCUS_DISMISSED).apply { `package` = packageName })
        }
    }

    private fun closeAndStop() {
        viewModel.reset()
        timerPrefs.clearState()
        timerPrefs.setServiceActive(false)
        applyScreenOn(false)
        hideOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleReset() {
        val sessionMs = viewModel.currentSessionMs
        serviceScope.launch {
            dataStore.accumulateTime(sessionMs)
            val updatedCycleMs = dataStore.configFlow
                .first { it.currentCycleMs > 0L || sessionMs == 0L }
                .currentCycleMs

            if (updatedCycleMs >= 12 * 3600 * 1000L) {
                dataStore.resetCycle()
                hideOverlay()
                val intent = Intent(this@MainService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_SHOW_DONATION, true)
                }
                startActivity(intent)
            }
            viewModel.reset()
        }
    }

    @SuppressLint("InlinedApi")
    private fun startForegroundWithNotification() {
        val notification = notificationHelper.buildNotification(
            viewModel.timerState.value,
            currentConfig.showHours,
            currentConfig.showSeconds
        )
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startNotificationUpdater() {
        notificationJob?.cancel()
        notificationJob = serviceScope.launch {
            var lastIsRunning = viewModel.timerState.value.isRunning
            var lastIsAtLimit = viewModel.timerState.value.isAtLimit
            var lastWasReset  = false

            viewModel.timerState.collectLatest { state ->
                val isReset      = !state.isRunning && state.elapsedMs == 0L && !lastWasReset
                val stateChanged = state.isRunning != lastIsRunning || state.isAtLimit != lastIsAtLimit || isReset

                if (stateChanged) {
                    lastIsRunning = state.isRunning
                    lastIsAtLimit = state.isAtLimit
                    lastWasReset  = isReset
                    val notification = notificationHelper.buildNotification(state, currentConfig.showHours, currentConfig.showSeconds)
                    (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(NOTIFICATION_ID, notification)
                } else {
                    lastWasReset = false
                }
            }
        }
    }

    // ========================================================
    // OBSERVADORES E RECURSOS
    // ========================================================

    private fun observeConfig() {
        serviceScope.launch {
            dataStore.configFlow.collectLatest { config ->
                val focusDisabled = currentConfig.focusModeEnabled && !config.focusModeEnabled
                val focusEnabled = !currentConfig.focusModeEnabled && config.focusModeEnabled

                currentConfig = config
                viewModel.setTimeLimit(config.timeLimitSeconds)

                if (focusDisabled) {
                    sendBroadcast(Intent(ACTION_FOCUS_DISMISSED).apply { `package` = packageName })
                } else if (focusEnabled && viewModel.timerState.value.isRunning && overlayManager.overlayVisible) {
                    startFocusMode()
                }
            }
        }
    }

    private fun observeScreenState() {
        serviceScope.launch {
            dataStore.configFlow.collect { config -> applyScreenOn(config.keepScreenOn) }
        }
    }

    private fun observeTimerRunning() {
        serviceScope.launch {
            var wasRunning = viewModel.timerState.value.isRunning
            viewModel.timerState.collect { state ->
                val justStarted = !wasRunning && state.isRunning
                wasRunning = state.isRunning
                if (justStarted && currentConfig.focusModeEnabled && overlayManager.overlayVisible) {
                    startFocusMode()
                }
            }
        }
    }

    private fun observeTimerLimit() {
        serviceScope.launch {
            viewModel.timerState.collect { state ->
                if (state.isAtLimit) {
                    hideOverlay()
                }
            }
        }
    }

    private fun applyScreenOn(enable: Boolean) {
        overlayManager.applyKeepScreenOn(enable)
        if (enable) {
            if (wakeLock?.isHeld != true) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Cronometro::WakeLock")
                    .apply { acquire(99 * 60 * 60 * 1000L) }
            }
        } else {
            releaseWakeLock()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun openMainActivity() {
        val i = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_settings", true)
        }
        startActivity(i)
    }

    private fun startFocusMode() {
        if (!overlayManager.overlayVisible) return
        val intent = Intent(this, FocusActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }
}
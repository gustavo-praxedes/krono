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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import androidx.lifecycle.*
import androidx.savedstate.*

const val ACTION_FOCUS_DISMISSED = "com.krono.app.ACTION_FOCUS_DISMISSED"

class MainService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private lateinit var dataStore: OverlayDataStore
    private lateinit var timerPrefs: TimerPreferences
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var overlayManager: OverlayManager
    private lateinit var feedbackManager: FeedbackManager

    private val viewModel: TimerViewModel get() = (application as KronoApp).timerViewModel
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationJob: Job? = null
    private var currentConfig: OverlayConfig = OverlayConfig()
    private var observersStarted = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        dataStore = OverlayDataStore(this)
        timerPrefs = TimerPreferences(this)
        
        notificationHelper = NotificationHelper(this)
        feedbackManager = FeedbackManager(this)
        overlayManager = OverlayManager(this, windowManager, dataStore, viewModel, serviceScope, this, this, this)

        startForegroundWithNotification()
        timerPrefs.setServiceActive(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        ensureObserversStarted()

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
            ACTION_STOP_SERVICE -> closeAndStop()
            ACTION_SHOW_OVERLAY -> overlayManager.showOverlayIfHidden()
            ACTION_HIDE_OVERLAY -> hideOverlay()
            ACTION_START_FOCUS -> {
                if (!overlayManager.overlayVisible) showOverlay()
                startFocusMode()
            }
            else -> if (!overlayManager.overlayVisible) showOverlay()
        }
        return START_STICKY
    }

    private fun ensureObserversStarted() {
        if (observersStarted) return
        observersStarted = true
        observeConfig()
        observeScreenState()
        observeTimerRunning()
        startNotificationUpdater()
        observeTimerLimit()
        observeDonationState()
    }

    private fun showOverlay() {
        serviceScope.launch {
            currentConfig = dataStore.configFlow.first()
            overlayManager.showOverlay(
                currentConfig = currentConfig,
                onStart = { viewModel.start(); feedbackManager.triggerFeedback(currentConfig) },
                onPause = { viewModel.pause(); feedbackManager.triggerFeedback(currentConfig) },
                onReset = { handleReset() },
                onClose = {
                    // O X agora apenas esconde o overlay, mantendo o serviço e o timer ativos.
                    hideOverlay()
                },
                onSettings = { openMainActivity(openSettings = true) },
                onFocusModeStarted = { startFocusMode() }
            )
        }
    }

    private fun handleReset() {
        val sessionMs = viewModel.currentSessionMs
        serviceScope.launch {
            dataStore.accumulateTime(sessionMs)
            viewModel.reset()
        }
    }

    private fun observeDonationState() {
        serviceScope.launch {
            dataStore.configFlow.collect { config ->
                if (config.donationPending) {
                    val wasVisible = overlayManager.overlayVisible
                    if (wasVisible) {
                        hideOverlay()
                    }
                    openMainActivity(showDonation = true, wasOverlayVisible = wasVisible)
                }
            }
        }
    }

    private fun openMainActivity(openSettings: Boolean = false, showDonation: Boolean = false, wasOverlayVisible: Boolean = false) {
        if (showDonation) {
            val intent = Intent(this, com.krono.app.ui.DonationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra("restore_overlay", wasOverlayVisible)
            }
            startActivity(intent)
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (openSettings) putExtra("open_settings", true)
        }
        startActivity(intent)
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
        overlayManager.removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("InlinedApi")
    private fun startForegroundWithNotification() {
        val notification = notificationHelper.buildNotification(viewModel.timerState.value, currentConfig.showHours, currentConfig.showSeconds)
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
                val isReset = !state.isRunning && state.elapsedMs == 0L && !lastWasReset
                if (state.isRunning != lastIsRunning || state.isAtLimit != lastIsAtLimit || isReset) {
                    lastIsRunning = state.isRunning
                    lastIsAtLimit = state.isAtLimit
                    lastWasReset = isReset
                    val n = notificationHelper.buildNotification(state, currentConfig.showHours, currentConfig.showSeconds)
                    (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(NOTIFICATION_ID, n)
                } else lastWasReset = false
            }
        }
    }

    private fun observeConfig() {
        serviceScope.launch {
            dataStore.configFlow.collectLatest { config ->
                val focusEnabled = !currentConfig.focusModeEnabled && config.focusModeEnabled
                currentConfig = config
                viewModel.setTimeLimit(config.timeLimitSeconds)
                if (!config.focusModeEnabled) {
                    sendBroadcast(Intent(ACTION_FOCUS_DISMISSED).apply { `package` = packageName })
                } else if (focusEnabled && viewModel.timerState.value.isRunning && overlayManager.overlayVisible) {
                    startFocusMode()
                }
            }
        }
    }

    private fun observeScreenState() {
        serviceScope.launch { dataStore.configFlow.collect { applyScreenOn(it.keepScreenOn) } }
    }

    private fun observeTimerRunning() {
        serviceScope.launch {
            var wasRunning = viewModel.timerState.value.isRunning
            viewModel.timerState.collect { state ->
                val started = !wasRunning && state.isRunning
                wasRunning = state.isRunning
                if (started && currentConfig.focusModeEnabled && overlayManager.overlayVisible) startFocusMode()
            }
        }
    }

    private fun observeTimerLimit() {
        serviceScope.launch { viewModel.timerState.collect { if (it.isAtLimit) hideOverlay() } }
    }

    private fun applyScreenOn(enable: Boolean) {
        overlayManager.applyKeepScreenOn(enable)
        if (enable) {
            if (wakeLock?.isHeld != true) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Cronometro::WakeLock").apply { acquire(99 * 3600_000L) }
            }
        } else releaseWakeLock()
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun startFocusMode() {
        if (!overlayManager.overlayVisible) return
        startActivity(Intent(this, FocusActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
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
}

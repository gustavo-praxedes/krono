package com.krono.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.krono.app.ACTION_HIDE_OVERLAY
import com.krono.app.ACTION_PAUSE
import com.krono.app.ACTION_PLAY
import com.krono.app.ACTION_RESET
import com.krono.app.ACTION_SHOW_OVERLAY
import com.krono.app.ACTION_START_FOCUS
import com.krono.app.ACTION_STOP_SERVICE
import com.krono.app.EXTRA_SHOW_DONATION
import com.krono.app.KronoApp
import com.krono.app.NOTIFICATION_CHANNEL_ID
import com.krono.app.NOTIFICATION_ID
import com.krono.app.R
import com.krono.app.data.OverlayConfig
import com.krono.app.data.OverlayDataStore
import com.krono.app.data.TimerPreferences
import com.krono.app.data.TimerState
import com.krono.app.data.toFormattedTime
import com.krono.app.receiver.NotificationActionReceiver
import com.krono.app.ui.FloatingTimerUi
import com.krono.app.ui.FocusActivity
import com.krono.app.ui.MainActivity
import com.krono.app.viewmodel.TimerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    private lateinit var windowManager: WindowManager
    private lateinit var dataStore: OverlayDataStore
    private lateinit var timerPrefs: TimerPreferences

    private val viewModel: TimerViewModel
        get() = (application as KronoApp).timerViewModel
    private var composeView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var notificationJob: Job? = null
    private var currentConfig: OverlayConfig = OverlayConfig()
    private var lastNotifiedSecond: Long = -1L

    private lateinit var contentPendingIntent: PendingIntent

    companion object {
        private const val EDGE_SNAP_THRESHOLD = 50
    }

    private var overlayVisible: Boolean = false

    // ========================================================
    // CICLO DE VIDA
    // ========================================================

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        dataStore = OverlayDataStore(this)
        timerPrefs = TimerPreferences(this)

        contentPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_ALARM, 80)
        } catch (_: Exception) {
            null
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        startForegroundWithNotification()
        timerPrefs.setServiceActive(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        when (intent?.action) {
            ACTION_PLAY -> {
                viewModel.start()
                triggerFeedback(currentConfig)
            }

            ACTION_PAUSE -> {
                viewModel.pause()
                triggerFeedback(currentConfig)
            }

            ACTION_RESET -> handleReset()

            ACTION_STOP_SERVICE -> {
                closeAndStop()
                return START_NOT_STICKY
            }

            ACTION_SHOW_OVERLAY -> showOverlayIfHidden()

            ACTION_HIDE_OVERLAY -> hideOverlay()

            ACTION_START_FOCUS -> startFocusMode()

            ACTION_FOCUS_DISMISSED -> {}

            else -> {
                serviceScope.launch {
                    if (composeView == null) {
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
        removeOverlay()
        releaseWakeLock()
        toneGenerator?.release()
        toneGenerator = null
        notificationJob?.cancel()
        serviceScope.cancel()
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ========================================================
    // FECHAR COMPLETAMENTE
    // ========================================================

    private fun closeAndStop() {
        viewModel.reset()
        timerPrefs.clearState()
        timerPrefs.setServiceActive(false)
        applyScreenOn(false)

        sendBroadcast(Intent(ACTION_FOCUS_DISMISSED).apply {
            `package` = packageName
        })

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

            val limitMs = 12 * 3600 * 1000L

            if (updatedCycleMs >= limitMs) {
                dataStore.resetCycle()
                hideOverlay()

                val intent = Intent(this@MainService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_SHOW_DONATION, true)
                }
                startActivity(intent)
            }
            // Reset NÃO envia ACTION_FOCUS_DISMISSED —
            // o Modo Foco permanece ativo após resetar
            viewModel.reset()
        }
    }

    // ========================================================
    // FOREGROUND SERVICE E NOTIFICAÇÃO
    // ========================================================

    @SuppressLint("InlinedApi")
    private fun startForegroundWithNotification() {
        val notification = buildNotification(viewModel.timerState.value)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(timerState: TimerState): Notification {

        fun actionIntent(action: String, requestCode: Int): PendingIntent {
            val i = Intent(this, NotificationActionReceiver::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                this, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_title))
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(
                if (timerState.isRunning) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                if (timerState.isRunning) getString(R.string.action_pause)
                else getString(R.string.action_play),
                if (timerState.isRunning) actionIntent(ACTION_PAUSE, 1)
                else actionIntent(ACTION_PLAY, 2)
            )
            .addAction(
                android.R.drawable.ic_menu_revert,
                getString(R.string.action_reset),
                actionIntent(ACTION_RESET, 3)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_stop_service),
                actionIntent(ACTION_STOP_SERVICE, 4)
            )

        if (timerState.isRunning && timerState.startTime != -1L) {
            val elapsedSinceStart = System.currentTimeMillis() - timerState.startTime
            val totalElapsed      = timerState.pauseOffset + elapsedSinceStart
            val whenMs            = System.currentTimeMillis() - totalElapsed

            builder
                .setUsesChronometer(true)
                .setChronometerCountDown(false)
                .setWhen(whenMs)
                .setShowWhen(true)
                .setContentText(getString(R.string.notification_text_running))

        } else {
            val frozenTime = timerState.elapsedMs.toFormattedTime(
                showHours   = currentConfig.showHours,
                showSeconds = currentConfig.showSeconds
            )
            builder
                .setUsesChronometer(false)
                .setShowWhen(false)
                .setContentText(frozenTime)
        }

        return builder.build()
    }

    private fun startNotificationUpdater() {
        notificationJob?.cancel()
        notificationJob = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            var lastIsRunning = viewModel.timerState.value.isRunning
            var lastIsAtLimit = viewModel.timerState.value.isAtLimit

            viewModel.timerState.collectLatest { state ->
                val stateChanged = state.isRunning != lastIsRunning ||
                        state.isAtLimit != lastIsAtLimit

                if (stateChanged) {
                    lastIsRunning = state.isRunning
                    lastIsAtLimit = state.isAtLimit
                    lastNotifiedSecond = state.elapsedMs / 1000L

                    val notification = buildNotification(state)
                    (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager)
                        .notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    // ========================================================
    // OVERLAY FLUTUANTE
    // ========================================================

    private fun showOverlay() {
        if (composeView != null) return

        val savedX = currentConfig.lastX.takeIf { it >= 0 } ?: 100
        val savedY = currentConfig.lastY.takeIf { it >= 0 } ?: 200

        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }

        val view = ComposeView(this).apply {
            setContent {
                val config     by dataStore.configFlow.collectAsState(initial = OverlayConfig())
                val timerState by viewModel.timerState.collectAsState()

                FloatingTimerUi(
                    timerState             = timerState,
                    config                 = config,
                    onStart                = {
                        viewModel.start()
                        triggerFeedback(currentConfig)
                    },
                    onPause                = {
                        viewModel.pause()
                        triggerFeedback(currentConfig)
                    },
                    onReset                = { handleReset() },
                    onDrag                 = { dx, dy -> handleDrag(dx, dy) },
                    onDragEnd              = { saveOverlayPosition() },
                    onClose                = { closeAndStop() },
                    onSettings             = { openMainActivity() },
                    onToggleFocus          = {
                        serviceScope.launch {
                            dataStore.updateConfig(
                                currentConfig.copy(focusModeEnabled = !currentConfig.focusModeEnabled)
                            )
                        }
                    },
                    onToggleKeepScreenOn   = {
                        serviceScope.launch {
                            dataStore.updateConfig(
                                currentConfig.copy(keepScreenOn = !currentConfig.keepScreenOn)
                            )
                        }
                    },
                    onMenuVisibilityChange = { menuOpen ->
                        setOverlayFocusable(menuOpen)
                    }
                )
            }
        }

        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)

        composeView = view
        windowManager.addView(view, overlayParams)
        overlayVisible = true
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        if (currentConfig.focusModeEnabled) {
            startFocusMode()
        }
    }

    private fun removeOverlay() {
        composeView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
            composeView = null
        }
    }

    private fun hideOverlay() {
        // Desabilitar overlay também desativa o Modo Foco
        sendBroadcast(Intent(ACTION_FOCUS_DISMISSED).apply {
            `package` = packageName
        })
        composeView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
        }
        overlayVisible = false
    }

    private fun showOverlayIfHidden() {
        if (!overlayVisible && composeView != null) {
            try {
                windowManager.addView(composeView, overlayParams)
                overlayVisible = true
            } catch (_: Exception) { }
        }
    }

    private fun setOverlayFocusable(focusable: Boolean) {
        val params = overlayParams ?: return
        val view   = composeView   ?: return
        params.flags = if (focusable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
    }

    private fun handleDrag(dx: Float, dy: Float) {
        val params = overlayParams ?: return
        val view   = composeView   ?: return

        val widgetWidth  = view.width.takeIf  { it > 0 } ?: return
        val widgetHeight = view.height.takeIf { it > 0 } ?: return

        val metrics      = resources.displayMetrics
        val screenWidth  = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        var newX = params.x + dx.toInt()
        var newY = params.y + dy.toInt()

        val rightEdge  = screenWidth  - widgetWidth
        val bottomEdge = screenHeight - widgetHeight

        when {
            newX <= EDGE_SNAP_THRESHOLD && dx <= 0             -> newX = 0
            newX >= rightEdge - EDGE_SNAP_THRESHOLD && dx >= 0 -> newX = rightEdge
        }
        when {
            newY <= EDGE_SNAP_THRESHOLD && dy <= 0              -> newY = 0
            newY >= bottomEdge - EDGE_SNAP_THRESHOLD && dy >= 0 -> newY = bottomEdge
        }

        params.x = newX.coerceIn(0, maxOf(0, rightEdge))
        params.y = newY.coerceIn(0, maxOf(0, bottomEdge))

        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
    }

    private fun saveOverlayPosition() {
        val params = overlayParams ?: return
        serviceScope.launch {
            dataStore.savePosition(params.x, params.y)
        }
    }

    // ========================================================
    // OBSERVADORES
    // ========================================================

    private fun observeConfig() {
        serviceScope.launch {
            dataStore.configFlow.collectLatest { config ->
                currentConfig = config
                viewModel.setTimeLimit(config.timeLimitSeconds)
            }
        }
    }

    // GIT 2 + GIT 5: keepScreenOn independe de isRunning.
    // O Modo Foco força a tela ligada via FLAG na própria
    // FocusActivity — não depende deste observer.
    private fun observeScreenState() {
        serviceScope.launch {
            dataStore.configFlow.collect { config ->
                applyScreenOn(config.keepScreenOn)
            }
        }
    }

    // GIT 5: Ativa o Modo Foco automaticamente quando o
    // cronômetro inicia, se focusModeEnabled estiver ligado.
    // Persiste no Pause e no Reset (não recebem dismiss).
    private fun observeTimerRunning() {
        serviceScope.launch {
            var wasRunning = viewModel.timerState.value.isRunning
            viewModel.timerState.collect { state ->
                val justStarted = !wasRunning && state.isRunning
                wasRunning = state.isRunning
                if (justStarted && currentConfig.focusModeEnabled && overlayVisible) {
                    startFocusMode()
                }
            }
        }
    }

    private fun observeTimerLimit() {
        serviceScope.launch {
            viewModel.timerState.collect { state ->
                if (state.isAtLimit) closeAndStop()
            }
        }
    }

    // ========================================================
    // WAKELOCK E TELA
    // ========================================================

    private fun applyScreenOn(enable: Boolean) {
        val params = overlayParams ?: return
        val view   = composeView   ?: return

        if (enable) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }

            if (wakeLock?.isHeld != true) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Cronometro::WakeLock"
                ).apply { acquire(99 * 60 * 60 * 1000L) }
            }
        } else {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
            try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
            releaseWakeLock()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    // ========================================================
    // FEEDBACK
    // ========================================================

    private fun triggerFeedback(config: OverlayConfig) {
        if (config.isBeepEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }
        if (config.isVibrationEnabled) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    private fun openMainActivity() {
        val i = Intent(this, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(i)
    }

    private fun startFocusMode() {
        if (!overlayVisible) return
        val intent = Intent(this, FocusActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }
}
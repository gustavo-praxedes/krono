package com.krono.app.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.krono.app.data.OverlayConfig
import com.krono.app.data.OverlayDataStore
import com.krono.app.ui.FloatingTimerUi
import com.krono.app.ui.theme.KronoTheme
import com.krono.app.viewmodel.TimerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val dataStore: OverlayDataStore,
    private val viewModel: TimerViewModel,
    private val serviceScope: CoroutineScope,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner
) {
    private var composeView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    var overlayVisible: Boolean = false
        private set

    companion object {
        private const val EDGE_SNAP_THRESHOLD = 50
    }

    fun showOverlay(
        currentConfig: OverlayConfig,
        onStart: () -> Unit,
        onPause: () -> Unit,
        onReset: () -> Unit,
        onClose: () -> Unit,
        onSettings: () -> Unit,
        onFocusModeStarted: () -> Unit
    ) {
        if (composeView != null) {
            if (!overlayVisible) {
                showOverlayIfHidden()
            }
            return
        }

        val savedX = currentConfig.lastX.takeIf { it >= 0 } ?: 100
        val savedY = currentConfig.lastY.takeIf { it >= 0 } ?: 200

        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }

        val view = ComposeView(context).apply {
            setContent {
                val config by dataStore.configFlow.collectAsState(initial = OverlayConfig())
                val timerState by viewModel.timerState.collectAsState()

                KronoTheme(selectedTheme = config.selectedTheme) {
                    FloatingTimerUi(
                        timerState = timerState,
                        config = config,
                        onStart = onStart,
                        onPause = onPause,
                        onReset = onReset,
                        onDrag = { dx, dy -> handleDrag(dx, dy) },
                        onDragEnd = { saveOverlayPosition() },
                        onClose = onClose,
                        onSettings = onSettings,
                        onToggleFocus = {
                            serviceScope.launch {
                                dataStore.updateConfig(config.copy(focusModeEnabled = !config.focusModeEnabled))
                            }
                        },
                        onToggleKeepScreenOn = {
                            serviceScope.launch {
                                dataStore.updateConfig(config.copy(keepScreenOn = !config.keepScreenOn))
                            }
                        },
                        onToggleAutoLaunch = {
                            serviceScope.launch {
                                dataStore.updateConfig(config.copy(autoLaunch = !config.autoLaunch))
                            }
                        },
                        onToggleBeep = {
                            serviceScope.launch {
                                dataStore.updateConfig(config.copy(isBeepEnabled = !config.isBeepEnabled))
                            }
                        },
                        onMenuVisibilityChange = { menuOpen -> setOverlayFocusable(menuOpen) }
                    )
                }
            }
        }

        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        view.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

        composeView = view
        try {
            windowManager.addView(view, overlayParams)
            overlayVisible = true
            if (currentConfig.focusModeEnabled) {
                onFocusModeStarted()
            }
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Error adding overlay: ${e.message}")
        }
    }

    fun removeOverlay() {
        composeView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
            composeView = null
        }
        overlayVisible = false
    }

    fun hideOverlay(onDismissFocus: () -> Unit) {
        onDismissFocus()
        composeView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
        }
        overlayVisible = false
    }

    fun showOverlayIfHidden() {
        if (!overlayVisible && composeView != null) {
            try {
                windowManager.addView(composeView, overlayParams)
                overlayVisible = true
            } catch (_: Exception) { }
        }
    }

    fun setOverlayFocusable(focusable: Boolean) {
        val params = overlayParams ?: return
        val view = composeView ?: return
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

        val screenWidth  = context.resources.displayMetrics.widthPixels
        val screenHeight = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            context.resources.displayMetrics.heightPixels
        }

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

    fun applyKeepScreenOn(enable: Boolean) {
        val params = overlayParams ?: return
        val view = composeView ?: return
        if (enable) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        } else {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        }
        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
    }
}
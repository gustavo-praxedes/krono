package com.krono.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.krono.app.service.ACTION_FOCUS_DISMISSED
import kotlinx.coroutines.delay

// ============================================================
// FocusActivity.kt — GIT 5
// ============================================================

private const val FOCUS_HIDE_TIMEOUT_MS = 6_000L

class FocusActivity : ComponentActivity() {

    val globalInteractionTick = mutableIntStateOf(0)

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_OUTSIDE) {
            globalInteractionTick.intValue++
        }
        return super.dispatchTouchEvent(ev)
    }

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FOCUS_DISMISSED && !isFinishing) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(
            dismissReceiver,
            IntentFilter(ACTION_FOCUS_DISMISSED),
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                RECEIVER_NOT_EXPORTED
            } else 0
        )

        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor     = android.graphics.Color.BLACK
            navigationBarColor = android.graphics.Color.BLACK
        }

        setContent {
            MaterialTheme {
                FocusScreen(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(dismissReceiver) } catch (_: Exception) { }
    }
}

@Composable
private fun FocusScreen(activity: FocusActivity) {
    var blackVisible by remember { mutableStateOf(true) }

    LaunchedEffect(blackVisible, activity.globalInteractionTick.intValue) {
        if (!blackVisible) {
            delay(FOCUS_HIDE_TIMEOUT_MS)
            blackVisible = true
        }
    }

    LaunchedEffect(blackVisible) {
        val window = activity.window
        if (blackVisible) {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            window.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        } else {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            window.setLayout(1, 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = blackVisible,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                blackVisible = false
                            }
                        )
                    }
            )
        }
    }
}

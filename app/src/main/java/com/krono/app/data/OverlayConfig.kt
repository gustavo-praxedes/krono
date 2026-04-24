package com.krono.app.data

data class OverlayConfig(
    val backgroundColor   : Int     = android.graphics.Color.WHITE,
    val textColor         : Int     = android.graphics.Color.BLACK,
    val bgOpacity         : Float   = 1.0f,
    val textOpacity       : Float   = 1.0f,
    val scale             : Float   = 1.0f,
    val cornerRadius      : Float   = 16f,
    val showHours         : Boolean = true,
    val showSeconds       : Boolean = true,
    val showButtons       : Boolean = true,
    val keepScreenOn      : Boolean = false,
    val autoLaunch        : Boolean = false,
    val timeLimitSeconds  : Long    = 0L,
    val isBeepEnabled     : Boolean = false,
    val isVibrationEnabled: Boolean = false,
    val lastX             : Int     = -1,
    val lastY             : Int     = -1,
    val totalLifetimeMs   : Long    = 0L,
    val currentCycleMs    : Long    = 0L,
    val lastUpdateCheck   : Long    = 0L,
    val focusModeEnabled  : Boolean = false,
    val selectedTheme     : String  = "AUTO",
    val donationPending   : Boolean = false, // Nova flag
)

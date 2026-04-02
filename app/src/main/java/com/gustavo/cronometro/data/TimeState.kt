package com.gustavo.cronometro.data

// ============================================================
// TimerState.kt
// elapsedMs agora é um campo real (não computado) para que
// o StateFlow detecte mudanças a cada tick e atualize a UI.
// ============================================================

data class TimerState(
    val startTime   : Long    = -1L,
    val pauseOffset : Long    = 0L,
    val isRunning   : Boolean = false,
    val isAtLimit   : Boolean = false,
    // Campo real — atualizado a cada 10ms pelo ViewModel.
    // Não é persistido (recalculado no restore via TimerPreferences).
    val elapsedMs   : Long    = 0L
)
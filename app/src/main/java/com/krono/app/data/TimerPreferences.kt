package com.krono.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

// ============================================================
// TimerPreferences.kt
// Correções:
//   • Usa extensão KTX edit { } em vez de edit().put...apply()
//   • saveStateSync e clearState usam edit(commit = true) { }
//     para escrita síncrona — mantém comportamento original
// ============================================================

class TimerPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private companion object {
        const val PREFS_NAME         = "timer_state"
        const val KEY_START_TIME     = "start_time"
        const val KEY_PAUSE_OFFSET   = "pause_offset"
        const val KEY_IS_RUNNING     = "is_running"
        const val KEY_IS_AT_LIMIT    = "is_at_limit"
        const val KEY_SERVICE_ACTIVE = "service_active"
    }

    fun loadState(): TimerState = TimerState(
        startTime   = prefs.getLong(KEY_START_TIME,   -1L),
        pauseOffset = prefs.getLong(KEY_PAUSE_OFFSET, 0L),
        isRunning   = prefs.getBoolean(KEY_IS_RUNNING,  false),
        isAtLimit   = prefs.getBoolean(KEY_IS_AT_LIMIT, false)
    )

    fun saveState(state: TimerState) {
        prefs.edit {
            putLong(KEY_START_TIME,   state.startTime)
            putLong(KEY_PAUSE_OFFSET, state.pauseOffset)
            putBoolean(KEY_IS_RUNNING,  state.isRunning)
            putBoolean(KEY_IS_AT_LIMIT, state.isAtLimit)
        }
    }

    // commit = true: escrita imediata no disco (processo prestes a encerrar)
    fun saveStateSync(state: TimerState) {
        prefs.edit(commit = true) {
            putLong(KEY_START_TIME,   state.startTime)
            putLong(KEY_PAUSE_OFFSET, state.pauseOffset)
            putBoolean(KEY_IS_RUNNING,  state.isRunning)
            putBoolean(KEY_IS_AT_LIMIT, state.isAtLimit)
        }
    }

    fun setServiceActive(active: Boolean) {
        prefs.edit {
            putBoolean(KEY_SERVICE_ACTIVE, active)
        }
    }

    // Usado pelo BootReceiver para decidir se relança o serviço
    fun isServiceActive(): Boolean =
        prefs.getBoolean(KEY_SERVICE_ACTIVE, false)

    fun clearState() {
        prefs.edit(commit = true) {
            putLong(KEY_START_TIME,        -1L)
            putLong(KEY_PAUSE_OFFSET,      0L)
            putBoolean(KEY_IS_RUNNING,     false)
            putBoolean(KEY_IS_AT_LIMIT,    false)
            putBoolean(KEY_SERVICE_ACTIVE, false)
        }
    }
}
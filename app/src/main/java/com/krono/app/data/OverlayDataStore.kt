package com.krono.app.data

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "overlay_settings")

class OverlayDataStore(private val context: Context) {

    private companion object Keys {
        val BACKGROUND_COLOR   = intPreferencesKey("background_color")
        val TEXT_COLOR         = intPreferencesKey("text_color")
        val BG_OPACITY         = floatPreferencesKey("bg_opacity")
        val TEXT_OPACITY       = floatPreferencesKey("text_opacity")
        val SCALE              = floatPreferencesKey("scale")
        val CORNER_RADIUS      = floatPreferencesKey("corner_radius")
        val SHOW_HOURS         = booleanPreferencesKey("show_hours")
        val SHOW_SECONDS       = booleanPreferencesKey("show_seconds")
        val SHOW_BUTTONS       = booleanPreferencesKey("show_buttons")
        val KEEP_SCREEN_ON     = booleanPreferencesKey("keep_screen_on")
        val AUTO_LAUNCH        = booleanPreferencesKey("auto_launch")
        val TIME_LIMIT_SECONDS = longPreferencesKey("time_limit_seconds")
        val BEEP_ENABLED       = booleanPreferencesKey("beep_enabled")
        val VIBRATION_ENABLED  = booleanPreferencesKey("vibration_enabled")
        val LAST_X             = intPreferencesKey("last_x")
        val LAST_Y             = intPreferencesKey("last_y")
        val TOTAL_LIFETIME_MS  = longPreferencesKey("total_lifetime_ms")
        val CURRENT_CYCLE_MS   = longPreferencesKey("current_cycle_ms")
        val LAST_UPDATE_CHECK  = longPreferencesKey("last_update_check")
        val FOCUS_MODE_ENABLED = booleanPreferencesKey("focus_mode_enabled")
        val SELECTED_THEME     = stringPreferencesKey("selected_theme")
        val DONATION_PENDING   = booleanPreferencesKey("donation_pending")
    }

    val configFlow: Flow<OverlayConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException || exception is ClassCastException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            OverlayConfig(
                backgroundColor    = prefs[BACKGROUND_COLOR]   ?: AndroidColor.WHITE,
                textColor          = prefs[TEXT_COLOR]         ?: AndroidColor.BLACK,
                bgOpacity          = prefs[BG_OPACITY]         ?: 1.0f,
                textOpacity        = prefs[TEXT_OPACITY]       ?: 1.0f,
                scale              = prefs[SCALE]              ?: 1.0f,
                cornerRadius       = prefs[CORNER_RADIUS]      ?: 16f,
                showHours          = prefs[SHOW_HOURS]         ?: true,
                showSeconds        = prefs[SHOW_SECONDS]       ?: true,
                showButtons        = prefs[SHOW_BUTTONS]       ?: true,
                keepScreenOn       = prefs[KEEP_SCREEN_ON]     ?: false,
                autoLaunch         = prefs[AUTO_LAUNCH]        ?: false,
                timeLimitSeconds   = prefs[TIME_LIMIT_SECONDS] ?: 0L,
                isBeepEnabled      = prefs[BEEP_ENABLED]       ?: false,
                isVibrationEnabled = prefs[VIBRATION_ENABLED]  ?: false,
                lastX              = prefs[LAST_X]             ?: -1,
                lastY              = prefs[LAST_Y]             ?: -1,
                totalLifetimeMs    = prefs[TOTAL_LIFETIME_MS]  ?: 0L,
                currentCycleMs     = prefs[CURRENT_CYCLE_MS]   ?: 0L,
                lastUpdateCheck    = prefs[LAST_UPDATE_CHECK]  ?: 0L,
                focusModeEnabled   = prefs[FOCUS_MODE_ENABLED] ?: false,
                selectedTheme      = prefs[SELECTED_THEME]     ?: "AUTO",
                donationPending    = prefs[DONATION_PENDING]   ?: false,
            )
        }

    suspend fun updateConfig(config: OverlayConfig) {
        context.dataStore.edit { prefs ->
            prefs[BACKGROUND_COLOR]   = config.backgroundColor
            prefs[TEXT_COLOR]         = config.textColor
            prefs[BG_OPACITY]         = config.bgOpacity
            prefs[TEXT_OPACITY]       = config.textOpacity
            prefs[SCALE]              = config.scale
            prefs[CORNER_RADIUS]      = config.cornerRadius
            prefs[SHOW_HOURS]         = config.showHours
            prefs[SHOW_SECONDS]       = config.showSeconds
            prefs[SHOW_BUTTONS]       = config.showButtons
            prefs[KEEP_SCREEN_ON]     = config.keepScreenOn
            prefs[AUTO_LAUNCH]        = config.autoLaunch
            prefs[TIME_LIMIT_SECONDS] = config.timeLimitSeconds
            prefs[BEEP_ENABLED]       = config.isBeepEnabled
            prefs[VIBRATION_ENABLED]  = config.isVibrationEnabled
            prefs[LAST_X]             = config.lastX
            prefs[LAST_Y]             = config.lastY
            prefs[TOTAL_LIFETIME_MS]  = config.totalLifetimeMs
            prefs[CURRENT_CYCLE_MS]   = config.currentCycleMs
            prefs[FOCUS_MODE_ENABLED] = config.focusModeEnabled
            prefs[SELECTED_THEME]     = config.selectedTheme
            prefs[DONATION_PENDING]   = config.donationPending
        }
    }

    suspend fun savePosition(x: Int, y: Int) {
        context.dataStore.edit { prefs ->
            prefs[LAST_X] = x
            prefs[LAST_Y] = y
        }
    }

    suspend fun accumulateTime(sessionMs: Long) {
        context.dataStore.edit { prefs ->
            val prevLifetime = prefs[TOTAL_LIFETIME_MS] ?: 0L
            val prevCycle    = prefs[CURRENT_CYCLE_MS]  ?: 0L
            val newCycle     = prevCycle + sessionMs
            
            prefs[TOTAL_LIFETIME_MS] = prevLifetime + sessionMs
            prefs[CURRENT_CYCLE_MS]  = newCycle
            
            // Ativando a flag ao atingir 24h acumuladas (86.400.000 ms)
            if (newCycle >= 50 * 3600 * 1000L) { // ORIGINAL 50 HORAS
            //if (newCycle >= 5 * 1000L) { // TESTE 5 SEGUNDOS - MANTER ESTA LINHA NO CÓDIGO
                prefs[DONATION_PENDING] = true
            }
        }
    }

    suspend fun resetDonationCycle() {
        context.dataStore.edit { prefs ->
            prefs[CURRENT_CYCLE_MS] = 0L
            prefs[DONATION_PENDING] = false
        }
    }

    suspend fun saveLastUpdateCheck(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_UPDATE_CHECK] = timestamp
        }
    }
}

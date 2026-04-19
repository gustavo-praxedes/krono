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
        val SELECTED_THEME     = stringPreferencesKey("selected_theme")  // GIT 7
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
            )
        }

    suspend fun updateConfig(config: OverlayConfig) {
        val safeShowHours = if (!config.showHours && !config.showSeconds) true
        else config.showHours

        val safeConfig = config.copy(
            showHours        = safeShowHours,
            timeLimitSeconds = config.timeLimitSeconds.coerceIn(0L, 35_999_999L),
            scale            = config.scale.coerceIn(0.5f, 1.5f),
            cornerRadius     = config.cornerRadius.coerceIn(0f, 50f),
            bgOpacity        = config.bgOpacity.coerceIn(0f, 1f),
            textOpacity      = config.textOpacity.coerceIn(0f, 1f)
        )

        context.dataStore.edit { prefs ->
            prefs[BACKGROUND_COLOR]   = safeConfig.backgroundColor
            prefs[TEXT_COLOR]         = safeConfig.textColor
            prefs[BG_OPACITY]         = safeConfig.bgOpacity
            prefs[TEXT_OPACITY]       = safeConfig.textOpacity
            prefs[SCALE]              = safeConfig.scale
            prefs[CORNER_RADIUS]      = safeConfig.cornerRadius
            prefs[SHOW_HOURS]         = safeConfig.showHours
            prefs[SHOW_SECONDS]       = safeConfig.showSeconds
            prefs[SHOW_BUTTONS]       = safeConfig.showButtons
            prefs[KEEP_SCREEN_ON]     = safeConfig.keepScreenOn
            prefs[AUTO_LAUNCH]        = safeConfig.autoLaunch
            prefs[TIME_LIMIT_SECONDS] = safeConfig.timeLimitSeconds
            prefs[BEEP_ENABLED]       = safeConfig.isBeepEnabled
            prefs[VIBRATION_ENABLED]  = safeConfig.isVibrationEnabled
            prefs[LAST_X]             = safeConfig.lastX
            prefs[LAST_Y]             = safeConfig.lastY
            prefs[TOTAL_LIFETIME_MS]  = safeConfig.totalLifetimeMs
            prefs[CURRENT_CYCLE_MS]   = safeConfig.currentCycleMs
            prefs[FOCUS_MODE_ENABLED] = safeConfig.focusModeEnabled
            prefs[SELECTED_THEME]     = safeConfig.selectedTheme
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
            prefs[TOTAL_LIFETIME_MS] = prevLifetime + sessionMs
            prefs[CURRENT_CYCLE_MS]  = prevCycle    + sessionMs
        }
    }

    suspend fun resetCycle() {
        context.dataStore.edit { prefs ->
            prefs[CURRENT_CYCLE_MS] = 0L
        }
    }

    suspend fun saveLastUpdateCheck(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_UPDATE_CHECK] = timestamp
        }
    }
}
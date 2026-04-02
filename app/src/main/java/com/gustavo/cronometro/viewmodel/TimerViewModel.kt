package com.gustavo.cronometro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.cronometro.data.TimerPreferences
import com.gustavo.cronometro.data.TimerState
import com.gustavo.cronometro.data.hasReachedLimit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ============================================================
// TimerViewModel.kt
// Gerencia a lógica do cronômetro com persistência resiliente.
//
// Correção principal: elapsedMs agora é um campo real no
// TimerState. O StateFlow detecta mudanças a cada tick porque
// o valor de elapsedMs muda, forçando recomposição da UI.
// ============================================================

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val timerPrefs = TimerPreferences(application)

    private val _timerState = MutableStateFlow(loadInitialState())


// Controle para testes em tempos longos.
/*    private val _timerState = MutableStateFlow(
        TimerState(
            startTime   = System.currentTimeMillis(),
            pauseOffset = 356_390_000L, // Ajuste conforme quiser em segundos
            isRunning   = true,
            elapsedMs   = 359_930_000L
        )
    )*/

    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // Emite a cada segundo completo — para beep e vibração
    private val _tickFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val tickFlow: SharedFlow<Unit> = _tickFlow.asSharedFlow()

    private var timerJob: Job? = null
    private var timeLimitSeconds: Long = 0L

    // ── Inicialização ────────────────────────────────────────
    init {
        // Retoma o loop se estava rodando antes do reboot/fechamento
        if (_timerState.value.isRunning) {
            startUpdateLoop()
        }
    }

    // Carrega o estado salvo e já calcula o elapsedMs correto
    // com base no startTime e tempo atual (sobrevive a reboots)
    private fun loadInitialState(): TimerState {
        val saved = timerPrefs.loadState()
        val elapsed = computeElapsed(saved)
        return saved.copy(elapsedMs = elapsed)
    }

    private fun computeElapsed(state: TimerState): Long = when {
        state.isAtLimit -> state.pauseOffset
        state.isRunning && state.startTime != -1L ->
            state.pauseOffset + (System.currentTimeMillis() - state.startTime)
        else -> state.pauseOffset
    }

    // ── API Pública ──────────────────────────────────────────

    // Agora recebe segundos em vez de horas
    fun setTimeLimit(seconds: Long) {
        timeLimitSeconds = seconds
    }

    fun start() {
        val current = _timerState.value
        if (current.isRunning || current.isAtLimit) return

        val newState = current.copy(
            startTime = System.currentTimeMillis(),
            isRunning = true
        )
        _timerState.value = newState
        timerPrefs.saveState(newState)
        startUpdateLoop()
    }

    fun pause() {
        val current = _timerState.value
        if (!current.isRunning) return

        val now         = System.currentTimeMillis()
        val accumulated = current.pauseOffset + (now - current.startTime)
        val newState = current.copy(
            startTime   = -1L,
            pauseOffset = accumulated,
            isRunning   = false,
            elapsedMs   = accumulated
        )
        _timerState.value = newState
        timerPrefs.saveState(newState)
        stopUpdateLoop()
    }

    fun reset() {
        stopUpdateLoop()
        val newState = TimerState() // zeros tudo, elapsedMs = 0
        _timerState.value = newState
        timerPrefs.clearState()
    }

    fun togglePlayPause() {
        if (_timerState.value.isRunning) pause() else start()
    }

    // ── Loop de Atualização ──────────────────────────────────
    // Roda a cada 10ms. Atualiza elapsedMs no TimerState,
    // o que faz o StateFlow emitir novo valor e a UI recompor.
    private fun startUpdateLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastSecond = _timerState.value.elapsedMs / 1000L

            while (true) {
                delay(10L)

                val current = _timerState.value
                if (!current.isRunning) break

                // Calcula elapsed em tempo real
                val elapsed = current.pauseOffset +
                        (System.currentTimeMillis() - current.startTime)

                // ── Limite de tempo ──────────────────────────
                // Verifica limite em segundos (0 = ilimitado)
                if (timeLimitSeconds > 0L && elapsed >= timeLimitSeconds * 1000L) {
                    val limitMs = timeLimitSeconds * 1000L
                    val limitState = current.copy(
                        pauseOffset = limitMs,
                        startTime   = -1L,
                        isRunning   = false,
                        isAtLimit   = true,
                        elapsedMs   = limitMs
                    )
                    _timerState.value = limitState
                    timerPrefs.saveStateSync(limitState)
                    break
                }

                // ── Tick de 1 segundo ────────────────────────
                val currentSecond = elapsed / 1000L
                if (currentSecond > lastSecond) {
                    lastSecond = currentSecond
                    _tickFlow.tryEmit(Unit)
                }

                // ── Atualiza elapsedMs — força recomposição ──
                // StateFlow emite porque elapsedMs mudou de valor
                _timerState.value = current.copy(elapsedMs = elapsed)
            }
        }
    }

    private fun stopUpdateLoop() {
        timerJob?.cancel()
        timerJob = null
    }

    // ── Ciclo de Vida ────────────────────────────────────────
    override fun onCleared() {
        super.onCleared()
        val current = _timerState.value
        if (current.isRunning) {
            val now         = System.currentTimeMillis()
            val accumulated = current.pauseOffset + (now - current.startTime)
            timerPrefs.saveStateSync(
                current.copy(
                    startTime   = -1L,
                    pauseOffset = accumulated,
                    isRunning   = false,
                    elapsedMs   = accumulated
                )
            )
        } else {
            timerPrefs.saveStateSync(current)
        }
        stopUpdateLoop()
    }
}
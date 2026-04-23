package com.krono.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krono.app.data.TimerPreferences
import com.krono.app.data.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val timerPrefs = TimerPreferences(application)

    private val _timerState = MutableStateFlow(loadInitialState())


// CONTROLE PARA TESTES DE TEMPOS LONGOS.
/*    private val _timerState = MutableStateFlow(
        TimerState(
            startTime   = System.currentTimeMillis(),
            pauseOffset = 356_390_000L, // Ajuste conforme quiser em segundos
            isRunning   = true,
            elapsedMs   = 359_930_000L
        )
    )*/

    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // Retorna o tempo decorrido desde o último start até agora.
    // Usado pelo MainService no momento do Reset para acumular
    // o tempo no DataStore antes de zerar o cronômetro.
    val currentSessionMs: Long
        get() {
            val current = _timerState.value
            return when {
                current.isRunning && current.startTime != -1L ->
                    current.pauseOffset + (System.currentTimeMillis() - current.startTime)
                else ->
                    current.pauseOffset
            }
        }

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

    // ── Loop de Atualização ──────────────────────────────────
    // Roda a cada 250ms (otimização de bateria para apps de produtividade).
    // Atualiza elapsedMs no TimerState, o que faz o StateFlow emitir
    // um novo valor e a UI recompor.
    private fun startUpdateLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {

            while (true) {
                delay(250L)

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
        // Salva o estado atual (rodando ou não) para persistência
        timerPrefs.saveStateSync(_timerState.value)
        stopUpdateLoop()
    }
}
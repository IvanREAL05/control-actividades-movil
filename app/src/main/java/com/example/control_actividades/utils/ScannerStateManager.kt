package com.example.control_actividades

import android.util.Log

/**
 * Singleton para controlar el estado del escáner a nivel global.
 * Previene que múltiples Activities interfieran entre sí.
 */
object ScannerStateManager {
    private var _isProcessing = false
    private var _currentActivityId: String? = null
    private var _requestInProgress = false  // Nueva bandera

    val isProcessing: Boolean
        get() = _isProcessing

    /**
     * Intenta bloquear el escáner.
     * Retorna true si pudo bloquear, false si ya estaba bloqueado.
     */
    fun tryLock(activityId: String): Boolean {
        synchronized(this) {
            if (_isProcessing) {
                Log.w("ScannerState", "Ya está procesando (Activity: $_currentActivityId)")
                return false
            }
            _isProcessing = true
            _requestInProgress = false
            _currentActivityId = activityId
            Log.d("ScannerState", "🔒 Bloqueado por Activity: $activityId")
            return true
        }
    }

    /**
     * Marca que se inició la petición HTTP.
     */
    fun markRequestStarted() {
        synchronized(this) {
            _requestInProgress = true
            Log.d("ScannerState", "🌐 Petición HTTP iniciada")
        }
    }

    /**
     * Desbloquea el escáner.
     */
    fun unlock(activityId: String) {
        synchronized(this) {
            if (_currentActivityId == activityId) {
                _isProcessing = false
                _requestInProgress = false
                _currentActivityId = null
                Log.d("ScannerState", "🔓 Desbloqueado por Activity: $activityId")
            } else {
                Log.w("ScannerState", "Intento de desbloqueo desde Activity incorrecta: $activityId (actual: $_currentActivityId)")
            }
        }
    }

    /**
     * Fuerza el desbloqueo SOLO si NO hay petición HTTP en progreso.
     */
    fun onActivityDestroyed(activityId: String) {
        synchronized(this) {
            if (_currentActivityId == activityId) {
                if (_requestInProgress) {
                    Log.w("ScannerState", "⚠️ Activity destruida pero petición HTTP activa - manteniendo bloqueo")
                    // NO desbloquear si hay petición activa
                } else {
                    _isProcessing = false
                    _currentActivityId = null
                    Log.d("ScannerState", "🔓 Desbloqueo por destrucción de Activity: $activityId")
                }
            }
        }
    }

    /**
     * Limpia el estado si la petición fue cancelada.
     */
    fun onRequestCancelled(activityId: String) {
        synchronized(this) {
            if (_currentActivityId == activityId) {
                _isProcessing = false
                _requestInProgress = false
                _currentActivityId = null
                Log.d("ScannerState", "🔓 Limpieza por cancelación de petición: $activityId")
            }
        }
    }
}
package com.example.control_actividades

import android.app.Application
import androidx.work.*
import com.example.control_actividades.workers.SyncWorker
import java.util.concurrent.TimeUnit
import android.util.Log

/**
 * Clase Application que se ejecuta al iniciar la app.
 * Configura la sincronización automática de registros pendientes.
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("MyApplication", "🚀 Aplicación iniciada")

        // Configurar sincronización periódica cada 15 minutos
        configurarSincronizacionAutomatica()
    }

    /**
     * Configura WorkManager para sincronizar registros pendientes
     * cada 15 minutos cuando haya conexión a internet.
     */
    private fun configurarSincronizacionAutomatica() {
        // Definir restricciones: solo sincronizar con internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Solo con internet
            .setRequiresBatteryNotLow(false) // No requiere batería alta
            .setRequiresCharging(false) // No requiere estar cargando
            .build()

        // Crear trabajo periódico cada 15 minutos
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES // Intervalo de 15 minutos
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10, TimeUnit.SECONDS // Si falla, reintentar después de 10 segundos
            )
            .addTag("sync_pendientes") // Tag para identificar el trabajo
            .build()

        // Programar el trabajo (no se duplicará si ya existe)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncPendientes", // Nombre único del trabajo
            ExistingPeriodicWorkPolicy.KEEP, // Mantener el trabajo existente si ya hay uno
            syncWorkRequest
        )

        Log.d("MyApplication", "✅ Sincronización automática configurada cada 15 minutos")
        Log.d("MyApplication", "📡 Solo sincronizará cuando haya conexión a internet")
    }
}
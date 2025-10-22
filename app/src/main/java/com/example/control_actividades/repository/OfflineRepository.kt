package com.example.control_actividades.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.control_actividades.database.AppDatabase
import com.example.control_actividades.database.PendingSyncEntity
import com.example.control_actividades.ApiService
import com.example.control_actividades.AsistenciaRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import com.example.control_actividades.EntregaRequest

/**
 * Resultado de operación de guardado
 */
sealed class SaveResult {
    object OnlineSuccess : SaveResult()
    object OfflineSaved : SaveResult()
    data class Error(val message: String) : SaveResult()
}

class OfflineRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val apiService: ApiService
) {

    private val gson = Gson()

    private fun tieneConexionInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun guardarAsistencia(qr: String, estado: String, idClase: Int): SaveResult {
        return withContext(Dispatchers.IO) {
            if (tieneConexionInternet()) {
                try {
                    Log.d("OfflineRepo", "🌐 Intentando enviar a la API...")

                    val request = AsistenciaRequest(
                        qr = qr,
                        estado = estado,
                        id_clase = idClase
                    )

                    val response = apiService.registrarAsistencia(request)

                    if (response.isSuccessful) {
                        Log.d("OfflineRepo", "✅ Enviado exitosamente a la API")
                        return@withContext SaveResult.OnlineSuccess
                    } else {
                        Log.w("OfflineRepo", "⚠️ API retornó error ${response.code()}, guardando offline")
                        guardarLocal("asistencia", qr, estado, idClase)
                        return@withContext SaveResult.OfflineSaved
                    }

                } catch (e: Exception) {
                    Log.e("OfflineRepo", "❌ Error de red: ${e.message}, guardando offline", e)
                    guardarLocal("asistencia", qr, estado, idClase)
                    return@withContext SaveResult.OfflineSaved
                }
            } else {
                Log.d("OfflineRepo", "💾 Sin internet, guardando localmente")
                guardarLocal("asistencia", qr, estado, idClase)
                return@withContext SaveResult.OfflineSaved
            }
        }
    }

    private suspend fun guardarLocal(tipo: String, qr: String, estado: String, idClase: Int) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("OfflineRepo", "💾 Guardando en base de datos local...")

                val dataMap = mapOf(
                    "qr" to qr,
                    "estado" to estado,
                    "id_clase" to idClase
                )

                val dataJson = gson.toJson(dataMap)

                val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())

                val entidad = PendingSyncEntity(
                    tipo = tipo,
                    data = dataJson,
                    fechaLocal = fechaActual,
                    timestamp = System.currentTimeMillis(),
                    sincronizado = false,
                    intentos = 0,
                    ultimoError = null
                )

                db.pendingSyncDao().insertRegistro(entidad)
                Log.d("OfflineRepo", "✅ Guardado localmente exitoso")

            } catch (e: Exception) {
                Log.e("OfflineRepo", "❌ Error al guardar local: ${e.message}", e)
            }
        }
    }

    suspend fun sincronizarPendientes() {
        if (!tieneConexionInternet()) {
            Log.d("OfflineRepo", "❌ No hay internet para sincronizar")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val pendientes = db.pendingSyncDao().getPendientesConMaxIntentos(maxIntentos = 5)

                if (pendientes.isEmpty()) {
                    Log.d("OfflineRepo", "✅ No hay registros pendientes")
                    return@withContext
                }

                Log.d("OfflineRepo", "🔄 Sincronizando ${pendientes.size} registros pendientes...")

                pendientes.forEach { pendiente ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val dataMap = gson.fromJson(pendiente.data, Map::class.java) as Map<String, Any>

                        when (pendiente.tipo) {
                            "asistencia" -> {
                                Log.d("OfflineRepo", "📤 Sincronizando asistencia ID ${pendiente.id}...")

                                val qr = dataMap["qr"] as? String ?: ""
                                val estado = dataMap["estado"] as? String ?: ""
                                val idClase = (dataMap["id_clase"] as? Double)?.toInt() ?: 0

                                val request = AsistenciaRequest(
                                    qr = qr,
                                    estado = estado,
                                    id_clase = idClase
                                )

                                val response = apiService.registrarAsistencia(request)

                                if (response.isSuccessful) {
                                    Log.d("OfflineRepo", "✅ Asistencia ${pendiente.id} sincronizada")
                                    db.pendingSyncDao().marcarSincronizado(pendiente.id)
                                } else {
                                    Log.w("OfflineRepo", "⚠️ Error HTTP ${response.code()} en asistencia ${pendiente.id}")
                                    db.pendingSyncDao().incrementarIntentos(
                                        pendiente.id,
                                        "HTTP ${response.code()}: ${response.message()}"
                                    )
                                }
                            }

                            "actividad" -> {
                                Log.d("OfflineRepo", "📤 Sincronizando actividad ID ${pendiente.id}...")

                                val qr = dataMap["qr"] as? String ?: ""
                                val idActividad = (dataMap["id_actividad"] as? Double)?.toInt() ?: 0
                                val calificacion = (dataMap["calificacion"] as? Double)?.toInt()

                                val request = EntregaRequest(qr, idActividad, calificacion)
                                val response = apiService.registrarEntrega(request)

                                if (response.success) {
                                    Log.d("OfflineRepo", "✅ Actividad ${pendiente.id} sincronizada")
                                    db.pendingSyncDao().marcarSincronizado(pendiente.id)
                                } else {
                                    Log.w("OfflineRepo", "⚠️ Error en actividad ${pendiente.id}: ${response.mensaje}")
                                    db.pendingSyncDao().incrementarIntentos(
                                        pendiente.id,
                                        "Error: ${response.mensaje}"
                                    )
                                }
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("OfflineRepo", "❌ Error al sincronizar ${pendiente.id}: ${e.message}", e)
                        db.pendingSyncDao().incrementarIntentos(
                            pendiente.id,
                            e.message ?: "Error desconocido"
                        )
                    }
                }

                val hace7Dias = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                db.pendingSyncDao().limpiarAntiguos(hace7Dias)

                Log.d("OfflineRepo", "✅ Sincronización completada")

            } catch (e: Exception) {
                Log.e("OfflineRepo", "❌ Error general en sincronización: ${e.message}", e)
            }
        }
    }

    suspend fun contarPendientes(): Int {
        return withContext(Dispatchers.IO) {
            db.pendingSyncDao().contarTodosPendientes()
        }
    }

    suspend fun limpiarSincronizados() {
        withContext(Dispatchers.IO) {
            db.pendingSyncDao().limpiarSincronizados()
        }
    }

    suspend fun guardarEntregaActividad(
        qr: String,
        idActividad: Int,
        calificacion: Int?
    ): SaveResult {
        return withContext(Dispatchers.IO) {
            if (tieneConexionInternet()) {
                try {
                    Log.d("OfflineRepo", "🌐 Intentando enviar actividad a la API...")

                    val request = EntregaRequest(qr, idActividad, calificacion)
                    val response = apiService.registrarEntrega(request)

                    if (response.success) {
                        Log.d("OfflineRepo", "✅ Actividad enviada exitosamente a la API")
                        return@withContext SaveResult.OnlineSuccess
                    } else {
                        Log.w("OfflineRepo", "⚠️ API retornó error, guardando offline")
                        guardarEntregaLocal(qr, idActividad, calificacion)
                        return@withContext SaveResult.OfflineSaved
                    }

                } catch (e: Exception) {
                    Log.e("OfflineRepo", "❌ Error de red en actividad: ${e.message}, guardando offline", e)
                    guardarEntregaLocal(qr, idActividad, calificacion)
                    return@withContext SaveResult.OfflineSaved
                }
            } else {
                Log.d("OfflineRepo", "💾 Sin internet, guardando actividad localmente")
                guardarEntregaLocal(qr, idActividad, calificacion)
                return@withContext SaveResult.OfflineSaved
            }
        }
    }

    private suspend fun guardarEntregaLocal(qr: String, idActividad: Int, calificacion: Int?) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("OfflineRepo", "💾 Guardando actividad en base de datos local...")

                val dataMap = mapOf(
                    "qr" to qr,
                    "id_actividad" to idActividad,
                    "calificacion" to (calificacion ?: 0)
                )

                val dataJson = gson.toJson(dataMap)

                val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())

                val entidad = PendingSyncEntity(
                    tipo = "actividad",
                    data = dataJson,
                    fechaLocal = fechaActual,
                    timestamp = System.currentTimeMillis(),
                    sincronizado = false,
                    intentos = 0,
                    ultimoError = null
                )

                db.pendingSyncDao().insertRegistro(entidad)
                Log.d("OfflineRepo", "✅ Actividad guardada localmente")

            } catch (e: Exception) {
                Log.e("OfflineRepo", "❌ Error al guardar actividad local: ${e.message}", e)
            }
        }
    }
}
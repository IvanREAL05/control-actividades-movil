package com.example.control_actividades.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Entidad que representa un registro pendiente de sincronizar con el servidor.
 * Puede ser una asistencia o una actividad entregada.
 */
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Tipo de registro: "asistencia" o "actividad"
    @ColumnInfo(name = "tipo")
    val tipo: String,

    // JSON con los datos completos del registro
    @ColumnInfo(name = "data")
    val data: String,

    // Fecha y hora local del registro (formato legible)
    @ColumnInfo(name = "fecha_local")
    val fechaLocal: String,

    // Timestamp en milisegundos (para ordenar y limpiar registros antiguos)
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    // Si ya se sincronizó correctamente o no
    @ColumnInfo(name = "sincronizado")
    val sincronizado: Boolean = false,

    // Número de intentos de sincronización fallidos
    @ColumnInfo(name = "intentos")
    val intentos: Int = 0,

    // Último error de sincronización (opcional, para debug)
    @ColumnInfo(name = "ultimo_error")
    val ultimoError: String? = null
)
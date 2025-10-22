package com.example.control_actividades.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update

/**
 * DAO para manejar los registros pendientes de sincronización.
 */
@Dao
interface PendingSyncDao {

    // Insertar nuevo registro pendiente
    @Insert
    suspend fun insertRegistro(pending: PendingSyncEntity)

    // Obtener todos los registros que aún no se han sincronizado
    // Ordenados por timestamp (los más antiguos primero)
    @Query("SELECT * FROM pending_sync WHERE sincronizado = 0 ORDER BY timestamp ASC")
    suspend fun getPendientes(): List<PendingSyncEntity>

    // Obtener registros pendientes que no hayan superado X intentos
    @Query("SELECT * FROM pending_sync WHERE sincronizado = 0 AND intentos < :maxIntentos ORDER BY timestamp ASC")
    suspend fun getPendientesConMaxIntentos(maxIntentos: Int = 5): List<PendingSyncEntity>

    // Marcar un registro como sincronizado
    @Query("UPDATE pending_sync SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarSincronizado(id: Int)

    // Incrementar contador de intentos fallidos
    @Query("UPDATE pending_sync SET intentos = intentos + 1, ultimo_error = :error WHERE id = :id")
    suspend fun incrementarIntentos(id: Int, error: String?)

    // Actualizar un registro completo
    @Update
    suspend fun actualizarRegistro(pending: PendingSyncEntity)

    // Eliminar un registro de la base local
    @Delete
    suspend fun eliminarRegistro(pending: PendingSyncEntity)

    // Eliminar por ID
    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun eliminarPorId(id: Int)

    // Borrar todos los registros sincronizados
    @Query("DELETE FROM pending_sync WHERE sincronizado = 1")
    suspend fun limpiarSincronizados()

    // Borrar registros antiguos (más de X días)
    @Query("DELETE FROM pending_sync WHERE timestamp < :timestampLimite AND sincronizado = 1")
    suspend fun limpiarAntiguos(timestampLimite: Long)

    // Contar registros pendientes por tipo
    @Query("SELECT COUNT(*) FROM pending_sync WHERE sincronizado = 0 AND tipo = :tipo")
    suspend fun contarPendientesPorTipo(tipo: String): Int

    // Contar TODOS los pendientes
    @Query("SELECT COUNT(*) FROM pending_sync WHERE sincronizado = 0")
    suspend fun contarTodosPendientes(): Int

    // Obtener registros con demasiados intentos fallidos (para alertar al usuario)
    @Query("SELECT * FROM pending_sync WHERE sincronizado = 0 AND intentos >= :maxIntentos")
    suspend fun getRegistrosConDemasiadosIntentos(maxIntentos: Int = 5): List<PendingSyncEntity>
}
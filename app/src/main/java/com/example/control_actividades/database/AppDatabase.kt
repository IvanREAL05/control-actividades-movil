package com.example.control_actividades.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos local de la app (Room)
 * Contiene las tablas necesarias para funcionamiento offline.
 */
@Database(entities = [PendingSyncEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de versión 1 a 2 (agregar nuevas columnas)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE pending_sync ADD COLUMN timestamp INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE pending_sync ADD COLUMN intentos INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE pending_sync ADD COLUMN ultimo_error TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "control_actividades_db"
                )
                    .addMigrations(MIGRATION_1_2) // Agrega la migración
                    .fallbackToDestructiveMigration() // Solo si no hay migración definida
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
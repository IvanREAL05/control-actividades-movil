package com.example.control_actividades.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.control_actividades.database.AppDatabase
import com.example.control_actividades.repository.OfflineRepository
import com.example.control_actividades.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://TU_BACKEND_AQUI/") // ⚠️ reemplaza con tu URL real
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(ApiService::class.java)

                // Pasar context, db y apiService
                val repository = OfflineRepository(applicationContext, db, apiService)

                repository.sincronizarPendientes()

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }
}
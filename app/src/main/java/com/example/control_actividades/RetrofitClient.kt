package com.example.control_actividades

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    //private const val BASE_URL = "https://control-asistenciav1.onrender.com/"
    //private const val BASE_URL = "http://10.0.2.2:8000/"
    private const val BASE_URL = "http://192.168.100.11:8000/"
    //private const val BASE_URL = "http://10.31.225.13:8000/"
    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}



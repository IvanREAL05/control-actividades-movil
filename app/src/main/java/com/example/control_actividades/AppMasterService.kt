package com.example.control_actividades

import retrofit2.Call
import retrofit2.http.GET

interface AppMasterService {
    @GET("exec")
    fun getAppEnabled(): Call<AppEnabledResponse>
}
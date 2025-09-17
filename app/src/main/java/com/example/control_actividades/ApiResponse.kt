package com.example.control_actividades

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)

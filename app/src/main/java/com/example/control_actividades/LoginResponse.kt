package com.example.control_actividades

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: DataResponse? // puede ser null si falla
)

data class DataResponse(
    val usuario: UsuarioResponse,
    val timestamp: String
)

data class UsuarioResponse(
    val id_usuario: Int,
    val nombre_completo: String,
    val correo: String,
    val usuario_login: String,
    val rol: String,
    val id_profesor: Int? = null,
    val nombre_profesor: String? = null
)

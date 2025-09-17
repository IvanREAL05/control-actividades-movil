package com.example.control_actividades

data class AvisoResponse(
    val data: List<Aviso>, // viene de `rows` en el backend
    val pagination: Pagination
)

data class Pagination(
    val totalItems: Int,
    val totalPages: Int,
    val currentPage: Int,
    val itemsPerPage: Int,
    val hasNextPage: Boolean,
    val hasPrevPage: Boolean
)

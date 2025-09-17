package com.example.control_actividades

data class ClasesHoyResponse(
    val success: Boolean,
    val data: ClasesHoyData
)

data class ClasesHoyData(
    val profesor: String,
    val dia: String,
    val clases: List<ClaseInfo>,
    val total: Int
)

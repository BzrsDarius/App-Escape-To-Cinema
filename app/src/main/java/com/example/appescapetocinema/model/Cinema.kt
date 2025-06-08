package com.example.appescapetocinema.model

// Clase de datos que representa un cine
data class Cinema(
    // Identificador único del cine
    val id: Long,
    // Nombre del cine
    val name: String,
    // Dirección del cine (puede incluir ciudad y código postal)
    val address: String?,
    // Distancia al cine en millas
    val distance: Double?,
    // Latitud de la ubicación del cine
    val latitude: Double?,
    // Longitud de la ubicación del cine
    val longitude: Double?,
    // URL del logo del cine
    val logoUrl: String?
)
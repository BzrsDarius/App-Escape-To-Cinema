// Paquete que define la ubicación de esta clase dentro del proyecto
package com.example.appescapetocinema.model

// Clase de datos que representa los resultados de horarios de un cine
data class CinemaShowtimeResultItem(
    // Identificador único del cine
    val cinemaId: Long,
    // Nombre del cine
    val cinemaName: String,
    // Distancia al cine en millas
    val distance: Double?,
    // Mapa formateado para fácil visualización: Ej: "Standard" -> "18:30 | 20:45"
    val formattedShowtimes: Map<String, String>
)
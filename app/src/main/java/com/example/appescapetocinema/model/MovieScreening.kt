// Paquete que define la ubicación de esta clase dentro del proyecto
package com.example.appescapetocinema.model

// Clase de datos que representa un horario específico
data class Showtime(
    // Hora de inicio del horario
    val startTime: String, // Ej: "19:30"
    // Hora de fin del horario (opcional)
    val endTime: String?
)

// Clase de datos que agrupa horarios por formato
data class ScreeningsByFormat(
    // Nombre del formato de proyección
    val formatName: String, // Ej: "2D", "3D", "VOSE"
    // Lista de horarios para este formato
    val times: List<Showtime>
)

// Clase de datos que representa la proyección de una película
data class MovieScreening(
    // Identificador único de MovieGlu para esta película
    val filmGluId: Long,
    // Nombre de la película
    val filmName: String,
    // Identificador de IMDb (opcional)
    val imdbId: String?,
    // URL de la imagen del póster de la película
    val posterImageUrl: String?,
    // Clasificación por edad de la película
    val ageRating: String?, // Ej: "12", "TP"
    // Duración de la película en minutos (opcional)
    val durationMins: Int?,
    // Lista de formatos y sus horarios para esta película en este cine
    val screeningFormats: List<ScreeningsByFormat>
)
package com.example.appescapetocinema.model

data class CinemaWithShowtimesForFilm(
    val cinemaId: Long,
    val cinemaName: String,
    val distance: Double?,
    val screeningsByFormat: List<ScreeningsByFormat> // Reutilizas tu modelo existente
)
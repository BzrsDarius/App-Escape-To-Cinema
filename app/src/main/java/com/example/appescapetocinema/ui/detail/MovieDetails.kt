package com.example.appescapetocinema.ui.detail

// Modelo más detallado para la pantalla de detalles
data class MovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?, // Imagen de fondo más ancha
    val releaseYear: String,
    val genres: List<String>,
    val rating: Double, // Ejemplo: 8.5
    val imdbId: String? = null // <-- AÑADE ESTE CAMPO
)
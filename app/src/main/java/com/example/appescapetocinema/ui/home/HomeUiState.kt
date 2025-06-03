package com.example.appescapetocinema.ui.home

import com.example.appescapetocinema.ui.components.MovieItem

// Estado para la pantalla de inicio
data class HomeUiState(
    val trendingMovies: List<MovieItem> = emptyList(),
    val classicMovies: List<MovieItem> = emptyList(),
    val recommendedMovies: List<MovieItem> = emptyList(),
    val isLoading: Boolean = true, // Empezar cargando por defecto
    val errorMessage: String? = null
    // Puedes añadir más listas o estados aquí si es necesario
)
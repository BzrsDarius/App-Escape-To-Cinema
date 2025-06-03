package com.example.appescapetocinema.ui.cinema

import com.example.appescapetocinema.model.MovieScreening

data class CinemaDetailUiState(
    val cinemaName: String? = null, // Nombre del cine (lo podríamos obtener de la lista anterior o de la API)
    val cinemaId: Long? = null, // Para referencia
    val date: String = "", // Fecha para la que se muestran los horarios
    val movieScreenings: List<MovieScreening> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
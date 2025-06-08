package com.example.appescapetocinema.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Clase que representa la respuesta de una API con una lista de películas
@Serializable
data class MovieListResponseDto(
    // Número de página actual
    val page: Int,

    // Lista de películas
    val results: List<MovieResultDto>,

    // Número total de páginas disponibles
    @SerialName("total_pages") val totalPages: Int,

    // Número total de resultados disponibles
    @SerialName("total_results") val totalResults: Int
)
package com.example.appescapetocinema.network.dto

import kotlinx.serialization.Serializable

// Clase que representa un género con dos propiedades: id y name
@Serializable
data class GenreDto(
    // Identificador único del género
    val id: Int,
    // Nombre del género
    val name: String
)

// Clase que representa la respuesta de una API que contiene una lista de géneros
@Serializable
data class GenresResponseDto(
    // Lista de objetos GenreDto
    val genres: List<GenreDto>
)
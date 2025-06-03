// En app/src/main/java/com/example/appescapetocinema/network/dto/MovieDetailsDto.kt
package com.example.appescapetocinema.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Clase que representa los detalles de una película
@Serializable
data class MovieDetailsDto(
    // Identificador único de la película
    val id: Int,

    // Título de la película. Puede ser nulo si no hay información disponible.
    val title: String?,

    // Descripción general de la película. Puede ser nula si no hay información disponible.
    val overview: String?,

    // Ruta al póster de la película. Puede ser nula si no hay información disponible.
    @SerialName("poster_path") val posterPath: String?,

    // Ruta al fondo de la película. Puede ser nula si no hay información disponible.
    @SerialName("backdrop_path") val backdropPath: String?,

    // Fecha de lanzamiento de la película. Puede ser nula si no hay información disponible.
    @SerialName("release_date") val releaseDate: String?,

    // Promedio de votos de la película. Puede ser nulo si no hay información disponible.
    @SerialName("vote_average") val voteAverage: Double?,

    // Lista de géneros asociados a la película. Puede ser nula si no hay información disponible.
    val genres: List<GenreDto>?,

    // Duración de la película en minutos. Puede ser nula si no hay información disponible.
    val runtime: Int?,

    // Estado de la película (ej. "Released", "Post Production"). Puede ser nulo si no hay información disponible.
    val status: String?,

    // Identificador único de la película en IMDb. Puede ser nulo si no hay información disponible.
    @SerialName("imdb_id") val imdbId: String? = null
)
// En network/dto/FindResponseDto.kt (o donde pongas tus DTOs)
package com.example.appescapetocinema.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FindResponseDto(
    // Lista de resultados de películas encontradas. Puede ser nula si no hay resultados.
    @SerialName("movie_results")
    val movieResults: List<FoundMovieResultDto>? = null
)

@Serializable
data class FoundMovieResultDto(
    // El ID único de la película en TMDb. Puede ser nulo si no hay información disponible.
    val id: Int?,

    // El título de la película. Puede ser nulo si no hay información disponible.
    val title: String?,

    // La ruta al póster de la película. Puede ser nulo si no hay información disponible.
    @SerialName("poster_path")
    val posterPath: String?,

    // La fecha de lanzamiento de la película. Puede ser nula si no hay información disponible.
    @SerialName("release_date")
    val releaseDate: String?,

    // Indica si la película es para adultos. Puede ser nulo si no hay información disponible.
    val adult: Boolean? = null,

    // El idioma original de la película. Puede ser nulo si no hay información disponible.
    @SerialName("original_language")
    val originalLanguage: String? = null,

    // El título original de la película. Puede ser nulo si no hay información disponible.
    @SerialName("original_title")
    val originalTitle: String? = null,

    // Una descripción general de la película. Puede ser nula si no hay información disponible.
    val overview: String? = null,

    // La popularidad de la película. Puede ser nula si no hay información disponible.
    val popularity: Double? = null,

    // La ruta al fondo de la película. Puede ser nulo si no hay información disponible.
    @SerialName("backdrop_path")
    val backdropPath: String? = null,

    // El promedio de votos de la película. Puede ser nulo si no hay información disponible.
    @SerialName("vote_average")
    val voteAverage: Double? = null,

    // El número de votos de la película. Puede ser nulo si no hay información disponible.
    @SerialName("vote_count")
    val voteCount: Int? = null,

    // Indica si la película tiene un video asociado. Puede ser nulo si no hay información disponible.
    val video: Boolean? = null
)
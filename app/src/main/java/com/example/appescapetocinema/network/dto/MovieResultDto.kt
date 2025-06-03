// En app/src/main/java/com/example/appescapetocinema/network/dto/MovieResultDto.kt
package com.example.appescapetocinema.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Clase que representa un resultado de película en una lista de películas
@Serializable
data class MovieResultDto(
    // Identificador único de la película
    val id: Int,

    // Título de la película. Puede ser nulo si no hay información disponible.
    val title: String?,

    // Ruta al póster de la película. Puede ser nula si no hay información disponible.
    @SerialName("poster_path") val posterPath: String?,

    // Ruta al fondo de la película. Puede ser nula si no hay información disponible.
    @SerialName("backdrop_path") val backdropPath: String?,

    // Descripción general de la película. Puede ser nula si no hay información disponible.
    @SerialName("overview") val overview: String?,

    // Fecha de lanzamiento de la película. Puede ser nula si no hay información disponible.
    @SerialName("release_date") val releaseDate: String?,

    // Promedio de votos de la película. Puede ser nulo si no hay información disponible.
    @SerialName("vote_average") val voteAverage: Double?

    // Otros campos que puedan ser necesarios de la API (ej. genre_ids, etc.)
)
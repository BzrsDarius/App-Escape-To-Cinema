package com.example.appescapetocinema.network.movieglu_dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowtimeDetailDto(
    @SerialName("start_time") val startTime: String = "", // Añadir default
    @SerialName("end_time") val endTime: String? = null   // Ya era nullable, default es bueno
)

@Serializable
data class ShowingsByFormatDto(
    val times: List<ShowtimeDetailDto>? // Lista de horarios para ESTE formato/película
)

@Serializable
data class AgeRatingDto(
    val rating: String?,
    @SerialName("age_rating_image") val ageRatingImage: String?,
    @SerialName("age_advisory") val ageAdvisory: String?
)

@Serializable
data class FilmShowingDto(
    @SerialName("film_id") val filmId: Long,
    @SerialName("film_name") val filmName: String,
    @SerialName("imdb_title_id") val imdbTitleId: String?,
    @SerialName("other_titles") val otherTitles: Map<String, String>? = null,
    @SerialName("version_type") val versionType: String? = null,


    // Hacemos la lista nullable, y el objeto AgeRatingDto dentro también nullable
    // por si el array viniera vacío o con objetos incompletos.
    @SerialName("age_rating") val ageRatings: List<AgeRatingDto?>? = null,
    @SerialName("film_image") val filmImage: String?,
    @SerialName("film_image_height") val filmImageHeight: Int?,
    @SerialName("film_image_width") val filmImageWidth: Int?,
    @SerialName("duration_mins") val durationMins: Int?,
    @SerialName("showings") val showingsByFormat: Map<String, ShowingTimesDto>?
)



@Serializable
data class ShowingTimesDto( // Objeto que está bajo cada clave de formato ("Standard", "3D")
    val times: List<ShowtimeDetailDto>?
)


// --- Para GET cinemaShowTimes ---
@Serializable
data class CinemaDetailMovieGluDto(
    @SerialName("cinema_id") val cinemaId: Long,
    @SerialName("cinema_name") val cinemaName: String
)

@Serializable
data class CinemaShowtimesResponseDto(
    val cinema: CinemaDetailMovieGluDto?, // Detalle del cine solicitado
    val films: List<FilmShowingDto> // Lista de películas con sus horarios para ESE cine
)

@Serializable
data class FilmShowtimeCinemaDto( // Representa un cine dentro de la lista de 'filmShowTimes'
    @SerialName("cinema_id") val cinemaId: Long,
    @SerialName("cinema_name") val cinemaName: String,
    val distance: Double?,
    @SerialName("showings") val showingsByFormat: Map<String, ShowingTimesDto>?
)

@Serializable
data class FilmShowtimesResponseDto(
    // --- Detalles de la película solicitada ---
    @SerialName("film_id") val filmId: Long,
    @SerialName("film_name") val filmName: String,
    @SerialName("imdb_title_id") val imdbTitleId: String?,
    @SerialName("other_titles") val otherTitles: Map<String, String>? = null,
    @SerialName("version_type") val versionType: String? = null,
    @SerialName("age_rating") val ageRating: AgeRatingDto?,
    @SerialName("film_image") val filmImage: String?,
    @SerialName("film_image_height") val filmImageHeight: Int?,
    @SerialName("film_image_width") val filmImageWidth: Int?,

    val cinemas: List<FilmShowtimeCinemaDto> // Lista de cines que proyectan esta película
)
package com.example.appescapetocinema.network.movieglu_dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CinemaMovieGluDto(
    @SerialName("cinema_id") val cinemaId: Long,
    @SerialName("cinema_name") val cinemaName: String,
    val address: String?,
    val address2: String?,
    val city: String?,
    val county: String?,
    val postcode: String?,
    @SerialName("lat") val latitude: Double?, // Mapea "lat" del JSON a "latitude"
    @SerialName("lng") val longitude: Double?,// Mapea "lng" del JSON a "longitude"
    val distance: Double?,
    @SerialName("logo_url") val logoUrl: String?
)

@Serializable
data class CinemasNearbyResponseDto( // Para GET cinemasNearby
    val cinemas: List<CinemaMovieGluDto>,
    val status: StatusDto? = null
)

@Serializable
data class StatusDto(
    val count: Int?,
    val state: String?,
    val message: String?
)
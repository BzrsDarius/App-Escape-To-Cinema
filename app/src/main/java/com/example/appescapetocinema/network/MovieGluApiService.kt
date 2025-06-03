package com.example.appescapetocinema.network

import com.example.appescapetocinema.BuildConfig
import com.example.appescapetocinema.network.movieglu_dto.CinemasNearbyResponseDto
import com.example.appescapetocinema.network.movieglu_dto.CinemaShowtimesResponseDto
import com.example.appescapetocinema.network.movieglu_dto.FilmShowtimesResponseDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface MovieGluApiService {

    companion object {
        const val BASE_URL = "https://api-gate2.movieglu.com/" // URL base de la API MovieGlu
        const val API_VERSION_MOVIEGLU = "v201" // Versión de la API
        const val TERRITORY_MOVIEGLU = "ES" // Territorio para las solicitudes

        // Valores constantes para la autenticación y cliente
        const val CLIENT_MOVIEGLU = BuildConfig.MOVIEGLU_CLIENT // Identificador del cliente desde local.properties
        const val AUTHORIZATION_HEADER_MOVIEGLU = BuildConfig.MOVIEGLU_AUTHORIZATION_HEADER // Header de autorización desde local.properties

        // Método helper para obtener la fecha y hora actual en formato ISO
        fun getCurrentDeviceDateTime(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }
    }

    // Endpoint para obtener cines cercanos
    @GET("cinemasNearby/")
    suspend fun getNearbyCinemas(
        @Header("client") client: String = CLIENT_MOVIEGLU, // Header del cliente
        @Header("x-api-key") apiKey: String = BuildConfig.MOVIEGLU_API_KEY, // API Key desde BuildConfig
        @Header("authorization") authorization: String = AUTHORIZATION_HEADER_MOVIEGLU, // Header de autorización
        @Header("api-version") apiVersion: String = API_VERSION_MOVIEGLU, // Versión de la API
        @Header("territory") territoryHeader: String = TERRITORY_MOVIEGLU, // Territorio de la solicitud
        @Header("device-datetime") deviceDateTime: String, // Fecha y hora del dispositivo
        @Header("geolocation") geolocation: String, // Coordenadas geográficas (latitud;longitud)
        @Query("n") count: Int? = 3 // Número de resultados deseados
    ): CinemasNearbyResponseDto?

    // Endpoint para obtener horarios de un cine específico
    @GET("cinemaShowTimes/")
    suspend fun getCinemaShowtimes(
        @Header("client") client: String = CLIENT_MOVIEGLU, // Header del cliente
        @Header("x-api-key") apiKey: String = BuildConfig.MOVIEGLU_API_KEY, // API Key desde BuildConfig
        @Header("authorization") authorization: String = AUTHORIZATION_HEADER_MOVIEGLU, // Header de autorización
        @Header("api-version") apiVersion: String = API_VERSION_MOVIEGLU, // Versión de la API
        @Header("territory") territoryHeader: String = TERRITORY_MOVIEGLU, // Territorio de la solicitud
        @Header("device-datetime") deviceDateTime: String, // Fecha y hora del dispositivo
        @Query("cinema_id") cinemaId: Long, // ID del cine
        @Query("date") date: String, // Fecha en formato YYYY-MM-DD
        @Query("film_id") filmId: Long? = null, // ID de la película (opcional)
        @Query("sort") sort: String? = null // Orden de los resultados (opcional)
    ): CinemaShowtimesResponseDto

    // Endpoint para obtener horarios de una película específica
    @GET("filmShowTimes/")
    suspend fun getFilmShowtimes(
        @Header("client") client: String = CLIENT_MOVIEGLU, // Header del cliente
        @Header("x-api-key") apiKey: String = BuildConfig.MOVIEGLU_API_KEY, // API Key desde BuildConfig
        @Header("authorization") authorization: String = AUTHORIZATION_HEADER_MOVIEGLU, // Header de autorización
        @Header("api-version") apiVersion: String = API_VERSION_MOVIEGLU, // Versión de la API
        @Header("territory") territoryHeader: String = TERRITORY_MOVIEGLU, // Territorio de la solicitud
        @Header("device-datetime") deviceDateTime: String, // Fecha y hora del dispositivo
        @Header("geolocation") geolocation: String, // Coordenadas geográficas (latitud;longitud)
        @Query("film_id") filmId: Long, // ID de la película
        @Query("date") date: String, // Fecha en formato YYYY-MM-DD
        @Query("n") count: Int? = 10 // Número de resultados deseados
    ): FilmShowtimesResponseDto
}
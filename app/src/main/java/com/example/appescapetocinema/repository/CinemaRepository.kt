package com.example.appescapetocinema.repository

import retrofit2.HttpException
import com.example.appescapetocinema.model.Cinema
import com.example.appescapetocinema.model.MovieScreening
import com.example.appescapetocinema.model.ScreeningsByFormat
import com.example.appescapetocinema.model.Showtime
import com.example.appescapetocinema.network.MovieGluApiService
import com.example.appescapetocinema.network.NetworkModule
import com.example.appescapetocinema.network.movieglu_dto.CinemaMovieGluDto
import com.example.appescapetocinema.network.movieglu_dto.FilmShowingDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.appescapetocinema.network.movieglu_dto.CinemasNearbyResponseDto
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// --- Interfaz ---
interface CinemaRepository {
    /**
     * Obtiene cines cercanos a la ubicación dada.
     */
    suspend fun getNearbyCinemas(latitude: Double, longitude: Double, count: Int = 10): Result<List<Cinema>>

    /**
     * Obtiene la cartelera (películas y horarios) para un cine y fecha específicos.
     */
    suspend fun getCinemaShowtimes(cinemaId: Long, date: String): Result<List<MovieScreening>>

    /**
     * (Opcional) Obtiene los cines y horarios para una película específica en una fecha.
     * Esto podría ser útil si el usuario busca una película y luego quiere ver dónde se proyecta.
     */
    // suspend fun getFilmShowtimesNearby(filmId: Long, date: String, latitude: Double, longitude: Double, count: Int = 5): Result<List<CinemaWithShowtimesForFilm>>
    // data class CinemaWithShowtimesForFilm(val cinema: Cinema, val screenings: List<ScreeningsByFormat>)
}


// --- Implementación ---
class CinemaRepositoryImpl(
    // Inyectamos la interfaz del servicio MovieGlu
    private val movieGluApiService: MovieGluApiService = NetworkModule.movieGluApiService,
    // Podríamos inyectar las claves/cliente si no los ponemos como default en la interfaz
    // private val movieGluClient: String = BuildConfig.MOVIEGLU_CLIENT,
    // private val movieGluApiKey: String = BuildConfig.MOVIEGLU_API_KEY,
    // private val movieGluAuthorization: String = "TERRITORYPASS ${BuildConfig.MOVIEGLU_TERRITORY_PASS_KEY}"
) : CinemaRepository {

    // Helper para formatear la fecha y hora del dispositivo para el header
    private fun getCurrentDeviceDateTimeFormatted(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    // Helper para formatear la geolocalización para el header
    private fun formatGeolocation(latitude: Double, longitude: Double): String {
        return "${String.format(Locale.US, "%.6f", latitude)};${String.format(Locale.US, "%.6f", longitude)}"
    }

    // Mapeo de CinemaMovieGluDto a Cinema (modelo UI)
    private fun mapToCinema(dto: CinemaMovieGluDto): Cinema {
        val fullAddress = listOfNotNull(dto.address, dto.address2, dto.city, dto.postcode)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        return Cinema(
            id = dto.cinemaId,
            name = dto.cinemaName,
            address = fullAddress.ifEmpty { null },
            distance = dto.distance,
            latitude = dto.latitude,
            longitude = dto.longitude,
            logoUrl = dto.logoUrl
        )
    }

    // Mapeo de FilmShowingDto a MovieScreening (modelo UI)
    private fun mapToMovieScreening(dto: FilmShowingDto): MovieScreening {
        val screeningFormats = dto.showingsByFormat?.map { (formatName, timesDto) ->
            ScreeningsByFormat(
                formatName = formatName,
                times = timesDto.times?.map { Showtime(it.startTime, it.endTime) } ?: emptyList()
            )
        } ?: emptyList()
        val firstValidRating = dto.ageRatings?.firstOrNull()?.rating
        return MovieScreening(
            filmGluId = dto.filmId,
            filmName = dto.filmName,
            imdbId = dto.imdbTitleId,
            posterImageUrl = dto.filmImage,
            ageRating = firstValidRating, // <-- Usa el primer rating de la lista
            durationMins = dto.durationMins,
            screeningFormats = screeningFormats
        )
    }


    override suspend fun getNearbyCinemas(latitude: Double, longitude: Double, count: Int): Result<List<Cinema>> {
        Log.d("CinemaRepository", "getNearbyCinemas: Lat=$latitude, Lon=$longitude, Count=$count")
        val deviceDateTime = getCurrentDeviceDateTimeFormatted()
        val geolocation = formatGeolocation(latitude, longitude)

        return try {
            // Llama a la función que AHORA puede devolver null
            val responseDto : CinemasNearbyResponseDto? = withContext(Dispatchers.IO) { // <-- El tipo aquí es nullable
                movieGluApiService.getNearbyCinemas(
                    deviceDateTime = deviceDateTime,
                    geolocation = geolocation,
                    count = count
                )
            }

            if (responseDto != null) {
                // Procesa la respuesta normal
                val cinemas = responseDto.cinemas.map { mapToCinema(it) }
                Log.d("CinemaRepository", "getNearbyCinemas: Éxito, ${cinemas.size} cines encontrados.")
                Result.success(cinemas)
            } else {
                // Si responseDto es null (por 204 No Content u otro motivo)
                Log.w("CinemaRepository", "getNearbyCinemas: Respuesta nula o sin contenido (probablemente 204). Devolviendo lista vacía.")
                Result.success(emptyList()) // Devuelve éxito con lista vacía
            }
        } catch (e: HttpException) { // <-- CAPTURA HttpException ESPECÍFICAMENTE
            Log.e("CinemaRepository", "getNearbyCinemas: HttpException - Código: ${e.code()}", e)
            val errorMessage = if (e.code() == 429) {
                // Mensaje específico para el límite de tasa excedido
                "Se ha excedido el límite de solicitudes al servicio de cines. Por favor, inténtalo más tarde."
            } else {
                // Mensaje más genérico para otros errores HTTP
                "Error ${e.code()} al obtener cines: ${e.message()}"
            }
            Result.failure(Exception(errorMessage, e))
        } catch (e: IOException) { // Captura errores de red (ej. no hay conexión, timeout)
            Log.e("CinemaRepository", "getNearbyCinemas: Error de red (IOException)", e)
            Result.failure(Exception("Error de red al conectar con el servicio de cines: ${e.message}", e))
        } catch (e: Exception) { // Captura otras excepciones inesperadas (ej. de deserialización si el DTO es incorrecto)
            Log.e("CinemaRepository", "getNearbyCinemas: Error general en procesamiento", e)
            Result.failure(Exception("Error inesperado al obtener cines: ${e.message}", e))
        }
    }

    override suspend fun getCinemaShowtimes(cinemaId: Long, date: String): Result<List<MovieScreening>> {
        Log.d("CinemaRepository", "getCinemaShowtimes: CinemaID=$cinemaId, Date=$date")
        val deviceDateTime = getCurrentDeviceDateTimeFormatted()

        return try {
            val response = withContext(Dispatchers.IO) {
                movieGluApiService.getCinemaShowtimes(
                    deviceDateTime = deviceDateTime,
                    cinemaId = cinemaId,
                    date = date
                    // Los headers fijos se toman de los defaults
                )
            }
            // Mapear la lista de DTOs a lista de Modelos UI
            val movieScreenings = response.films.map { mapToMovieScreening(it) }
            Log.d("CinemaRepository", "getCinemaShowtimes: Éxito, ${movieScreenings.size} películas con horarios encontradas.")
            Result.success(movieScreenings)
        } catch (e: Exception) {
            Log.e("CinemaRepository", "getCinemaShowtimes: Error", e)
            Result.failure(Exception("Error al obtener cartelera del cine: ${e.message}", e))
        }
    }

}
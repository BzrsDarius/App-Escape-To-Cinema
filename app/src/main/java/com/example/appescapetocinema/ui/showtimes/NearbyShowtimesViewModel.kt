package com.example.appescapetocinema.ui.showtimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.appescapetocinema.repository.CinemaRepository
import com.example.appescapetocinema.model.ScreeningsByFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import com.example.appescapetocinema.model.CinemaShowtimeResultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NearbyShowtimesViewModel(
    private val cinemaRepository: CinemaRepository,
    private val movieTmdbId: Long, // Podría ser útil en el futuro
    private val movieTitle: String,
    private val movieImdbId: String // Hacerlo no nulo aquí, la navegación no debería ocurrir si es nulo
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyShowtimesUiState(movieTitle = movieTitle))
    val uiState: StateFlow<NearbyShowtimesUiState> = _uiState.asStateFlow()

    private fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun formatShowtimesForUi(screenings: List<ScreeningsByFormat>): Map<String, String> {
        return screenings.associate { format ->
            val timesString = format.times.joinToString(" | ") { it.startTime.substringBeforeLast(":") } // Quitar segundos si los hay
            format.formatName to timesString
        }.filter { it.value.isNotEmpty() }
    }

    // --- Permisos y Ubicación (adaptado de CinemasViewModel) ---
    fun onLocationPermissionResult(isGranted: Boolean) {
        Log.d("NearbyShowtimesVM", "Permiso ubicación: $isGranted")
        _uiState.update { it.copy(locationPermissionGranted = isGranted, isRequestingLocation = false, error = if (!isGranted) "Permiso de ubicación necesario." else null) }
        // La lógica para obtener ubicación si se concede está en el Container
    }

    fun setRequestingLocation(isRequesting: Boolean) {
        _uiState.update { it.copy(isRequestingLocation = isRequesting) }
    }

    // --- Lógica Principal de Búsqueda (Plan B) ---
    fun findNearbyShowtimes(latitude: Double, longitude: Double) {
        if (!_uiState.value.locationPermissionGranted) {
            _uiState.update { it.copy(error = "Permiso denegado.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null, noResultsFound = false) }

        viewModelScope.launch {
            Log.d("NearbyShowtimesVM", "Ejecutando Plan B para IMDB: $movieImdbId")
            val nearbyCinemasResult = cinemaRepository.getNearbyCinemas(latitude, longitude, count = 20) // Ajusta el count si es necesario

            if (nearbyCinemasResult.isFailure) {
                _uiState.update { it.copy(isLoading = false, error = "Error al buscar cines cercanos: ${nearbyCinemasResult.exceptionOrNull()?.message}") }
                return@launch
            }

            val nearbyCinemas = nearbyCinemasResult.getOrThrow()
            if (nearbyCinemas.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, noResultsFound = true) }
                return@launch
            }

            val results = mutableListOf<CinemaShowtimeResultItem>()
            val date = getCurrentDateFormatted()

            nearbyCinemas.forEach { cinema ->
                val showtimesResult = cinemaRepository.getCinemaShowtimes(cinema.id, date)
                if (showtimesResult.isSuccess) {
                    val screeningsInCinema = showtimesResult.getOrThrow()
                    val relevantScreening = screeningsInCinema.find { it.imdbId == movieImdbId }

                    if (relevantScreening != null && relevantScreening.screeningFormats.any { it.times.isNotEmpty() }) { // Asegura que haya horarios
                        results.add(
                            CinemaShowtimeResultItem(
                                cinemaId = cinema.id,
                                cinemaName = cinema.name,
                                distance = cinema.distance,
                                formattedShowtimes = formatShowtimesForUi(relevantScreening.screeningFormats)
                            )
                        )
                    }
                } else {
                    Log.w("NearbyShowtimesVM", "Error al obtener cartelera para cine ${cinema.id}: ${showtimesResult.exceptionOrNull()?.message}")
                }
            }

            Log.d("NearbyShowtimesVM", "Plan B completado. Encontrados ${results.size} cines con la película.")
            // Ordenar por distancia si existe
            results.sortBy { it.distance }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    showtimeResults = results,
                    noResultsFound = results.isEmpty(),
                    error = null // Limpiar error si la búsqueda (aunque vacía) fue exitosa
                )
            }
        }
    }
    // --- Fin Lógica Principal ---

    fun retry(latitude: Double?, longitude: Double?) {
        if (latitude != null && longitude != null) {
            findNearbyShowtimes(latitude, longitude)
        } else {
            _uiState.update { it.copy(isLoading = false, error = "No se pudo obtener ubicación para reintentar.") }
        }
    }

    // --- Factory ---
    companion object {
        fun Factory(
            cinemaRepository: CinemaRepository,
            movieTmdbId: Long,
            movieTitle: String,
            movieImdbId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NearbyShowtimesViewModel::class.java)) {
                    return NearbyShowtimesViewModel(cinemaRepository, movieTmdbId, movieTitle, movieImdbId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class for NearbyShowtimes")
            }
        }
    }
}
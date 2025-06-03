package com.example.appescapetocinema.ui.cinemas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appescapetocinema.repository.CinemaRepository // Importar Interfaz
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

class CinemasViewModel(
    private val cinemaRepository: CinemaRepository // Inyectar el repositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(CinemasUiState())
    val uiState: StateFlow<CinemasUiState> = _uiState.asStateFlow()

    // --- Permisos y Ubicación ---
    fun onLocationPermissionResult(isGranted: Boolean) {
        Log.d("CinemasViewModel", "Permiso de ubicación concedido: $isGranted")
        _uiState.update { it.copy(locationPermissionGranted = isGranted, isRequestingLocation = false) }
        if (isGranted) {
            // Si se concedió el permiso, podríamos intentar obtener la ubicación aquí
            // si no la estamos obteniendo ya desde la UI.
        } else {
            _uiState.update { it.copy(errorMessage = "Permiso de ubicación necesario para encontrar cines cercanos.") }
        }
    }

    fun setRequestingLocation(isRequesting: Boolean) {
        _uiState.update { it.copy(isRequestingLocation = isRequesting) }
    }

    fun updateLastKnownLocation(latitude: Double?, longitude: Double?) {
        if (latitude != null && longitude != null) {
            Log.d("CinemasViewModel", "Ubicación actualizada: Lat=$latitude, Lon=$longitude")
            _uiState.update { it.copy(lastKnownLatitude = latitude, lastKnownLongitude = longitude) }
            // Una vez que tenemos la ubicación, podríamos auto-cargar los cines
            fetchNearbyCinemas(latitude, longitude)
        } else {
            Log.w("CinemasViewModel", "Intento de actualizar con ubicación nula.")
        }
    }

    // --- Cargar Cines Cercanos ---
    fun fetchNearbyCinemas(latitude: Double, longitude: Double, count: Int = 10) {
        if (!_uiState.value.locationPermissionGranted) {
            _uiState.update { it.copy(errorMessage = "Permiso de ubicación denegado.") }
            return
        }

        Log.d("CinemasViewModel", "fetchNearbyCinemas: Lat=$latitude, Lon=$longitude")
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = cinemaRepository.getNearbyCinemas(latitude, longitude, count)
            result.fold(
                onSuccess = { cinemas ->
                    Log.d("CinemasViewModel", "Cines cercanos cargados: ${cinemas.size}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            nearbyCinemas = cinemas,
                            errorMessage = if (cinemas.isEmpty()) "No se encontraron cines cercanos." else null
                        )
                    }
                },
                onFailure = { error ->
                    Log.e("CinemasViewModel", "Error cargando cines cercanos", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            nearbyCinemas = emptyList(),
                            errorMessage = "Error al buscar cines: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    // Retry general, o reintenta obtener cines si tenemos ubicación
    fun retry() {
        val lat = _uiState.value.lastKnownLatitude
        val lon = _uiState.value.lastKnownLongitude
        if (lat != null && lon != null) {
            fetchNearbyCinemas(lat, lon)
        } else {
            _uiState.update { it.copy(errorMessage = "No se pudo obtener la ubicación para reintentar.") }
        }
    }
}
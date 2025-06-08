package com.example.appescapetocinema.ui.cinema

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.appescapetocinema.repository.CinemaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import com.example.appescapetocinema.repository.MovieRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class CinemaDetailNavEvent {
    data class NavigateToMovieDetail(val tmdbId: Int) : CinemaDetailNavEvent()
}

// Clave para el argumento de navegación
const val CINEMA_ID_ARG = "cinemaId"

class CinemaDetailViewModel(
    private val cinemaRepository: CinemaRepository,
    private val movieRepository: MovieRepository, // Inyectado
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CinemaDetailUiState()) // Asumiendo que CinemaDetailUiState está definido
    val uiState: StateFlow<CinemaDetailUiState> = _uiState.asStateFlow()

    private val _navEvent = Channel<CinemaDetailNavEvent>(Channel.BUFFERED)
    val navEvent: Flow<CinemaDetailNavEvent> = _navEvent.receiveAsFlow()

    private val cinemaId: Long? = savedStateHandle[CINEMA_ID_ARG]

    private fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    init {
        Log.d("CinemaDetailVM", "Iniciando ViewModel para CinemaID: $cinemaId")
        val currentDate = getCurrentDateFormatted()
        _uiState.update { it.copy(cinemaId = cinemaId, date = currentDate) }


        if (cinemaId != null) {
            fetchShowtimes(cinemaId, currentDate)
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "ID de cine no válido.") }
        }
    }

    fun fetchShowtimes(id: Long, date: String) {
        Log.d("CinemaDetailVM", "fetchShowtimes para CinemaID: $id, Fecha: $date")
        _uiState.update { it.copy(isLoading = true, errorMessage = null, date = date) }

        viewModelScope.launch {
            val result = cinemaRepository.getCinemaShowtimes(id, date)
            result.fold(
                onSuccess = { screenings ->
                    Log.d("CinemaDetailVM", "Cartelera obtenida: ${screenings.size} películas.")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            movieScreenings = screenings,
                            errorMessage = if (screenings.isEmpty()) "No hay cartelera para esta fecha." else null
                        )
                    }
                },
                onFailure = { error ->
                    Log.e("CinemaDetailVM", "Error cargando cartelera", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            movieScreenings = emptyList(),
                            errorMessage = "Error al cargar cartelera: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun onMovieScreeningClicked(imdbId: String?) {
        if (imdbId.isNullOrBlank()) {
            Log.w("CinemaDetailVM", "onMovieScreeningClicked con imdbId nulo o vacío.")
            return
        }

        Log.d("CinemaDetailVM", "onMovieScreeningClicked, buscando TMDb ID para IMDb: $imdbId")
        viewModelScope.launch {
            val result = movieRepository.findTmdbIdViaApi(imdbId) // Llama al método que usa tu API

            result.fold(
                onSuccess = { tmdbId ->
                    if (tmdbId != null) {
                        Log.d("CinemaDetailVM", "TMDb ID encontrado (vía API): $tmdbId. Emitiendo evento de navegación.")
                        _navEvent.send(CinemaDetailNavEvent.NavigateToMovieDetail(tmdbId))
                    } else {
                        Log.w("CinemaDetailVM", "No se encontró TMDb ID para IMDb (vía API): $imdbId")
                        _uiState.update { it.copy(errorMessage = "No se encontraron detalles adicionales para esta película.") }
                    }
                },
                onFailure = { error ->
                    Log.e("CinemaDetailVM", "Error buscando TMDb ID para IMDb (vía API): $imdbId", error)
                    _uiState.update { it.copy(errorMessage = "Error al buscar detalles: ${error.localizedMessage}") }
                }
            )
        }
    }

    fun retry() {
        cinemaId?.let { id -> fetchShowtimes(id, _uiState.value.date.ifEmpty { getCurrentDateFormatted() }) }
    }

    companion object {
        fun Factory(
            cinemaRepository: CinemaRepository,
            movieRepository: MovieRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(CinemaDetailViewModel::class.java)) {
                    val savedStateHandle = extras.createSavedStateHandle()
                    return CinemaDetailViewModel(cinemaRepository, movieRepository, savedStateHandle) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class for CinemaDetailViewModel")
            }
        }
    }
}
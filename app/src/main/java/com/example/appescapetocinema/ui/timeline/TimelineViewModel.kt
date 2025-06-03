package com.example.appescapetocinema.ui.timeline

import android.app.Application // Necesario para AndroidViewModel
import androidx.lifecycle.AndroidViewModel // Cambiar ViewModel a AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.appescapetocinema.model.TimelineEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.IOException
import android.util.Log
import androidx.lifecycle.ViewModel

class TimelineViewModel(
    application: Application // Recibir Application
) : AndroidViewModel(application) { // Heredar de AndroidViewModel

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    // Json parser configurado para ser permisivo con campos desconocidos y otros detalles
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true // Útil si un Int? es "" en el JSON, lo trata como null
    }

    init {
        loadTimelineEventsFromJson()
    }

    private fun loadTimelineEventsFromJson() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val events = readTimelineEventsFromAssets()
                // Ordenar por año, luego por mes (si existe), luego por día (si existe)
                val sortedEvents = events.sortedWith(
                    compareBy<TimelineEvent> { it.year }
                        .thenByDescending { it.month ?: 0 } // Meses más altos (diciembre) primero dentro del año si se quiere
                        .thenByDescending { it.day ?: 0 }  // Días más altos primero
                )
                _uiState.update {
                    it.copy(isLoading = false, events = sortedEvents)
                }
            } catch (e: Exception) {
                Log.e("TimelineViewModel", "Error al cargar o parsear eventos de la línea de tiempo", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Error: ${e.localizedMessage}")
                }
            }
        }
    }

    private suspend fun readTimelineEventsFromAssets(): List<TimelineEvent> {
        val context = getApplication<Application>().applicationContext
        val fileName = "timeline_events.json"
        return withContext(Dispatchers.IO) {
            try {
                Log.d("TimelineViewModel", "Leyendo $fileName desde assets...")
                val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
                Log.d("TimelineViewModel", "Parseando JSON para TimelineEvents...")
                val parsedEvents = jsonParser.decodeFromString<List<TimelineEvent>>(jsonString)
                Log.d("TimelineViewModel", "Parseadas ${parsedEvents.size} TimelineEvents.")
                parsedEvents
            } catch (e: IOException) {
                Log.e("TimelineViewModel", "IOException al leer $fileName", e)
                emptyList()
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.e("TimelineViewModel", "SerializationException al parsear $fileName", e)
                emptyList()
            } catch (e: Exception) {
                Log.e("TimelineViewModel", "Excepción general al procesar $fileName", e)
                emptyList()
            }
        }
    }

    fun retryLoad() {
        loadTimelineEventsFromJson()
    }

    // Factory actualizada para AndroidViewModel
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(TimelineViewModel::class.java)) {
                    // Obtener Application de CreationExtras
                    val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                    return TimelineViewModel(application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
package com.example.appescapetocinema.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appescapetocinema.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

class NewsViewModel(
    private val newsRepository: NewsRepository // Inyecta el repositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState()) // Inicializa con isLoading = true
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    init {
        loadNews() // Carga noticias al iniciar
    }

    fun loadNews() {
        Log.d("NewsViewModel", "Cargando noticias...")
        // Asegura mostrar carga y limpiar errores previos al recargar
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = newsRepository.getLatestNews() // Llama al repo (que devuelve datos de ejemplo)
            result.fold(
                onSuccess = { articles ->
                    Log.d("NewsViewModel", "Noticias cargadas: ${articles.size}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            articles = articles, // Actualiza la lista de artículos
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    Log.e("NewsViewModel", "Error cargando noticias", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            articles = emptyList(), // Vacía la lista en caso de error
                            errorMessage = "No se pudieron cargar las noticias: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun refreshNews() {
        loadNews()
    }
}
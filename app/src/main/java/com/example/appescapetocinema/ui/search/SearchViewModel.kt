package com.example.appescapetocinema.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.appescapetocinema.repository.MovieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import com.example.appescapetocinema.model.DEFAULT_SORT_BY
import com.example.appescapetocinema.repository.UserProfileRepository
import com.example.appescapetocinema.ui.components.MovieItem

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val movieRepository: MovieRepository,
    private val userProfileRepository: UserProfileRepository // Añadir
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private data class FilterParams(
        val query: String = "",
        val genreId: Int? = null,
        val year: Int? = null,
        val minRating: Float? = null,
        val sortBy: String = DEFAULT_SORT_BY // Incluye sortBy aquí también
    )

    private val filterFlow: StateFlow<FilterParams> = combine(
        _uiState.map { it.searchQuery }.debounce(300L), // Debounce para la query de texto
        _uiState.map { it.selectedGenreId },
        _uiState.map { it.selectedYear },
        _uiState.map { it.selectedMinRating },
        _uiState.map { it.sortBy }
    ) { query, genreId, year, rating, sort ->
        FilterParams(query, genreId, year, rating, sort)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FilterParams()
    )

    // Flow de Resultados Paginados
    val searchResultsFlow: Flow<PagingData<MovieItem>> = filterFlow
        .onEach { params ->
            // Lógica para logros
            checkAndTriggerFilteredSearchAchievement(params) // Para el logro de búsquedas filtradas
            if (params.year != null) {
                Log.d("SearchViewModel", "Búsqueda con año ${params.year} detectada. Registrando década para logro.")
                viewModelScope.launch {
                    userProfileRepository.addSearchedDecadeAndCheckAchievement(params.year).onFailure { error ->
                        Log.e("SearchViewModel", "Error al registrar década buscada para logro", error)
                    }
                }
            }
        }
        .flatMapLatest { params ->
            Log.d("SearchViewModel", "flatMapLatest: Params=$params")
            when {
                // 1. Si HAY Filtros de Descubrimiento (Género O Año O Rating) -> LLAMA A DISCOVER
                params.genreId != null || params.year != null || params.minRating != null -> {
                    Log.d("SearchViewModel", "-> Calling DISCOVER with Genre ${params.genreId}, Year ${params.year}, Rating ${params.minRating}, SortBy ${params.sortBy}")
                    movieRepository.discoverMoviesStream(
                        genreId = params.genreId,
                        year = params.year,
                        minRating = params.minRating,
                        sortBy = params.sortBy
                    )
                }
                // 2. Si NO hay filtros de descubrimiento, PERO SÍ query de texto -> LLAMA A SEARCH
                params.query.isNotBlank() -> {
                    Log.d("SearchViewModel", "-> Calling SEARCH with Query '${params.query}'")
                    movieRepository.searchMoviesStream(params.query)
                }
                // 3. Si NO hay filtros NI query -> MOSTRAR POPULARES POR DEFECTO
                else -> {
                    Log.d("SearchViewModel", "-> No filters/query, fetching POPULAR movies by default.")
                    movieRepository.getPopularMoviesStream()
                }
            }
        }
        .cachedIn(viewModelScope)

    init {
        loadGenres()
    }


    private fun checkAndTriggerFilteredSearchAchievement(params: FilterParams) {
        // Definir qué se considera una "búsqueda con filtros"
        // Condición: al menos un filtro de descubrimiento está activo O la query de texto tiene > N caracteres
        // Y que la búsqueda no sea vacía
        val hasDiscoveryFilters = params.genreId != null || params.year != null || params.minRating != null
        val hasMeaningfulQuery = params.query.length > 2 // Evitar contar por cada letra tecleada si la query dispara búsqueda

        // Solo contar si se va a realizar una búsqueda real (no un flow vacío)
        val willPerformSearch = hasDiscoveryFilters || params.query.isNotBlank()


        if (willPerformSearch && (hasDiscoveryFilters || hasMeaningfulQuery /*ajusta esta condición si es necesario*/)) {
            Log.d("SearchViewModel", "Búsqueda con filtros detectada: $params. Incrementando contador para logro.")
            viewModelScope.launch {
                userProfileRepository.incrementFilteredSearchCountAndCheckAchievement().onFailure { error ->
                    Log.e("SearchViewModel", "Error al incrementar contador de búsqueda filtrada", error)
                }
                // Si incrementFilteredSearchCountAndCheckAchievement devolviera un booleano
                // indicando "newlyUnlocked", podrías emitir aquí el _achievementUnlockedEvent
            }
        }
    }

    private fun loadGenres() {
        _uiState.update { it.copy(isLoadingGenres = true) }
        viewModelScope.launch {
            val result = movieRepository.getMovieGenres()
            result.fold(
                onSuccess = { genres ->
                    Log.d("SearchViewModel", "Géneros cargados: ${genres.size}")
                    _uiState.update { it.copy(isLoadingGenres = false, genreList = genres) }
                },
                onFailure = { error ->
                    Log.e("SearchViewModel", "Error cargando géneros", error)
                    _uiState.update { it.copy(isLoadingGenres = false) }
                }
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        if (_uiState.value.searchQuery != query) {
            _uiState.update { it.copy(searchQuery = query) }
        }
    }

    fun onGenreSelected(genreId: Int?) {
        val currentGenreId = _uiState.value.selectedGenreId
        val newGenreId = if (currentGenreId == genreId) null else genreId
        if (currentGenreId != newGenreId) {
            Log.d("SearchViewModel", "onGenreSelected: Nuevo ID = $newGenreId")
            _uiState.update { it.copy(selectedGenreId = newGenreId) }
        }
    }

    fun onYearSelected(year: Int?) {
        val currentYear = _uiState.value.selectedYear
        val newYear = if (currentYear == year && year != null) null else year
        if (currentYear != newYear) {
            Log.d("SearchViewModel", "onYearSelected: Nuevo Año = $newYear")
            _uiState.update { it.copy(selectedYear = newYear) }
        }
    }

    fun onMinRatingChange(rating: Float?) {
        // Convertir 0 a null para "sin filtro"
        val newRating = rating?.takeIf { it > 0f }
        if (_uiState.value.selectedMinRating != newRating) {
            Log.d("SearchViewModel", "onMinRatingChange: Nuevo Rating = $newRating")
            _uiState.update { it.copy(selectedMinRating = newRating, genresError = null) }
        }
    }

    // --- Cambiar Criterio de Ordenación ---
    fun onSortByChange(newSortBy: String) {
        if (newSortBy.isNotBlank() && _uiState.value.sortBy != newSortBy) {
            Log.d("SearchViewModel", "onSortByChange: Nuevo sortBy = $newSortBy")
            _uiState.update { it.copy(sortBy = newSortBy, genresError = null) } // Actualiza y limpia error de géneros
        }
    }

    fun clearSearchAndFilters() {
        Log.d("SearchViewModel", "clearSearchAndFilters")
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedGenreId = null,
                selectedYear = null,
                selectedMinRating = null,
                sortBy = DEFAULT_SORT_BY, // <-- Resetea a default
                genresError = null
            )
        }
    }


    fun retry() {
        if (_uiState.value.genreList.isEmpty() && _uiState.value.genresError != null) {
            loadGenres()
        }
    }
}
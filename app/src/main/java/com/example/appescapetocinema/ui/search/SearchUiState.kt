package com.example.appescapetocinema.ui.search

import com.example.appescapetocinema.model.DEFAULT_SORT_BY
import com.example.appescapetocinema.repository.Genre

data class SearchUiState(
    val searchQuery: String = "",
    val isLoadingGenres: Boolean = false,
    val genreList: List<Genre> = emptyList(),
    val selectedGenreId: Int? = null,
    val selectedYear: Int? = null,
    val selectedMinRating: Float? = null, // Rating mínimo (0.0-10.0) o null
    val sortBy: String = DEFAULT_SORT_BY,
    val genresError: String? = null // Error específico de carga de géneros

)
package com.example.appescapetocinema.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appescapetocinema.repository.MovieRepository // Importa la interfaz
import com.example.appescapetocinema.repository.UserProfileRepository

class SearchViewModelFactory(
    private val movieRepository: MovieRepository, // Recibe la interfaz MovieRepository
    private val userProfileRepository: UserProfileRepository // Recibe la interfaz UserProfileRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(
                movieRepository,
                userProfileRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for SearchViewModelFactory")
    }
}
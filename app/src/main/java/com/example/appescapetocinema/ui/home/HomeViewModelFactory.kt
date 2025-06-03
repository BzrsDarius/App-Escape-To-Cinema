package com.example.appescapetocinema.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appescapetocinema.repository.MovieRepository // Importa la INTERFAZ

class HomeViewModelFactory(
    // Recibe la interfaz del repositorio
    private val movieRepository: MovieRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pasa la instancia del repositorio al ViewModel
            return HomeViewModel(movieRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for HomeViewModelFactory")
    }
}
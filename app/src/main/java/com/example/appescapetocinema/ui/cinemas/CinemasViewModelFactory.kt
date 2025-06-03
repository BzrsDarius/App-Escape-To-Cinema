package com.example.appescapetocinema.ui.cinemas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appescapetocinema.repository.CinemaRepository // Importar Interfaz

class CinemasViewModelFactory(
    private val cinemaRepository: CinemaRepository // Recibe la interfaz
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CinemasViewModel::class.java)) {
            return CinemasViewModel(cinemaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for CinemasViewModelFactory")
    }
}
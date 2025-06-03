package com.example.appescapetocinema.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appescapetocinema.repository.NewsRepository // Importa Interfaz

class NewsViewModelFactory(
    private val newsRepository: NewsRepository // Recibe la interfaz
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            // Pasa la instancia del repo al ViewModel
            return NewsViewModel(newsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for NewsViewModelFactory")
    }
}
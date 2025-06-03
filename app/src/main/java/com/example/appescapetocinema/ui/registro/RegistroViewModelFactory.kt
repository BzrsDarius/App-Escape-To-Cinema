package com.example.appescapetocinema.ui.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase

class RegistroViewModelFactory(
    private val repositorioAuth: RepositorioAutenticacionFirebase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistroViewModel(repositorioAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for RegistroViewModelFactory")
    }
}
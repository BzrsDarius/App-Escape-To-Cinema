package com.example.appescapetocinema.ui.profile

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase
import com.example.appescapetocinema.repository.MovieRepository
import com.example.appescapetocinema.repository.UserRepository
import com.example.appescapetocinema.repository.UserProfileRepository

class ProfileViewModelFactory(
    private val application: Application,
    private val movieRepository: MovieRepository,
    private val userRepository: UserRepository,
    private val authRepository: RepositorioAutenticacionFirebase,
    private val userProfileRepository: UserProfileRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(
                application,
                movieRepository,
                userRepository,
                authRepository,
                userProfileRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for ProfileViewModelFactory")
    }
}
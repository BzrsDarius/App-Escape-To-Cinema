package com.example.appescapetocinema.util

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SnackbarMessage(val message: String, val duration: SnackbarDuration = SnackbarDuration.Short)

@Singleton
class AppSnackbarManager @Inject constructor() {

    private val _messages = MutableSharedFlow<SnackbarMessage>() // Permite re-emitir si hay múltiples observadores o si se necesita
    val messages: SharedFlow<SnackbarMessage> = _messages.asSharedFlow()

    suspend fun showMessage(text: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        _messages.emit(SnackbarMessage(message = text, duration = duration))
    }
}
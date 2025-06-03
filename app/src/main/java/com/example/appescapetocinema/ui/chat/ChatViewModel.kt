package com.example.appescapetocinema.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.appescapetocinema.model.ChatMessage
import com.example.appescapetocinema.model.SenderType
import com.example.appescapetocinema.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputTextChange(newText: String) {
        _uiState.update { it.copy(currentInput = newText, errorMessage = null) }
    }

    fun sendMessage() {
        val userInput = _uiState.value.currentInput.trim()
        if (userInput.isBlank()) {
            return // No enviar mensajes vacíos
        }

        // Añadir mensaje del usuario a la UI inmediatamente
        val userMessage = ChatMessage(text = userInput, sender = SenderType.USER)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                currentInput = "", // Limpiar el campo de texto
                isBotTyping = true, // Bot empieza a "escribir"
                inputEnabled = false, // Deshabilitar input mientras el bot responde
                errorMessage = null
            )
        }

        // Enviar mensaje al bot a través del repositorio
        viewModelScope.launch {
            val result = chatRepository.sendQueryToBot(userInput)
            result.fold(
                onSuccess = { botReplyText ->
                    val botMessage = ChatMessage(text = botReplyText, sender = SenderType.BOT)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + botMessage,
                            isBotTyping = false,
                            inputEnabled = true
                        )
                    }
                },
                onFailure = { error ->
                    Log.e("ChatViewModel", "Error al obtener respuesta del bot", error)
                    val errorMessage = ChatMessage(
                        text = "CineBot no está disponible en este momento. Inténtalo más tarde. (Error: ${error.localizedMessage ?: "desconocido"})",
                        sender = SenderType.ERROR // Mostrar como un mensaje de error en el chat
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + errorMessage,
                            isBotTyping = false,
                            inputEnabled = true
                        )
                    }
                }
            )
        }
    }
}
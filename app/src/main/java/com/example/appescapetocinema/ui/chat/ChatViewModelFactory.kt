package com.example.appescapetocinema.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appescapetocinema.repository.ChatRepository
import com.example.appescapetocinema.repository.ChatRepositoryImpl // Importa la implementación

// Factory para ChatViewModel
class ChatViewModelFactory(
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(chatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for ChatViewModelFactory")
    }
}
package com.example.appescapetocinema.repository

import android.util.Log
import com.example.appescapetocinema.network.NetworkModule
import com.example.appescapetocinema.network.TmdbApiService
import com.example.appescapetocinema.network.dto.ChatQueryRequestDto
import java.io.IOException

class ChatRepositoryImpl(
    // Obtenemos la instancia de TmdbApiService de la misma forma que en MovieRepositoryImpl
    private val tmdbApiService: TmdbApiService = NetworkModule.tmdbApiService
) : ChatRepository {

    override suspend fun sendQueryToBot(userQuery: String): Result<String> {
        if (userQuery.isBlank()) {
            Log.w("ChatRepositoryImpl", "Intento de enviar query vacía al bot.")
            return Result.failure(IllegalArgumentException("La consulta no puede estar vacía."))
        }

        Log.d("ChatRepositoryImpl", "Enviando query al bot: '$userQuery'")
        return try {
            val requestDto = ChatQueryRequestDto(query = userQuery)
            val response = tmdbApiService.sendChatQuery(requestDto) // Llama a la función de tu TmdbApiService

            if (response.isSuccessful) {
                val botReply = response.body()
                if (botReply != null) {
                    Log.d("ChatRepositoryImpl", "Respuesta del bot obtenida: '$botReply'")
                    Result.success(botReply)
                } else {
                    Log.w("ChatRepositoryImpl", "Respuesta de chat exitosa (código ${response.code()}) pero cuerpo nulo.")
                    Result.failure(IOException("La respuesta del servidor para el chat estaba inesperadamente vacía."))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Respuesta de error desconocida"
                Log.e("ChatRepositoryImpl", "Error API Chat: ${response.code()} - ${response.message()}. Body: $errorBody")
                Result.failure(IOException("Error ${response.code()} del servidor al contactar al chatbot: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "Excepción en sendQueryToBot: ${e.javaClass.simpleName} - ${e.localizedMessage}", e)
            Result.failure(e) // Envuelve la excepción original en Result.failure
        }
    }
}
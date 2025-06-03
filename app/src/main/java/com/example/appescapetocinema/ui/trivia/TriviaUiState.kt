package com.example.appescapetocinema.ui.trivia

import com.example.appescapetocinema.model.TriviaQuestion

const val DEFAULT_TIME_PER_QUESTION = 15

data class TriviaUiState(
    val currentQuestion: TriviaQuestion? = null,
    val currentScore: Int = 0,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val selectedAnswerIndex: Int? = null,
    val answerSubmitted: Boolean = false,
    val isAnswerCorrect: Boolean? = null,
    val isGameOver: Boolean = false,
    val isLoading: Boolean = true,
    val livesLeft: Int = 0 ,// Vidas restantes
    val maxTimePerQuestion: Int = DEFAULT_TIME_PER_QUESTION, // Tiempo total para la pregunta
    val timeLeft: Int = DEFAULT_TIME_PER_QUESTION, // Tiempo restante actual
    val timeRanOut: Boolean = false // Flag para indicar si se acabó el tiempo en la pregunta actual
)
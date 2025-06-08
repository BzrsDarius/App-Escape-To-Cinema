package com.example.appescapetocinema.ui.trivia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.appescapetocinema.model.TriviaQuestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import android.util.Log
import androidx.lifecycle.ViewModel
import java.io.IOException
import kotlinx.coroutines.Job
import com.example.appescapetocinema.repository.UserProfileRepository
import com.example.appescapetocinema.repository.UserProfileRepositoryImpl
import com.example.appescapetocinema.util.ACHIEVEMENT_TRIVIA_MASTER_GENERAL
import com.example.appescapetocinema.util.ACHIEVEMENT_TRIVIA_STREAK_10
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive

// Clave para el argumento de categoría
const val TRIVIA_CATEGORY_ARG = "category"

// Hereda de AndroidViewModel para acceder al contexto/assets
class TriviaViewModel(
    application: Application, // Contexto para leer assets
    private val savedStateHandle: SavedStateHandle, // Para obtener categoría
    private val userProfileRepository: UserProfileRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TriviaUiState())
    val uiState: StateFlow<TriviaUiState> = _uiState.asStateFlow()

    private var allQuestions: List<TriviaQuestion> = emptyList()
    private var currentRoundQuestions: List<TriviaQuestion> = emptyList()
    private val questionsPerRound = 10
    private val initialLives = 3

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val selectedCategory: String = savedStateHandle[TRIVIA_CATEGORY_ARG] ?: "all"

    private var timerJob: Job? = null
    private var currentCorrectStreak = 0 // Variable para la racha actual

    // Notificar a la UI sobre logros desbloqueados
    private val _achievementUnlockedEvent = Channel<String>(Channel.BUFFERED)
    val achievementUnlockedEvent: Flow<String> = _achievementUnlockedEvent.receiveAsFlow()

    init {
        Log.d("TriviaViewModel", "Iniciando ViewModel para Categoría: $selectedCategory")
        loadQuestionsAndStartGame()
    }

    private fun loadQuestionsAndStartGame() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            allQuestions = loadTriviaQuestionsFromAssets()

            if (allQuestions.isEmpty()) {
                Log.e("TriviaViewModel", "No se pudieron cargar preguntas desde assets.")
                _uiState.update { it.copy(isLoading = false, isGameOver = true, currentQuestion = null) }
                return@launch
            }
            prepareNewRound()
        }
    }

    private suspend fun loadTriviaQuestionsFromAssets(): List<TriviaQuestion> {
        val context = getApplication<Application>().applicationContext
        val fileName = "trivia_questions.json"
        return withContext(Dispatchers.IO) {
            try {
                Log.d("TriviaViewModel", "Leyendo archivo $fileName desde assets...")
                val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
                Log.d("TriviaViewModel", "Archivo JSON leído, parseando...")
                val questions = json.decodeFromString<List<TriviaQuestion>>(jsonString)
                Log.d("TriviaViewModel", "Parseo JSON exitoso. ${questions.size} preguntas cargadas.")
                questions
            } catch (e: IOException) {
                Log.e("TriviaViewModel", "Error leyendo archivo $fileName desde assets", e)
                emptyList()
            } catch (e: Exception) {
                Log.e("TriviaViewModel", "Error parseando JSON de $fileName", e)
                emptyList()
            }
        }
    }

    private fun prepareNewRound() {
        Log.d("TriviaViewModel", "Preparando nueva ronda para categoría: $selectedCategory")
        currentCorrectStreak = 0 // Resetear racha al inicio de una nueva ronda

        val filteredQuestions = if (selectedCategory.equals("all", ignoreCase = true)) { allQuestions }
        else { allQuestions.filter { it.category.equals(selectedCategory, ignoreCase = true) } }
        currentRoundQuestions = filteredQuestions.shuffled().take(questionsPerRound)

        if (currentRoundQuestions.isNotEmpty()) {
            _uiState.update {
                TriviaUiState(
                    isLoading = false,
                    currentQuestion = currentRoundQuestions[0],
                    currentQuestionIndex = 0,
                    totalQuestions = currentRoundQuestions.size,
                    currentScore = 0,
                    livesLeft = initialLives,
                    maxTimePerQuestion = DEFAULT_TIME_PER_QUESTION,
                    timeLeft = DEFAULT_TIME_PER_QUESTION,
                    timeRanOut = false,
                    selectedAnswerIndex = null,
                    answerSubmitted = false,
                    isAnswerCorrect = null,
                    isGameOver = false
                )
            }
            startTimer()
        } else {
            Log.w("TriviaViewModel", "No se encontraron preguntas para la categoría '$selectedCategory'. Terminando juego.")
            _uiState.update { it.copy(isLoading = false, isGameOver = true, currentQuestion = null) }
        }
    }

    fun selectAnswer(index: Int) {
        if (!_uiState.value.answerSubmitted) { _uiState.update { it.copy(selectedAnswerIndex = index) } }
    }

    fun submitAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion
        val selectedIndex = state.selectedAnswerIndex
        if (state.answerSubmitted || selectedIndex == null || question == null) return

        timerJob?.cancel()
        Log.d("TriviaViewModel", "Temporizador cancelado por submitAnswer.")

        val isCorrect = selectedIndex == question.correctAnswerIndex
        var newLivesLeft = state.livesLeft
        val newScore = if (isCorrect) {
            currentCorrectStreak++
            Log.d("TriviaViewModel", "Respuesta correcta. Racha actual: $currentCorrectStreak")
            if (currentCorrectStreak >= 10) { // Comprobar racha para logro
                unlockAchievementAndLog(ACHIEVEMENT_TRIVIA_STREAK_10, "Racha de $currentCorrectStreak alcanzada")
            }
            state.currentScore + 1
        } else {
            currentCorrectStreak = 0 // Resetear racha
            Log.d("TriviaViewModel", "Respuesta incorrecta. Racha reseteada a 0.")
            newLivesLeft = (state.livesLeft - 1).coerceAtLeast(0)
            state.currentScore
        }
        val isNowGameOver = newLivesLeft <= 0

        _uiState.update {
            it.copy(
                answerSubmitted = true,
                isAnswerCorrect = isCorrect,
                currentScore = newScore,
                livesLeft = newLivesLeft,
                isGameOver = isNowGameOver,
                currentQuestion = if (isNowGameOver) null else it.currentQuestion,
                timeRanOut = false
            )
        }
        if (isNowGameOver) { // Si el juego termina por vidas aquí
            saveHighScore()
        }
    }

    fun loadNextQuestion() {
        if (_uiState.value.isGameOver) { return }

        timerJob?.cancel()
        Log.d("TriviaViewModel", "Temporizador cancelado por loadNextQuestion.")

        val currentIdx = _uiState.value.currentQuestionIndex
        if (currentIdx + 1 < currentRoundQuestions.size) {
            val nextIndex = currentIdx + 1
            _uiState.update {
                it.copy(
                    currentQuestion = currentRoundQuestions[nextIndex],
                    currentQuestionIndex = nextIndex,
                    selectedAnswerIndex = null,
                    answerSubmitted = false,
                    isAnswerCorrect = null,
                    timeLeft = DEFAULT_TIME_PER_QUESTION,
                    timeRanOut = false
                )
            }
            startTimer()
        } else {
            Log.d("TriviaViewModel", "Fin del juego (preguntas completadas).")
            saveHighScore()

            val finalScore = _uiState.value.currentScore
            val totalQuestionsInRound = currentRoundQuestions.size
            if (totalQuestionsInRound > 0 && finalScore == totalQuestionsInRound) {
                unlockAchievementAndLog(ACHIEVEMENT_TRIVIA_MASTER_GENERAL, "Puntuación perfecta ($finalScore/$totalQuestionsInRound)")
            }
            _uiState.update { it.copy(isGameOver = true, currentQuestion = null) }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        Log.d("TriviaViewModel", "Iniciando temporizador...")
        timerJob = viewModelScope.launch {
            var currentTime = _uiState.value.maxTimePerQuestion
            while (currentTime > 0 && isActive) {
                delay(1000)
                if (!isActive) break
                currentTime--
                _uiState.update { it.copy(timeLeft = currentTime) }
                Log.v("TriviaViewModel", "Tick: Tiempo restante $currentTime")
            }
            if (isActive && currentTime <= 0) {
                Log.d("TriviaViewModel", "¡Tiempo agotado!")
                handleTimeRanOut()
            } else {
                Log.d("TriviaViewModel", "Temporizador detenido/cancelado antes de agotarse.")
            }
        }
    }

    private fun handleTimeRanOut() {
        timerJob?.cancel()
        currentCorrectStreak = 0 // Resetear racha
        Log.d("TriviaViewModel", "Tiempo agotado. Racha reseteada a 0.")

        val currentLives = _uiState.value.livesLeft
        val newLivesLeft = (currentLives - 1).coerceAtLeast(0)
        val isNowGameOver = newLivesLeft <= 0
        Log.d("TriviaViewModel", "handleTimeRanOut: Vidas restantes: $newLivesLeft, GameOver: $isNowGameOver")

        if (isNowGameOver) {
            saveHighScore()
        }
        _uiState.update {
            it.copy(
                timeLeft = 0,
                timeRanOut = true,
                answerSubmitted = true,
                isAnswerCorrect = false,
                livesLeft = newLivesLeft,
                isGameOver = isNowGameOver,
                currentQuestion = if (isNowGameOver) null else it.currentQuestion
            )
        }
    }

    private fun saveHighScore() {
        val scoreOfThisRound = _uiState.value.currentScore // Esta es la puntuación de la ronda actual
        val category = selectedCategory // Categoría de esta ronda

        viewModelScope.launch {
            Log.d("TriviaViewModel", "Intentando guardar HighScore: Cat=$category, Score=$scoreOfThisRound")
            userProfileRepository.updateTriviaHighScore(category, scoreOfThisRound)
                .onSuccess {
                    Log.d("TriviaViewModel", "updateTriviaHighScore Success para categoría $category.")
                }.onFailure { error ->
                    Log.e("TriviaViewModel", "updateTriviaHighScore Failure para categoría $category.", error)
                }

            // --- AÑADIR PUNTUACIÓN AL TOTAL Y VERIFICAR LOGRO ---
            if (scoreOfThisRound > 0) { // Solo si se ganaron puntos en la ronda
                Log.d("TriviaViewModel", "Añadiendo $scoreOfThisRound al total de trivia y verificando logro.")
                userProfileRepository.addScoreToTotalTriviaScoreAndCheckAchievement(scoreOfThisRound).onFailure { error ->
                    Log.e("TriviaViewModel", "Error al añadir al total de trivia y verificar logro", error)
                }
            }
        }
    }

    // Helper para desbloquear logro y loggear
    private fun unlockAchievementAndLog(achievementId: String, reason: String) {
        viewModelScope.launch {
            Log.d("TriviaViewModel", "$reason. Intentando desbloquear logro: $achievementId")
            userProfileRepository.unlockAchievement(achievementId).onSuccess { newlyUnlocked ->
                if (newlyUnlocked) {
                    Log.i("TriviaViewModel", "Logro '$achievementId' DESBLOQUEADO!")
                    // Enviar evento para Snackbar
                    val achievementName = com.example.appescapetocinema.util.getAchievementDefinition(achievementId)?.name ?: achievementId
                    _achievementUnlockedEvent.send("¡Logro Desbloqueado: $achievementName!")
                } else {
                    Log.d("TriviaViewModel", "Logro '$achievementId' ya estaba desbloqueado o no se pudo verificar.")
                }
            }.onFailure { error ->
                Log.e("TriviaViewModel", "Error desbloqueando '$achievementId'", error)
            }
        }
    }

    fun resetGame() {
        Log.d("TriviaViewModel", "Reseteando juego...")
        currentCorrectStreak = 0 // Resetear racha
        timerJob?.cancel()
        prepareNewRound()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        Log.d("TriviaViewModel", "onCleared: Temporizador cancelado.")
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(TriviaViewModel::class.java)) {
                    val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                    val savedStateHandle = extras.createSavedStateHandle()
                    val userProfileRepository = UserProfileRepositoryImpl()

                    return TriviaViewModel(application, savedStateHandle, userProfileRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
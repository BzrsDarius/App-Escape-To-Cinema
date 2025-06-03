package com.example.appescapetocinema.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.appescapetocinema.repository.MovieRepository // Repo de películas
import com.example.appescapetocinema.repository.UserRepository // <-- NUEVO: Repo de usuario
import kotlinx.coroutines.flow.* // Importa collect, etc.
import kotlinx.coroutines.launch
import android.util.Log
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.appescapetocinema.model.NewsArticle
import com.example.appescapetocinema.model.Review
import com.example.appescapetocinema.repository.NewsRepository
import com.example.appescapetocinema.repository.ReviewRepository
import com.example.appescapetocinema.repository.UserProfileRepository
import com.example.appescapetocinema.ui.components.MovieItem
import com.example.appescapetocinema.util.ACHIEVEMENT_FIRST_REVIEW
import com.example.appescapetocinema.util.ACHIEVEMENT_GEM_COLLECTOR_25
import com.example.appescapetocinema.util.ACHIEVEMENT_REVIEW_AUTHOR_5
import com.example.appescapetocinema.util.ACHIEVEMENT_REVIEW_PRO_15
import com.example.appescapetocinema.util.getAchievementName
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async // Para llamadas paralelas
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.Channel // Para eventos de una sola vez
import kotlinx.coroutines.flow.receiveAsFlow // Para convertir Channel a Flow

// Importa el estado si está en otro archivo
// import com.example.appescapetocinema.ui.detail.DetailUiState

const val MOVIE_ID_ARG = "movieId"
private const val JOHN_CARPENTER_PERSON_ID = 11770  // ID de persona de John Carpenter en TMDb

class DetailViewModel(
    private val movieRepository: MovieRepository,
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val auth: FirebaseAuth,
    private val savedStateHandle: SavedStateHandle,
    private val userProfileRepository: UserProfileRepository,
    // --- 1. AÑADIR NewsRepository A LAS DEPENDENCIAS DEL CONSTRUCTOR ---
    private val newsRepository: NewsRepository
) : ViewModel() {

    // _uiState ya lo tienes. Asegúrate que DetailUiState tiene los campos para noticias.
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val movieId: Int? = savedStateHandle[MOVIE_ID_ARG]

    private val _achievementUnlockedEvent = Channel<String>(Channel.BUFFERED)
    val achievementUnlockedEvent: Flow<String> = _achievementUnlockedEvent.receiveAsFlow()

    init {
        Log.d("DetailViewModel", "Iniciando ViewModel para movieId: $movieId")
        _uiState.update { it.copy(currentUserId = auth.currentUser?.uid) }
        movieId?.let { id ->
            // --- 2. CAMBIAR NOMBRE DE FUNCIÓN DE CARGA PRINCIPAL (Opcional, pero más claro) ---
            loadInitialMovieData(id) // Carga detalles, cast, y luego noticias
            observeMyListStatus(id)
            observeUserRating(id)
            observeReviews(id)
            checkExistingUserReview(id)
        } ?: run {
            Log.e("DetailViewModel", "Error: MovieId es nulo al iniciar ViewModel.")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMovieNews = false, // Añadir si no estaba
                    errorMessage = "Error: ID de película inválido."
                )
            }
        }
    }

    // --- Carga TODO en paralelo ---
    private fun loadInitialMovieData(id: Int) {
        Log.d("DetailViewModel", "loadInitialMovieData: Iniciando carga completa para ID: $id")
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = true,              // Carga principal de la película
                isLoadingReviews = true,       // Inicia carga de reviews (observador se encargará)
                isLoadingMovieNews = true,     // Inicia carga de noticias
                isLoadingWatchProviders = true,// Inicia carga de dónde ver
                errorMessage = null,           // Limpiar errores generales
                reviewSubmissionError = null,  // Limpiar errores de reviews
                movieNewsError = null,         // Limpiar errores de noticias
                watchProvidersError = null     // Limpiar errores de dónde ver
            )
        }

        viewModelScope.launch {
            try {
                // Lanzar todas las llamadas de red en paralelo
                val detailsDeferred = async { movieRepository.getMovieDetails(id) }
                val creditsDeferred = async { movieRepository.getMovieCredits(id) }
                val watchProvidersDeferred = async { movieRepository.getWatchProviders(id) }

                // Los observadores de Firebase para MiLista, Rating, Reviews, ExistingReview
                // ya se inician en el bloque `init` y se ejecutarán concurrentemente.
                // No es necesario volver a llamarlos aquí a menos que quieras forzar una re-lectura
                // que no sea por el listener.

                // Esperar y obtener los resultados o null si fallan
                val movieDetailsResult = detailsDeferred.await() // Guardamos el Result para un mejor log de error
                val creditsResult = creditsDeferred.await()       // Guardamos el Result
                val watchProvidersResultValue = watchProvidersDeferred.await() // Es Result<WatchProvidersResponseDto>

                // Procesar resultado de Watch Providers primero, ya que no depende de movieDetails
                watchProvidersResultValue.fold(
                    onSuccess = { responseDto ->
                        Log.d("DetailViewModel", "Watch providers obtenidos: ${responseDto.results.size} países.")
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoadingWatchProviders = false,
                                watchProvidersByCountry = responseDto.results,
                                watchProvidersError = null
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e("DetailViewModel", "Error cargando watch providers", error)
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoadingWatchProviders = false,
                                watchProvidersByCountry = null, // O emptyMap() si prefieres
                                watchProvidersError = error.localizedMessage ?: "Error al cargar dónde ver"
                            )
                        }
                    }
                )

                // Procesar detalles y créditos
                val movieDetails = movieDetailsResult.getOrNull()
                val credits = creditsResult.getOrNull()

                val director = credits?.crew?.find { it.job == "Director" }
                val cast = credits?.cast?.sortedBy { it.order }?.take(10) ?: emptyList()

                if (movieDetails != null) {
                    // Éxito al obtener detalles de la película
                    if (credits == null) {
                        Log.w("DetailViewModel", "Fallo al obtener créditos para película ID: $id. Causa: ${creditsResult.exceptionOrNull()?.localizedMessage}")
                    }

                    _uiState.update { currentState ->
                        currentState.copy(
                            movieDetails = movieDetails,
                            cast = cast,
                            director = director
                            // isLoading principal se pondrá a false al final
                        )
                    }

                    // Cargar noticias relacionadas ahora que tenemos el título y otros detalles
                    // Asumiendo que MovieDetails.title es non-null. Si es nullable: if (movieDetails.title != null)
                    loadRelatedNewsFromRepository(
                        movieTitle = movieDetails.title, // Asume que title no es nulo aquí
                        directorName = director?.name,
                        actorName = cast.firstOrNull()?.name
                    )

                } else {
                    // Fallo crítico al obtener detalles de la película
                    val errorCause = movieDetailsResult.exceptionOrNull()?.localizedMessage ?: "Causa desconocida"
                    Log.e("DetailViewModel", "Fallo crítico al obtener detalles para película ID: $id. Causa: $errorCause")
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,           // Terminar carga general
                            isLoadingReviews = false,    // Si las reviews dependen de esto, apagarlas
                            isLoadingMovieNews = false,  // Terminar carga de noticias
                            isLoadingWatchProviders = false, // Ya debería estar false, pero por si acaso
                            errorMessage = "Error cargando detalles: $errorCause"
                        )
                    }
                    return@launch // No continuar si los detalles de la película fallan
                }

                // Marcar la carga principal (detalles de película) como finalizada
                // Los otros isLoading (reviews, news, watchproviders) se manejan
                // en sus respectivas funciones de carga o callbacks de observadores.
                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) { // Captura excepciones generales del bloque launch
                Log.e("DetailViewModel", "Excepción inesperada en loadInitialMovieData para ID $id", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isLoadingReviews = false,
                        isLoadingMovieNews = false,
                        isLoadingWatchProviders = false,
                        errorMessage = "Error inesperado: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
    // En DetailViewModel.kt

    private fun loadRelatedNewsFromRepository(movieTitle: String, directorName: String?, actorName: String?) {
        Log.d("DetailViewModel", "Cargando noticias para: '$movieTitle', Director: '$directorName', Actor: '$actorName'")
        // isLoadingMovieNews ya se debería haber puesto a true

        viewModelScope.launch {
            try {
                // --- LLAMADA DIRECTA A LA FUNCIÓN SUSPENDIDA ---
                val result: Result<List<NewsArticle>> = newsRepository.getMovieRelatedNews(
                    movieTitle = movieTitle,
                    directorName = directorName,
                    actorName = actorName
                )

                // Manejar el resultado directamente
                result.fold(
                    onSuccess = { articles: List<NewsArticle> ->
                        Log.d("DetailViewModel", "Noticias relacionadas obtenidas: ${articles.size} artículos.")
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoadingMovieNews = false,
                                movieNewsArticles = articles,
                                movieNewsError = null
                            )
                        }
                    },
                    onFailure = { error: Throwable ->
                        Log.e("DetailViewModel", "Fallo al cargar noticias relacionadas (Result.Failure): ${error.localizedMessage}", error)
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoadingMovieNews = false,
                                movieNewsArticles = emptyList(),
                                movieNewsError = error.localizedMessage ?: "Error al cargar noticias"
                            )
                        }
                    }
                )
            } catch (e: Exception) { // Captura excepciones de la corrutina o de la llamada si el repo no las maneja y las relanza
                Log.e("DetailViewModel", "Excepción en corrutina de carga de noticias: ${e.localizedMessage}", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoadingMovieNews = false,
                        movieNewsArticles = emptyList(),
                        movieNewsError = e.localizedMessage ?: "Error inesperado obteniendo noticias"
                    )
                }
            }
        }
    }

    // --- Asegúrate de que la propiedad del Flow existe ---
    val similarMoviesFlow: Flow<PagingData<MovieItem>>? = movieId?.let { id ->
        movieRepository.getSimilarMoviesStream(id).cachedIn(viewModelScope)
    }

    // --- observeMyListStatus y toggleMyListStatus (sin cambios) ---
    private fun observeMyListStatus(id: Int) {
        Log.d("DetailViewModel", "[Observe Start] Iniciando observación de 'Mi Lista' para ID: $id")
        viewModelScope.launch {
            userRepository.isMovieInMyListFlow(id)
                .catch { error ->
                    // --- Log en CATCH ---
                    Log.e(
                        "DetailViewModel",
                        "[Observe Error] Error en Flow isMovieInMyListFlow para ID: $id",
                        error
                    )
                    _uiState.update { it.copy(errorMessage = "No se pudo verificar 'Mi Lista': ${error.localizedMessage}") }
                }
                .collect { isInList ->
                    // --- Log en COLLECT ---
                    Log.d(
                        "DetailViewModel",
                        "[Observe Collect] Recibido estado de 'Mi Lista' para ID $id: $isInList"
                    )
                    _uiState.update { it.copy(isMovieInMyList = isInList) }
                }
        }
    } // --- Fin observeMyListStatus ---

    private fun observeUserRating(id: Int) {
        Log.d("DetailViewModel", "[Observe Rating Start] ID: $id")
        viewModelScope.launch {
            userRepository.getUserRatingForMovieFlow(id)
                .catch { error ->
                    Log.e(
                        "DetailViewModel",
                        "[Observe Rating Error] ID: $id",
                        error
                    )
                }
                .collect { rating ->
                    Log.d("DetailViewModel", "[Observe Rating Collect] ID $id: $rating")
                    _uiState.update { it.copy(userRating = rating) }
                }
        }
    }

    fun toggleMyListStatus() {
        // --- LOG AÑADIDO AQUÍ ---
        Log.d("DetailViewModel", "toggleMyListStatus() INVOCADO.")
        // --- FIN LOG AÑADIDO ---

        val currentUiState = _uiState.value
        val currentMovieId = this.movieId ?: run {
            Log.e("DetailViewModel", "toggleMyListStatus llamado sin movieId válido.")
            return // No hacer nada si no hay ID
        }
        val isInList = currentUiState.isMovieInMyList

        if (currentUiState.isUpdatingMyList) {
            Log.w("DetailViewModel", "toggleMyListStatus ignorado: ya está actualizando.")
            return // Evita múltiples clics
        }
        val directorId = currentUiState.director?.id // Asumiendo que director está en UiState

        _uiState.update {
            it.copy(
                isUpdatingMyList = true,
                errorMessage = null,
            )
        } // Limpia errores previos también

        viewModelScope.launch {
            val result = if (isInList) {
                Log.d(
                    "DetailViewModel",
                    "Llamando a removeMovieFromMyList para ID: $currentMovieId"
                )
                userRepository.removeMovieFromMyList(currentMovieId)
            } else {
                Log.d("DetailViewModel", "Llamando a addMovieToMyList para ID: $currentMovieId")
                userRepository.addMovieToMyList(currentMovieId)
            }

            // --- Añadir logs para el resultado ---
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isUpdatingMyList = false) }
                    if (!isInList) { // Si se añadió
                        checkAndUnlockAchievement(ACHIEVEMENT_FIRST_ADD_MYLIST)
                        checkAndUnlockMyListCountAchievement()
                    }
                    // --- LÓGICA PARA LOGRO SECRETO DE CARPENTER ---
                    val directorId = currentUiState.director?.id
                    Log.d("DetailViewModel_Carpenter", "toggleMyListStatus: currentMovieId=$currentMovieId, Director ID en UIState: $directorId, Constante Carpenter ID: $JOHN_CARPENTER_PERSON_ID")

                    if (directorId == JOHN_CARPENTER_PERSON_ID) {
                        Log.d("DetailViewModel_Carpenter", "¡Es una película de John Carpenter!") // <--- NUEVO LOG
                        if (!isInList) { // Se añadió una película de Carpenter
                            Log.d("DetailViewModel", "Película de Carpenter (ID: $directorId) añadida. Actualizando perfil.")
                            userProfileRepository.addCarpenterMovieToProfile(currentMovieId).onFailure { e ->
                                Log.e("DetailViewModel", "Error addCarpenterMovieToProfile", e)
                            }
                        } else { // Se quitó una película de Carpenter
                            Log.d("DetailViewModel", "Película de Carpenter (ID: $directorId) quitada. Actualizando perfil.")
                            userProfileRepository.removeCarpenterMovieFromProfile(currentMovieId).onFailure { e ->
                                Log.e("DetailViewModel", "Error removeCarpenterMovieFromProfile", e)
                            }
                        }
                    }
                },
                onFailure = { error ->
                    Log.e("DetailViewModel", "Operación MyList para $currentMovieId FALLÓ", error)
                    _uiState.update {
                        it.copy(
                            isUpdatingMyList = false,
                            errorMessage = "Error al actualizar 'Mi Lista': ${error.localizedMessage}"
                        )
                    }
                }
            )
            // --- Fin logs de resultado ---
        }
    }

    // --- NUEVO: Enviar/Guardar la valoración ---
    fun submitRating(rating: Double) { // Recibe Double
        val currentMovieId = this.movieId ?: return // Usa this.movieId
        if (rating < 0.0 || rating > 10.0) { /* ... validación ... */ return
        }
        if (_uiState.value.isUpdatingRating) return

        Log.d("DetailViewModel", "submitRating ID: $currentMovieId, Rating: $rating")
        _uiState.update { it.copy(isUpdatingRating = true, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.rateMovie(currentMovieId, rating)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isUpdatingRating = false) }
                    checkAndUnlockAchievement(ACHIEVEMENT_FIRST_RATING)
                    checkAndUnlockRatedCountAchievement()
                },
                onFailure = { error ->
                    Log.e(
                        "DetailViewModel",
                        "Error guardando rating $rating para ID: $currentMovieId",
                        error
                    )
                    _uiState.update {
                        it.copy(
                            isUpdatingRating = false,
                            errorMessage = "Error al guardar: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    // --- Reintentar ahora carga TODO ---
    fun retryLoad() {
        movieId?.let { loadInitialMovieData(it) } // Llama a la función que carga todo
            ?: _uiState.update { it.copy(errorMessage = "Error: No se puede reintentar sin un ID.") }
    }

    // --- NUEVAS FUNCIONES PARA RESEÑAS ---

    private fun observeReviews(id: Int) {
        Log.d("DetailViewModel", "[Observe Reviews Start] ID: $id")
        _uiState.update {
            it.copy(
                isLoadingReviews = true,
                reviewSubmissionError = null
            )
        } // Indicar carga
        viewModelScope.launch {
            reviewRepository.getReviewsForMovieFlow(id)
                .catch { error ->
                    Log.e("DetailViewModel", "[Observe Reviews Error] ID: $id", error)
                    _uiState.update {
                        it.copy(
                            isLoadingReviews = false,
                            reviewSubmissionError = "Error cargando reseñas."
                        )
                    }
                }
                .collect { reviewList ->
                    Log.d(
                        "DetailViewModel",
                        "[Observe Reviews Collect] ID $id: Recibidas ${reviewList.size} reseñas"
                    )
                    // Actualiza lista y quita estado de carga/error específico de reseñas
                    _uiState.update {
                        it.copy(
                            isLoadingReviews = false,
                            reviews = reviewList,
                            reviewSubmissionError = null
                        )
                    }
                }
        }
    }

    private fun checkExistingUserReview(id: Int) {
        val userId = auth.currentUser?.uid ?: return
        Log.d("DetailViewModel", "checkExistingUserReview: User $userId, Movie $id")
        viewModelScope.launch {
            val result = reviewRepository.getUserReviewForMovie(id, userId)
            result.fold(
                onSuccess = { existingReview ->
                    val hasReview = existingReview != null
                    Log.d(
                        "DetailViewModel",
                        "checkExistingUserReview: User $userId tiene reseña? $hasReview"
                    )
                    // Actualiza el flag y potencialmente pre-rellena el texto
                    _uiState.update { it.copy(userHasExistingReview = hasReview) }
                },
                onFailure = { error ->
                    Log.e("DetailViewModel", "checkExistingUserReview: Error", error)
                    _uiState.update { it.copy(userHasExistingReview = false) } // Asume que no tiene si hay error
                }
            )
        }
    }

    fun onReviewTextChange(newText: String) {
        _uiState.update {
            val canSubmit = newText.isNotBlank() && (it.userRating ?: 0.0) > 0.0
            it.copy(
                userReviewInputText = newText,
                canSubmitReview = canSubmit,
                reviewSubmissionError = null // Limpiar error al escribir
            )
        }
    }

    fun submitReview() {
        val currentState = _uiState.value
        val currentMovieId = this.movieId ?: return
        val currentUser = auth.currentUser ?: run {
            _uiState.update { it.copy(reviewSubmissionError = "Debes iniciar sesión.") }; return
        }
        val userId = currentUser.uid
        val userName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: currentUser.email
        ?: "Usuario Anónimo"
        val userRating = currentState.userRating ?: run {
            _uiState.update { it.copy(reviewSubmissionError = "Debes valorar la película primero.") }; return
        }
        val reviewText = currentState.userReviewInputText.trim()

        if (!currentState.canSubmitReview || reviewText.isBlank()) {
            _uiState.update { it.copy(reviewSubmissionError = "Escribe tu reseña (y valora la película).") }; return
        }
        if (currentState.isSubmittingReview) return

        Log.d("DetailViewModel", "submitReview: Intentando enviar...")
        _uiState.update { it.copy(isSubmittingReview = true, reviewSubmissionError = null) }

        viewModelScope.launch {
            val result = reviewRepository.submitReview(
                currentMovieId,
                userId,
                userName,
                userRating,
                reviewText
            )
            result.fold(
                onSuccess = {
                    Log.d("DetailViewModel", "submitReview: Éxito.")
                    _uiState.update {
                        it.copy(
                            isSubmittingReview = false,
                            userHasExistingReview = true,
                            canSubmitReview = currentState.userReviewInputText.isNotBlank() // Recalcular por si solo cambió rating
                        )
                    }
                    checkAndUnlockAchievement(ACHIEVEMENT_FIRST_REVIEW) // Usa el import completo o define la constante localmente
                    Log.d("DetailViewModel", "Llamando a incrementar contador de reseñas y verificar logros.")
                    userProfileRepository.incrementReviewCountAndCheckAchievements().onFailure { error ->
                        Log.e("DetailViewModel", "Error al incrementar contador de reseñas y verificar logros", error)
                    }
                },

                onFailure = { error ->
                    Log.e("DetailViewModel", "submitReview: Error", error)
                    _uiState.update {
                        it.copy(
                            isSubmittingReview = false,
                            reviewSubmissionError = "Error al enviar: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    private fun checkReviewCountAchievements() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Esto asume que tus reseñas están en una colección raíz o subcolección fácil de consultar por userId

                reviewRepository.getUserReviewCount(userId).onSuccess { count -> // Asumiendo que creas esta función
                    Log.d("DetailViewModel", "Total reseñas del usuario $userId: $count")
                    if (count >= 5) {
                        checkAndUnlockAchievement(ACHIEVEMENT_REVIEW_AUTHOR_5)
                    }
                    if (count >= 15) {
                        checkAndUnlockAchievement(ACHIEVEMENT_REVIEW_PRO_15)
                    }
                }.onFailure { error ->
                    Log.e("DetailViewModel", "Error obteniendo conteo de reseñas", error)
                }
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Excepción al contar reseñas", e)
            }
        }
    }

    private fun checkAndUnlockAchievement(achievementId: String) {
        viewModelScope.launch {
            Log.d("DetailViewModel", "Checkeando logro: $achievementId")
            userProfileRepository.unlockAchievement(achievementId).onSuccess { newlyUnlocked ->
                if (newlyUnlocked) {
                    Log.i("DetailViewModel", "¡LOGRO DESBLOQUEADO!: $achievementId")
                    val achievementName = getAchievementName(achievementId) // Usa tu helper
                    _achievementUnlockedEvent.send("¡Logro Desbloqueado: $achievementName!")
                } else {
                    Log.d("DetailViewModel", "Logro '$achievementId' ya estaba desbloqueado.")
                }
            }.onFailure { error ->
                Log.e("DetailViewModel", "Error al intentar desbloquear logro '$achievementId'", error)
            }
        }
    }

    private fun checkAndUnlockMyListCountAchievement() {
        viewModelScope.launch {
            userRepository.getMyListSize().onSuccess { size ->
                if (size >= 5) {
                    checkAndUnlockAchievement(ACHIEVEMENT_MYLIST_5) // Usa la constante directamente
                }
                if (size >= 25) {
                    checkAndUnlockAchievement(ACHIEVEMENT_GEM_COLLECTOR_25)
                }
            }.onFailure { Log.e("DetailViewModel", "Error getMyListSize", it) }
        }
    }

    private fun checkAndUnlockRatedCountAchievement() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val docRef =
                    FirebaseFirestore.getInstance().collection("user_ratings").document(userId)
                val snapshot = docRef.get().await()
                val ratedCount = (snapshot.data?.get("ratings") as? Map<*, *>)?.size ?: 0
                if (ratedCount >= 5) checkAndUnlockAchievement(ACHIEVEMENT_RATED_5)
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error contando ratings", e)
            }
        }
    }

    fun startEditingReview(review: Review) {
        // Verifica que la reseña pertenezca al usuario actual por seguridad extra
        if (review.userId != auth.currentUser?.uid || this.movieId != review.movieId.toIntOrNull()) {
            Log.w("DetailViewModel", "Intento de editar reseña no propia o de otra película.")
            _uiState.update { it.copy(reviewSubmissionError = "No se puede editar esta reseña.") }
            return
        }
        Log.d("DetailViewModel", "startEditingReview: Poniendo texto '${review.text}' en el input.")
        // Actualiza el campo de texto con el contenido de la reseña y habilita el botón
        _uiState.update {
            val canSubmit = review.text.isNotBlank() && (it.userRating ?: 0.0) > 0.0
            it.copy(
                userReviewInputText = review.text,
                canSubmitReview = canSubmit, // Habilitar botón (si tiene rating)
                reviewSubmissionError = null // Limpiar errores previos
            )
        }
    }


    fun deleteCurrentUserReview(review: Review) { // Recibe la Review por si acaso, aunque usamos datos internos
        val currentMovieId = movieId ?: run { Log.e("DetailVM", "Delete sin movieId"); return }
        val userId = auth.currentUser?.uid ?: run { Log.e("DetailVM", "Delete sin userId"); return }

        // Doble check por seguridad
        if(review.userId != userId || review.movieId != currentMovieId.toString()){
            Log.e("DetailViewModel", "Intento de borrar reseña incorrecta.")
            _uiState.update { it.copy(reviewSubmissionError = "Error al intentar borrar la reseña.")}
            return
        }

        Log.d("DetailViewModel", "deleteCurrentUserReview: Intentando borrar reseña para Movie $currentMovieId, User $userId")
        // No marcamos isSubmittingReview aquí, es una acción diferente
        _uiState.update { it.copy(reviewSubmissionError = null) } // Limpiar error previo

        viewModelScope.launch {
            val result = reviewRepository.deleteReview(currentMovieId, userId)
            result.fold(
                onSuccess = {
                    Log.d("DetailViewModel", "deleteCurrentUserReview: Éxito.")
                    // Limpia el campo de texto, resetea flags
                    _uiState.update {
                        it.copy(
                            userReviewInputText = "",
                            userHasExistingReview = false,
                            canSubmitReview = false,
                            reviewSubmissionError = null
                        )
                    }
                    // La lista de reseñas se actualizará por el Flow `observeReviews`
                },
                onFailure = { error ->
                    Log.e("DetailViewModel", "deleteCurrentUserReview: Error", error)
                    _uiState.update {
                        it.copy(reviewSubmissionError = "Error al borrar la reseña: ${error.localizedMessage}")
                    }
                }
            )
        }
    }
            // --- Factory DENTRO del Companion Object ---
            companion object {
                private const val ACHIEVEMENT_FIRST_ADD_MYLIST = "FIRST_ADD_MYLIST"
                private const val ACHIEVEMENT_FIRST_RATING = "FIRST_RATING"
                private const val ACHIEVEMENT_MYLIST_5 = "MYLIST_5"
                private const val ACHIEVEMENT_RATED_5 = "RATED_5"
                // La función se llama 'Factory' con F mayúscula
                // Acepta todas las dependencias que el ViewModel necesita y que no se inyectan solas
                fun Factory(
                    movieRepository: MovieRepository,
                    userRepository: UserRepository,
                    reviewRepository: ReviewRepository,
                    userProfileRepository: UserProfileRepository,
                    auth: FirebaseAuth,
                    newsRepository: NewsRepository
                ): ViewModelProvider.Factory =
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                            if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
                                val savedStateHandle = extras.createSavedStateHandle()
                                return DetailViewModel(
                                    movieRepository,
                                    userRepository,
                                    reviewRepository,
                                    auth,
                                    savedStateHandle,
                                    userProfileRepository,
                                    newsRepository
                                ) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class (${modelClass.name}) for DetailViewModel.Factory")
                        }
                    }
            }
}
package com.example.appescapetocinema.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase
import com.example.appescapetocinema.data.SharedPreferencesHelper
import com.example.appescapetocinema.repository.MovieRepository
import com.example.appescapetocinema.repository.MyListPagingSource
import com.example.appescapetocinema.repository.UserProfileRepository
import com.example.appescapetocinema.repository.UserRepository
import com.example.appescapetocinema.ui.detail.MovieDetails
import com.example.appescapetocinema.util.AchievementDisplayInfo
import com.example.appescapetocinema.util.allAchievementDefinitions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine // Para envolver callbacks


class ProfileViewModel(
    application: Application,
    private val movieRepository: MovieRepository,
    private val userRepository: UserRepository,
    private val authRepository: RepositorioAutenticacionFirebase,
    private val userProfileRepository: UserProfileRepository
) : AndroidViewModel(application) {

    private val sharedPreferencesHelper = SharedPreferencesHelper(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _myListMovieIds = MutableStateFlow<List<Int>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val myListMoviesFlow: Flow<PagingData<MovieDetails>> = _myListMovieIds
        .filterNotNull()
        .flatMapLatest { ids ->
            Log.d("ProfileViewModel", "myListMovieIds actualizado, creando Pager para Mi Lista con ${ids.size} IDs.")
            if (ids.isEmpty()) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = MyListPagingSource.MY_LIST_PAGE_SIZE,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        MyListPagingSource(movieIds = ids, movieRepository = movieRepository)
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    private var authStateJob: Job? = null
    private var achievementsJob: Job? = null
    private var highScoresJob: Job? = null

    init {
        Log.d("ProfileViewModel", "INIT ViewModel")
        observeAuthState() // Iniciar la observación del estado de autenticación
        loadWatchProviderCountryPreference() // Cargar preferencia de país (no depende del usuario)
    }

    private fun observeAuthState() {
        authStateJob?.cancel()
        authStateJob = authRepository.getAuthStateFlow()
            .onEach { firebaseUser: FirebaseUser? ->
                if (firebaseUser != null) {
                    Log.d("ProfileViewModel", "AuthState: Usuario LOGUEADO (${firebaseUser.email}).")
                    _uiState.update { it.copy(isLoading = true, userEmail = firebaseUser.email) } // Poner isLoading y email
                    loadInitialUserData() // Cargar MiLista y luego iniciar listeners
                } else {
                    Log.d("ProfileViewModel", "AuthState: Usuario DESLOGUEADO. Deteniendo observers y limpiando UI.")
                    stopObservingUserData()
                    val countryPref = _uiState.value.watchProviderCountryPreference // Mantener preferencia de país
                    _uiState.update { ProfileUiState(watchProviderCountryPreference = countryPref) }
                    _myListMovieIds.value = null
                }
            }
            .catch { e -> Log.e("ProfileViewModel", "Error en authStateFlow", e) }
            .launchIn(viewModelScope)
    }

    private fun loadInitialUserData() {
        Log.d("ProfileViewModel", "loadInitialUserData: Cargando IDs de Mi Lista...")
        viewModelScope.launch {
            val idListResult = userRepository.getMyListMovieIds()
            idListResult.fold(
                onSuccess = { movieIds ->
                    Log.d("ProfileViewModel", "loadInitialUserData: IDs de Mi Lista obtenidos: ${movieIds.size}.")
                    _myListMovieIds.value = movieIds
                },
                onFailure = { error ->
                    Log.e("ProfileViewModel", "loadInitialUserData: Error cargando IDs de Mi Lista", error)
                    _myListMovieIds.value = emptyList()
                    _uiState.update { it.copy(errorMessage = (it.errorMessage ?: "") + "\nError Mi Lista: ${error.localizedMessage}") }
                }
            )
            // Una vez cargados los IDs (o fallado), iniciar los listeners de logros y puntuaciones
            // Estos listeners también pondrán isLoading a false cuando reciban datos.
            startObservingUserData()
        }
    }


    private fun startObservingUserData() {
        Log.d("ProfileViewModel", "startObservingUserData: Iniciando listeners de logros y puntuaciones.")
        if (!_uiState.value.isLoading && authRepository.obtenerUsuarioActual() != null) {
            _uiState.update { it.copy(isLoading = true) }
        }

        achievementsJob?.cancel()
        achievementsJob = userProfileRepository.getUnlockedAchievementDataFlow()
            .catch { error ->
                Log.e("ProfileViewModel", "[Observe Achievements] Error en Flow", error)
                _uiState.update { it.copy(errorMessage = (it.errorMessage ?: "") + "\nError al cargar logros.", isLoading = false) }
            }
            .onEach { unlockedAchievementsData: Map<String, Timestamp> ->
                Log.d("ProfileViewModel", "[Observe Achievements] Datos recibidos: ${unlockedAchievementsData.size} logros.")
                val displayableAchievements = allAchievementDefinitions
                    .filter { definition -> !definition.secret || unlockedAchievementsData.containsKey(definition.id) }
                    .map { definition ->
                        val unlockedTimestamp = unlockedAchievementsData[definition.id]
                        AchievementDisplayInfo(definition = definition, isUnlocked = unlockedTimestamp != null, unlockedDate = unlockedTimestamp)
                    }.sortedWith(compareByDescending<AchievementDisplayInfo> { it.isUnlocked }.thenBy { it.definition.name })
                _uiState.update {
                    it.copy(
                        achievementsForDisplay = displayableAchievements,
                        unlockedAchievements = unlockedAchievementsData.keys, // Mantener si se usa
                        isLoading = false // Poner isLoading a false aquí, después de cargar logros
                    )
                }
            }
            .launchIn(viewModelScope)

        highScoresJob?.cancel()
        highScoresJob = userProfileRepository.getTriviaHighScoresFlow()
            .catch { error ->
                Log.e("ProfileViewModel", "[Observe HighScores] Error en Flow", error)
                _uiState.update { it.copy(errorMessage = (it.errorMessage ?: "") + "\nError al cargar puntuaciones.") }
            }
            .onEach { highScoresMap: Map<String, Long> ->
                Log.d("ProfileViewModel", "[Observe HighScores] Recibidas ${highScoresMap.size} puntuaciones.")
                _uiState.update { it.copy(triviaHighScores = highScoresMap) }
            }
            .launchIn(viewModelScope)
    }

    private fun stopObservingUserData() {
        Log.d("ProfileViewModel", "stopObservingUserData: Cancelando listeners.")
        achievementsJob?.cancel()
        achievementsJob = null
        highScoresJob?.cancel()
        highScoresJob = null
    }

    // Carga/Actualización de preferencias de país (no depende del login)
    private fun loadWatchProviderCountryPreference() {
        val currentCountry = sharedPreferencesHelper.getWatchProviderCountry()
        _uiState.update { it.copy(watchProviderCountryPreference = currentCountry) }
    }

    fun updateWatchProviderCountryPreference(newCountryCode: String) {
        sharedPreferencesHelper.setWatchProviderCountry(newCountryCode)
        _uiState.update { it.copy(watchProviderCountryPreference = newCountryCode, showCountrySelectionDialog = false) }
        Log.i("ProfileViewModel", "Preferencia de país para Watch Providers actualizada a: $newCountryCode")
    }

    fun openCountrySelectionDialog() {
        _uiState.update { it.copy(showCountrySelectionDialog = true) }
        if (_uiState.value.availableWatchProviderCountries.isEmpty() && !_uiState.value.isLoadingCountries) {
            loadAvailableWatchProviderRegions()
        }
    }

    fun closeCountrySelectionDialog() {
        _uiState.update { it.copy(showCountrySelectionDialog = false) }
    }

    private fun loadAvailableWatchProviderRegions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCountries = true, countriesLoadingError = null) }
            val result = movieRepository.getWatchProviderRegions()
            result.fold(
                onSuccess = { regionsList ->
                    val countries = regionsList.map { it.iso3166_1 to (it.nativeName ?: it.englishName) }.sortedBy { it.second }
                    _uiState.update { it.copy(availableWatchProviderCountries = countries, isLoadingCountries = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoadingCountries = false, countriesLoadingError = error.localizedMessage ?: "Error al cargar países") }
                }
            )
        }
    }

    // Función de Refresh manual
    fun refresh() {
        Log.d("ProfileViewModel", "Refresh solicitado.")
        if (authRepository.obtenerUsuarioActual() != null) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) } // Indicar carga
            // Recargar Mi Lista. Los listeners de logros/puntuaciones se actualizarán solos si hay cambios.
            loadInitialUserData()
        } else {
            Log.w("ProfileViewModel", "Refresh solicitado pero usuario no logueado.")
        }
    }

    // Borrar Mi Lista
    fun openClearMyListDialog() {
        _uiState.update { it.copy(showClearMyListDialog = true, clearMyListMessage = null) }
    }

    fun closeClearMyListDialog() {
        _uiState.update { it.copy(showClearMyListDialog = false) }
    }

    fun confirmClearMyList() {
        _uiState.update { it.copy(showClearMyListDialog = false) }
        viewModelScope.launch {
            val result = userRepository.clearMyList()
            result.fold(
                onSuccess = {
                    Log.i("ProfileViewModel", "Mi Lista borrada exitosamente.")
                    _uiState.update { it.copy(clearMyListMessage = "Tu Lista ha sido borrada.") }
                    _myListMovieIds.value = emptyList() // Actualizar Pager a vacío
                },
                onFailure = { error ->
                    Log.e("ProfileViewModel", "Error al borrar Mi Lista", error)
                    _uiState.update { it.copy(clearMyListMessage = "Error al borrar Mi Lista: ${error.localizedMessage}") }
                }
            )
        }
    }

    fun clearClearMyListMessage() {
        _uiState.update { it.copy(clearMyListMessage = null) }
    }

    // Eliminar Cuenta
    fun openDeleteAccountConfirmationDialog() {
        _uiState.update { it.copy(showDeleteAccountConfirmationDialog = true, deleteAccountError = null, deleteAccountSuccessMessage = null) }
    }

    fun closeDeleteAccountConfirmationDialog() {
        _uiState.update { it.copy(showDeleteAccountConfirmationDialog = false) }
    }

    fun onDeleteAccountPasswordInputChange(password: String) {
        _uiState.update { it.copy(deleteAccountPasswordInput = password, deleteAccountError = null) }
    }

    fun proceedToReAuthForDelete() {
        _uiState.update {
            it.copy(showDeleteAccountConfirmationDialog = false, showDeleteAccountReAuthDialog = true, deleteAccountPasswordInput = "", deleteAccountError = null)
        }
    }

    fun closeReAuthDialogForDelete() {
        _uiState.update { it.copy(showDeleteAccountReAuthDialog = false, deleteAccountPasswordInput = "", deleteAccountError = null) }
    }

    fun confirmAccountDeletionWithPassword() {
        val password = _uiState.value.deleteAccountPasswordInput
        if (password.isBlank()) {
            _uiState.update { it.copy(deleteAccountError = "La contraseña no puede estar vacía.") }
            return
        }
        _uiState.update { it.copy(isDeletingAccount = true, deleteAccountError = null, showDeleteAccountReAuthDialog = false) }

        viewModelScope.launch {
            val reauthResult = suspendReauthenticate(password)

            reauthResult.fold(
                onSuccess = {
                    Log.d("ProfileViewModel", "Reautenticación exitosa. Procediendo a eliminar cuenta y datos.")
                    val deleteResult = authRepository.deleteCurrentUserAccountAndData()
                    deleteResult.fold(
                        onSuccess = {
                            Log.i("ProfileViewModel", "Cuenta y datos eliminados exitosamente.")
                            _uiState.update { it.copy(isDeletingAccount = false, deleteAccountSuccessMessage = "Tu cuenta ha sido eliminada permanentemente.") }
                            authRepository.cerrarSesion() // Esto disparará el authStateFlow para limpiar
                        },
                        onFailure = { error ->
                            Log.e("ProfileViewModel", "Error al eliminar cuenta y datos después de reautenticar", error)
                            _uiState.update { it.copy(isDeletingAccount = false, deleteAccountError = error.localizedMessage ?: "Error al eliminar la cuenta.") }
                        }
                    )
                },
                onFailure = { error ->
                    Log.w("ProfileViewModel", "Error de reautenticación para eliminar cuenta", error)
                    _uiState.update {
                        it.copy(isDeletingAccount = false, showDeleteAccountReAuthDialog = true, deleteAccountError = error.localizedMessage ?: "Error de reautenticación.")
                    }
                }
            )
        }
    }

    // Helper para envolver reauthenticateUser con callback si no tienes versión suspendible
    private suspend fun suspendReauthenticate(password: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            authRepository.reauthenticateUser(password) { success, errorMessage ->
                if (continuation.isActive) { // Comprobar si la corutina sigue activa
                    if (success) {
                        continuation.resume(Result.success(Unit)) {}
                    } else {
                        continuation.resume(Result.failure(Exception(errorMessage ?: "Error de reautenticación desconocido"))) {}
                    }
                }
            }
        }
    }

    fun clearDeleteAccountMessages() {
        _uiState.update { it.copy(deleteAccountError = null, deleteAccountSuccessMessage = null) }
    }

    // Logout
    fun logout() {
        Log.d("ProfileViewModel", "Logout solicitado.")
        _uiState.update { it.copy(isLoggingOut = true) }
        // authStateFlow se encargará de llamar a stopObservingUserData() y resetear UiState
        authRepository.cerrarSesion()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ProfileViewModel", "onCleared: Cancelando todos los jobs.")
        authStateJob?.cancel()
        stopObservingUserData() // Asegura que los jobs de datos también se cancelen
    }

    // Helper para obtener detalles en paralelo (si es necesario fuera del Pager)
    // Este no se usa activamente en la lógica actual de ProfileViewModel
    private suspend fun fetchMovieDetailsInParallel(movieIds: List<Int>): List<MovieDetails> = coroutineScope {
        Log.d("ProfileViewModel", "Iniciando fetchMovieDetailsInParallel para ${movieIds.size} IDs")
        val deferredResults = movieIds.map { id ->
            async(Dispatchers.IO) { movieRepository.getMovieDetails(id) }
        }
        deferredResults.awaitAll()
            .mapNotNull { result -> result.getOrNull() }
            .also { Log.d("ProfileViewModel", "fetchMovieDetailsInParallel completado. Obtenidos ${it.size} detalles.") }
    }
}
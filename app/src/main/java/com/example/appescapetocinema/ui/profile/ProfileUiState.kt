package com.example.appescapetocinema.ui.profile

import com.example.appescapetocinema.util.AchievementDisplayInfo

data class ProfileUiState(
    val userEmail: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isLoggingOut: Boolean = false,

    val unlockedAchievements: Set<String> = emptySet(),

    val achievementsForDisplay: List<AchievementDisplayInfo> = emptyList(),

    val triviaHighScores: Map<String, Long> = emptyMap(),
    val watchProviderCountryPreference: String = "ES", // Valor por defecto inicial

    /** Lista de países disponibles para seleccionar, como Pares de (código, nombre legible). */
    val availableWatchProviderCountries: List<Pair<String, String>> = emptyList(),

    /** Indica si la lista de países disponibles se está cargando desde la API. */
    val isLoadingCountries: Boolean = false,

    /** Controla la visibilidad del diálogo de selección de país. */
    val showCountrySelectionDialog: Boolean = false,

    /** Opcional: Mensaje de error específico para la carga de países. */
    val countriesLoadingError: String? = null,
    val showClearMyListDialog: Boolean = false,
    val clearMyListMessage: String? = null, // Para feedback después de la acción
    val showDeleteAccountConfirmationDialog: Boolean = false,
    val showDeleteAccountReAuthDialog: Boolean = false,
    val deleteAccountPasswordInput: String = "",
    val isDeletingAccount: Boolean = false,
    val deleteAccountError: String? = null,
    val deleteAccountSuccessMessage: String? = null

)
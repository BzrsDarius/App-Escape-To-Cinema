package com.example.appescapetocinema.ui.detail

import com.example.appescapetocinema.network.dto.CountrySpecificProvidersDto
import com.example.appescapetocinema.model.NewsArticle
import com.example.appescapetocinema.model.Review
import com.example.appescapetocinema.ui.components.MovieItem
import com.example.appescapetocinema.network.dto.CastMemberDto
import com.example.appescapetocinema.network.dto.CrewMemberDto

data class DetailUiState(
    val movieDetails: MovieDetails? = null,
    val isLoading: Boolean = true, // Carga inicial principal
    val errorMessage: String? = null, // Error general o de carga inicial
    val isMovieInMyList: Boolean = false,
    val isUpdatingMyList: Boolean = false,
    val userRating: Double? = null,
    val isUpdatingRating: Boolean = false,
    val cast: List<CastMemberDto> = emptyList(),
    val director: CrewMemberDto? = null,
    val similarMovies: List<MovieItem> = emptyList(),

    val reviews: List<Review> = emptyList(),          // Lista de reseñas cargadas
    val isLoadingReviews: Boolean = false,       // ¿Están cargando específicamente las reseñas?
    val userReviewInputText: String = "",        // Texto en el TextField de reseña
    val canSubmitReview: Boolean = false,        // ¿Puede enviar la reseña? (Texto + Rating > 0)
    val isSubmittingReview: Boolean = false,     // ¿Está enviando la reseña ahora?
    val reviewSubmissionError: String? = null, // Error específico del envío de reseña
    val userHasExistingReview: Boolean = false,   // ¿El usuario ya tiene una reseña para esta peli?
    val currentUserId: String? = null,

    val isLoadingMovieNews: Boolean = true, // Estado de carga para las noticias
    val movieNewsArticles: List<NewsArticle> = emptyList(), // Lista de artículos de noticias
    val movieNewsError: String? = null, // Mensaje de error si falla la carga de noticias

    val isLoadingWatchProviders: Boolean = false, // Inicialmente false, se pone true al cargar
    // El Map tendrá el código del país (ej. "ES") como clave.
    val watchProvidersByCountry: Map<String, CountrySpecificProvidersDto>? = null,
    val watchProvidersError: String? = null,
    val selectedWatchProviderCountry: String = "ES" // País por defecto para mostrar, ej. España. Podría ser configurable.
)
package com.example.appescapetocinema.network

import com.example.appescapetocinema.network.dto.ChatQueryRequestDto
import com.example.appescapetocinema.network.dto.MovieDetailsDto
import com.example.appescapetocinema.network.dto.MovieListResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.appescapetocinema.network.dto.CreditsDto
import com.example.appescapetocinema.network.dto.FindResponseDto
import com.example.appescapetocinema.network.dto.GenresResponseDto
import com.example.appescapetocinema.network.dto.WatchProviderRegionsListResponseDto
import com.example.appescapetocinema.network.dto.WatchProvidersResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TmdbApiService {

    // Constantes base
    companion object {
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        const val POSTER_SIZE_W500 = "w500" // Tamaño común para posters
        const val BACKDROP_SIZE_W1280 = "w1280" // Tamaño común para backdrops

        // Función helper para construir URL completa de imagen
        fun getPosterUrl(path: String?): String? =
            path?.let { "$IMAGE_BASE_URL$POSTER_SIZE_W500$it" }

        fun getBackdropUrl(path: String?): String? =
            path?.let { "$IMAGE_BASE_URL$BACKDROP_SIZE_W1280$it" }
    }

    // --- Endpoints ---

    // Obtener películas populares
    @GET("api/movies/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int,
        @Query("language") language: String = "es-ES"
    ): MovieListResponseDto

    @GET("api/movies/top_rated")
    suspend fun getTopRatedMovies(
        @Query("page") page: Int,
        @Query("language") language: String = "es-ES"
    ): MovieListResponseDto

    @GET("api/movies/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int,
        @Query("language") language: String = "es-ES"
        // ELIMINADO: region (a menos que tu API lo maneje)
    ): MovieListResponseDto

    @GET("api/movies/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-ES"
    ): MovieDetailsDto

    @GET("api/movies/search")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("language") language: String = "es-ES",
        @Query("include_adult") includeAdult: Boolean = false
    ): MovieListResponseDto

    @GET("api/movies/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-ES"
    ): CreditsDto

    @GET("api/movies/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int,
        @Query("language") language: String = "es-ES"
    ): MovieListResponseDto

    @GET("api/genres") // Asumiendo que tu GenreController usa esta ruta
    suspend fun getMovieGenres(
        @Query("language") language: String = "es-ES"
    ): GenresResponseDto // O el nombre correcto de tu DTO

    // --- UNA SOLA FUNCIÓN DISCOVER ---
    @GET("api/movies/discover")
    suspend fun discoverMovies(
        @Query("page") page: Int,
        @Query("language") language: String? = "es-ES",
        @Query("includeAdult") includeAdult: Boolean? = false,
        @Query("genreId") genreId: Int?,
        @Query("year") year: Int?,
        @Query("release_date_gte") releaseDateGte: String?, // snake_case para el query param
        @Query("release_date_lte") releaseDateLte: String?, // snake_case
        @Query("with_people") withPeople: Int?,             // snake_case
        @Query("minRating") minRating: Float?,
        @Query("sortBy") sortBy: String?
    ): MovieListResponseDto // El DTO que devuelve para esta ruta


    // @GET("find/{external_id}") ...
    @GET("api/movies/find/{externalId}") // Ruta de TU API para /find
    suspend fun findByExternalIdThroughApi( // Nuevo nombre para distinguirlo
        @Path("externalId") externalId: String,
        @Query("externalSource") externalSource: String = "imdb_id", // Parámetro para TU API
        @Query("language") language: String = "es-ES"         // Parámetro para TU API
    ): FindResponseDto // Espera el DTO parseado

    @GET("api/movies/{movieId}/watch-providers")
    suspend fun getWatchProviders(@Path("movieId") movieId: Int): Response<WatchProvidersResponseDto>

    @POST("api/chat/query") // Endpoint POST en tu backend
    suspend fun sendChatQuery(
        @Body request: ChatQueryRequestDto // El cuerpo de la petición es el DTO de la query
    ): Response<String>

    @GET("api/movies/watch-provider-regions") // Llama a TU backend
    suspend fun getWatchProviderRegions(): Response<WatchProviderRegionsListResponseDto> // <-- USA TU DTO DE LISTA
}
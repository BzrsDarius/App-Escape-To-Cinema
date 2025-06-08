package com.example.appescapetocinema.repository

import androidx.paging.PagingData
import com.example.appescapetocinema.ui.detail.MovieDetails
import com.example.appescapetocinema.network.dto.CreditsDto
import com.example.appescapetocinema.network.dto.WatchProviderRegionsResponseDto
import com.example.appescapetocinema.network.dto.WatchProvidersResponseDto
import com.example.appescapetocinema.ui.components.MovieItem
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getPopularMoviesStream(): Flow<PagingData<MovieItem>>
    fun getTopRatedMoviesStream(): Flow<PagingData<MovieItem>>
    fun getNowPlayingMoviesStream(): Flow<PagingData<MovieItem>>
    fun getSimilarMoviesStream(movieId: Int): Flow<PagingData<MovieItem>> // O Long
    fun searchMoviesStream(query: String): Flow<PagingData<MovieItem>>
    fun discoverMoviesStream(
        genreId: Int?,
        year: Int?,
        minRating: Float?,
        sortBy: String
    ): Flow<PagingData<MovieItem>>

    suspend fun getMovieDetails(movieId: Int): Result<MovieDetails> // O Long
    suspend fun getMovieCredits(movieId: Int): Result<CreditsDto> // O Long
    suspend fun getMovieGenres(): Result<List<Genre>> // Usa tu modelo Genre
    suspend fun findTmdbIdViaApi(imdbId: String): Result<Int?> // Devuelve tmdb_id nullable
    suspend fun getWatchProviders(movieId: Int): Result<WatchProvidersResponseDto>
    suspend fun getWatchProviderRegions(): Result<List<WatchProviderRegionsResponseDto>>
}

data class Genre(val id: Int, val name: String)
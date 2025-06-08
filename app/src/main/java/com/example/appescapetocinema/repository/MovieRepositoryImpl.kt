package com.example.appescapetocinema.repository

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.appescapetocinema.ui.components.MovieItem
import com.example.appescapetocinema.ui.detail.MovieDetails
import com.example.appescapetocinema.network.NetworkModule
import com.example.appescapetocinema.network.TmdbApiService
import com.example.appescapetocinema.network.dto.CreditsDto
import com.example.appescapetocinema.network.dto.MovieResultDto
import com.example.appescapetocinema.network.dto.MovieDetailsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException
import android.util.Log
import com.example.appescapetocinema.network.dto.WatchProviderRegionsResponseDto
import com.example.appescapetocinema.network.dto.WatchProvidersResponseDto
import java.io.IOException

class MovieRepositoryImpl(
    private val apiService: TmdbApiService = NetworkModule.tmdbApiService
) : MovieRepository {

    // --- Configuración de Paging ---
    companion object {
        const val NETWORK_PAGE_SIZE = 20
    }

    // --- IMPLEMENTACIÓN DE FUNCIONES PAGINADAS ---

    override fun getPopularMoviesStream(): Flow<PagingData<MovieItem>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                MoviePagingSource(apiService, MoviePagingSource.MovieListType.POPULAR)
            }
        ).flow
    }

    override fun getTopRatedMoviesStream(): Flow<PagingData<MovieItem>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                MoviePagingSource(apiService, MoviePagingSource.MovieListType.TOP_RATED)
            }
        ).flow
    }

    override fun getNowPlayingMoviesStream(): Flow<PagingData<MovieItem>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                MoviePagingSource(apiService, MoviePagingSource.MovieListType.NOW_PLAYING)
            }
        ).flow
    }

    override fun getSimilarMoviesStream(movieId: Int): Flow<PagingData<MovieItem>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                MoviePagingSource(
                    tmdbApiService = apiService,
                    listType = MoviePagingSource.MovieListType.SIMILAR,
                    relatedToMovieId = movieId
                )
            }
        ).flow
    }

    override fun searchMoviesStream(query: String): Flow<PagingData<MovieItem>> {
        if (query.isBlank()) return emptyFlow()
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                MoviePagingSource(
                    tmdbApiService = apiService,
                    listType = MoviePagingSource.MovieListType.SEARCH,
                    query = query
                )
            }
        ).flow
    }

    override fun discoverMoviesStream(
        genreId: Int?,
        year: Int?,
        minRating: Float?, // Acepta minRating
        sortBy: String
    ): Flow<PagingData<MovieItem>> {
        Log.d("MovieRepositoryImpl", "Creando Pager para DISCOVER (YOUR API) (Genre: $genreId, Year: $year, MinRating: $minRating, SortBy: $sortBy)")
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                MoviePagingSource(
                    tmdbApiService = apiService,
                    listType = MoviePagingSource.MovieListType.DISCOVER_FILTERED,
                    genreId = genreId,
                    year = year,
                    minRating = minRating, // Pasa minRating
                    sortBy = sortBy
                )
            }
        ).flow
    }

    override suspend fun getWatchProviders(movieId: Int): Result<WatchProvidersResponseDto> {
        Log.d("MovieRepositoryImpl", "getWatchProviders (via YOUR API) para movieId: $movieId")

        //Llamada directa y manejo manual de Result
        return try {
            val response = apiService.getWatchProviders(movieId)

            if (response.isSuccessful && response.body() != null) {
                Log.d("MovieRepositoryImpl", "Watch providers obtenidos exitosamente para movieId: $movieId")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Respuesta de error desconocida"
                Log.e("MovieRepositoryImpl", "Error al obtener watch providers para movieId $movieId: ${response.code()} - ${response.message()}. Body: $errorBody")
                Result.failure(IOException("Error ${response.code()}: ${response.message()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("MovieRepositoryImpl", "Excepción en getWatchProviders para movieId $movieId", e)
            Result.failure(e)
        }
    }

    override suspend fun getWatchProviderRegions(): Result<List<WatchProviderRegionsResponseDto>> {
        Log.d("MovieRepositoryImpl", "getWatchProviderRegions (via YOUR API) llamado")
        return try {
            // apiService.getWatchProviderRegions() devuelve Response<WatchProviderRegionsListResponseDto>
            val response = apiService.getWatchProviderRegions()

            if (response.isSuccessful && response.body() != null) {
                val listOfIndividualRegions: List<WatchProviderRegionsResponseDto> = response.body()!!.results
                Log.d("MovieRepositoryImpl", "Regiones obtenidas: ${listOfIndividualRegions.size}")
                Result.success(listOfIndividualRegions)
            } else {
                Log.e("MovieRepositoryImpl", "Error getWatchProviderRegions: ${response.code()} - ${response.message()}")
                Result.failure(IOException("Error ${response.code()} al obtener regiones: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("MovieRepositoryImpl", "Excepción en getWatchProviderRegions", e)
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getMovieDetails(movieId: Int): Result<MovieDetails> {
        Log.d("MovieRepositoryImpl", "getMovieDetails (YOUR API) para ID: $movieId")
        return safeApiCall {
            val detailsDto = apiService.getMovieDetails(movieId) // Sin apiKey
            mapToMovieDetails(detailsDto) ?: throw NullPointerException("No se pudieron mapear los detalles (ID: $movieId)")
        }
    }

    override suspend fun getMovieCredits(movieId: Int): Result<CreditsDto> {
        Log.d("MovieRepositoryImpl", "getMovieCredits (YOUR API) para ID: $movieId")
        return safeApiCall {
            apiService.getMovieCredits(movieId) // Sin apiKey
        }
    }

    override suspend fun getMovieGenres(): Result<List<Genre>> {
        Log.d("MovieRepositoryImpl", "getMovieGenres (YOUR API) llamado")
        return safeApiCall {
            val responseDto = apiService.getMovieGenres() // Sin apiKey
            responseDto.genres.map { genreDto ->
                Genre(id = genreDto.id, name = genreDto.name)
            }
        }
    }
    override suspend fun findTmdbIdViaApi(imdbId: String): Result<Int?> {
        if (imdbId.isBlank()) {
            Log.w("MovieRepositoryImpl", "Intento de buscar con IMDb ID vacío.")
            return Result.success(null)
        }
        Log.d("MovieRepositoryImpl", "findTmdbIdViaApi llamado para IMDb ID: $imdbId (llamando a nuestra API)")

        return safeApiCall { // Tu helper para llamadas seguras
            // Llama al método de la interfaz que apunta a TU API
            val responseDto = apiService.findByExternalIdThroughApi(
                externalId = imdbId,
                externalSource = "imdb_id"
            )
            val tmdbId = responseDto.movieResults?.firstOrNull()?.id
            Log.d("MovieRepositoryImpl", "TMDb ID encontrado (vía API) para IMDb $imdbId: $tmdbId")
            tmdbId // Esto será envuelto en Result.success por safeApiCall
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseYear(releaseDate: String?): String {
        return try {
            releaseDate?.let { LocalDate.parse(it).year.toString() } ?: "N/A"
        } catch (e: DateTimeParseException) {
            "N/A"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O) // Si mapToMovieDetails usa parseYear
    private fun mapToMovieDetails(dto: MovieDetailsDto?): MovieDetails? {
        return dto?.let {
            MovieDetails(
                id = it.id,
                title = it.title ?: "Título no disponible",
                overview = it.overview ?: "Sin sinopsis.",
                posterUrl = TmdbApiService.getPosterUrl(it.posterPath),
                backdropUrl = TmdbApiService.getBackdropUrl(it.backdropPath),
                releaseYear = parseYear(it.releaseDate), // Se llama aquí
                genres = it.genres?.mapNotNull { genreDto -> genreDto.name } ?: emptyList(),
                rating = it.voteAverage ?: 0.0,
                imdbId = it.imdbId
            )
        }
    }

    private suspend inline fun <T> safeApiCall(crossinline apiCall: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(apiCall.invoke())
            } catch (e: Exception) {
                Log.e("MovieRepositoryImpl", "API Call (YOUR API) failed: ${e.message}", e)
                Result.failure(Exception("Error de red o API: ${e.message}", e))
            }
        }
    }
    private fun mapToMovieItems(dtoList: List<MovieResultDto>?): List<MovieItem> {
        return dtoList?.mapNotNull { dto ->
            if (dto.id != null && dto.title != null) {
                MovieItem(
                    id = dto.id,
                    title = dto.title,
                    posterUrl = TmdbApiService.getPosterUrl(dto.posterPath)
                )
            } else { null }
        } ?: emptyList()
    }
}
package com.example.appescapetocinema.repository // O tu paquete elegido

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.appescapetocinema.BuildConfig // API Key
import com.example.appescapetocinema.ui.components.MovieItem // Modelo UI
import com.example.appescapetocinema.network.TmdbApiService // Interfaz Retrofit
import com.example.appescapetocinema.network.dto.MovieResultDto // DTO API
import retrofit2.HttpException
import java.io.IOException
import android.util.Log
import com.example.appescapetocinema.model.DEFAULT_SORT_BY
import com.example.appescapetocinema.network.dto.MovieListResponseDto

// Página inicial estándar para la API de TMDb
private const val TMDB_STARTING_PAGE_INDEX = 1
private const val JOHN_CARPENTER_PERSON_ID_PAGING = 11770

class MoviePagingSource(
    private val tmdbApiService: TmdbApiService, // Tu interfaz que llama a TU backend
    private val listType: MovieListType,
    private val query: String? = null,
    private val relatedToMovieId: Int? = null,
    private val genreId: Int? = null,
    private val year: Int? = null,
    private val releaseDateGte: String? = null,
    private val releaseDateLte: String? = null,
    private val withPeopleId: Int? = null,
    private val minRating: Float? = null,
    private val sortBy: String = DEFAULT_SORT_BY
) : PagingSource<Int, MovieItem>() {

    enum class MovieListType {
        POPULAR, TOP_RATED, NOW_PLAYING, SIMILAR, SEARCH,
        DISCOVER_FILTERED, // Se mantiene para filtros dinámicos
        HORROR_MOVIES,     // ID Género 27
        ACTION_MOVIES,     // ID Género 28
        EIGHTIES_MOVIES,   // primary_release_date.gte=1980-01-01, primary_release_date.lte=1989-12-31
        CARPENTER_MOVIES   // with_people=11770
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MovieItem> {
        val page = params.key ?: TMDB_STARTING_PAGE_INDEX // Sigue usando tu TMDB_STARTING_PAGE_INDEX
        var effectiveGenreId: Int? = this.genreId
        var effectiveYear: Int? = this.year
        var effectiveReleaseDateGte: String? = this.releaseDateGte
        var effectiveReleaseDateLte: String? = this.releaseDateLte
        var effectiveWithPeopleId: Int? = this.withPeopleId
        val effectiveSortBy = this.sortBy // sortBy se pasa tal cual o usa el default del constructor
        val effectiveMinRating = this.minRating // minRating se pasa tal cual

        // Sobreescribir/configurar parámetros efectivos para los tipos temáticos
        when (listType) {
            MovieListType.HORROR_MOVIES -> {
                effectiveGenreId = 27 // ID de Género Terror
            }
            MovieListType.ACTION_MOVIES -> {
                effectiveGenreId = 28 // ID de Género Acción
            }
            MovieListType.EIGHTIES_MOVIES -> {
                effectiveReleaseDateGte = "1980-01-01"
                effectiveReleaseDateLte = "1989-12-31"
                effectiveYear = null // Anulamos el año específico si es una búsqueda por década
            }
            MovieListType.CARPENTER_MOVIES -> {
                effectiveWithPeopleId = JOHN_CARPENTER_PERSON_ID_PAGING
            }
            else -> { /* No action needed for other types here */ }
        }

        Log.d("MoviePagingSource", "load (YOUR API): Page=$page, OriginalListType=$listType, " +
                "Query='$query', RelatedTo=$relatedToMovieId, " +
                "Final Params -> Genre=$effectiveGenreId, Year=$effectiveYear, DateGTE=$effectiveReleaseDateGte, DateLTE=$effectiveReleaseDateLte, " +
                "People=$effectiveWithPeopleId, MinRating=$effectiveMinRating, SortBy='$effectiveSortBy'")

        return try {
            val response: MovieListResponseDto = when (listType) {
                MovieListType.POPULAR -> tmdbApiService.getPopularMovies(page = page)
                MovieListType.TOP_RATED -> tmdbApiService.getTopRatedMovies(page = page)
                MovieListType.NOW_PLAYING -> tmdbApiService.getNowPlayingMovies(page = page)
                MovieListType.SEARCH -> {
                    val searchQuery = query?.takeIf { it.isNotBlank() }
                        ?: return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null) // Devuelve página vacía si no hay query
                    tmdbApiService.searchMovies(query = searchQuery, page = page)
                }
                MovieListType.SIMILAR -> {
                    val movieId = relatedToMovieId
                        ?: throw IllegalStateException("Movie ID es requerido para SIMILAR")
                    tmdbApiService.getSimilarMovies(movieId = movieId, page = page)
                }
                // Todos los tipos de descubrimiento (incluidos los nuevos temáticos) usan el mismo endpoint
                MovieListType.DISCOVER_FILTERED,
                MovieListType.HORROR_MOVIES,
                MovieListType.ACTION_MOVIES,
                MovieListType.EIGHTIES_MOVIES,
                MovieListType.CARPENTER_MOVIES -> {
                    tmdbApiService.discoverMovies(
                        page = page,
                        genreId = effectiveGenreId,
                        year = effectiveYear,
                        releaseDateGte = effectiveReleaseDateGte, // Pasa el parámetro para el @Query("release_date_gte")
                        releaseDateLte = effectiveReleaseDateLte, // Pasa el parámetro para el @Query("release_date_lte")
                        withPeople = effectiveWithPeopleId,       // Pasa el parámetro para el @Query("with_people")
                        minRating = effectiveMinRating,
                        sortBy = effectiveSortBy
                    )
                }
            }

            val movies = mapToMovieItems(response.results)
            Log.d("MoviePagingSource", "load (YOUR API): Page $page ($listType) - ${movies.size} items mapeados. Total Pages API: ${response.totalPages}")

            val prevKey = if (page == TMDB_STARTING_PAGE_INDEX) null else page - 1
            val nextKey = if (movies.isEmpty() || response.totalPages == 0 || page >= response.totalPages) null else page + 1


            LoadResult.Page(data = movies, prevKey = prevKey, nextKey = nextKey)

        } catch (exception: IOException) {
            Log.e("MoviePagingSource", "load (YOUR API): IOException ($listType, Page $page)", exception)
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            Log.e("MoviePagingSource", "load (YOUR API): HttpException ($listType, Page $page) - Code: ${exception.code()}", exception)
            return LoadResult.Error(exception)
        } catch (exception: Exception) { // Captura genérica para otros posibles errores
            Log.e("MoviePagingSource", "load (YOUR API): Exception Genérica ($listType, Page $page)", exception)
            return LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MovieItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.let { anchorPage ->
                anchorPage.prevKey?.plus(1) ?: anchorPage.nextKey?.minus(1)
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
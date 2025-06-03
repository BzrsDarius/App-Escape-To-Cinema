package com.example.appescapetocinema.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData // Importar PagingData
import androidx.paging.cachedIn // Importar cachedIn
import com.example.appescapetocinema.network.NetworkModule
import com.example.appescapetocinema.repository.MoviePagingSource
import com.example.appescapetocinema.repository.MovieRepository // Importar Interfaz Repo
import com.example.appescapetocinema.repository.MovieRepositoryImpl
import com.example.appescapetocinema.ui.components.MovieItem // Importar Modelo UI
import kotlinx.coroutines.flow.Flow // Importar Flow

// Ya no necesitamos un UiState complejo aquí para las listas
// data class HomeUiState(...)

class HomeViewModel(
    private val movieRepository: MovieRepository // Recibe la interfaz
) : ViewModel() {

    // --- Exponer Flows Paginados Directamente ---

    val popularMoviesFlow: Flow<PagingData<MovieItem>> = movieRepository
        .getPopularMoviesStream() // <-- Llama a la función Stream del repo
        .cachedIn(viewModelScope) // <-- Cachea el resultado

    val topRatedMoviesFlow: Flow<PagingData<MovieItem>> = movieRepository
        .getTopRatedMoviesStream() // <-- Llama a la función Stream del repo
        .cachedIn(viewModelScope) // <-- Cachea el resultado

    val nowPlayingMoviesFlow: Flow<PagingData<MovieItem>> = movieRepository
        .getNowPlayingMoviesStream() // <-- Llama a la función Stream del repo
        .cachedIn(viewModelScope) // <-- Cachea el resultado

    private val tmdbApiServiceInstance = NetworkModule.tmdbApiService // Ejemplo, ajusta a cómo obtienes tu servicio

    val horrorMoviesFlow: Flow<PagingData<MovieItem>> = Pager(
        config = PagingConfig(pageSize = MovieRepositoryImpl.NETWORK_PAGE_SIZE),
        pagingSourceFactory = {
            MoviePagingSource(
                tmdbApiService = tmdbApiServiceInstance, // Pasa el servicio
                listType = MoviePagingSource.MovieListType.HORROR_MOVIES
                // sortBy = "vote_average.desc" // Opcional: ordenar las de terror por valoración
            )
        }
    ).flow.cachedIn(viewModelScope)

    val actionMoviesFlow: Flow<PagingData<MovieItem>> = Pager(
        config = PagingConfig(pageSize = MovieRepositoryImpl.NETWORK_PAGE_SIZE),
        pagingSourceFactory = {
            MoviePagingSource(
                tmdbApiService = tmdbApiServiceInstance,
                listType = MoviePagingSource.MovieListType.ACTION_MOVIES
                // sortBy = "popularity.desc" // Opcional: ordenar por popularidad
            )
        }
    ).flow.cachedIn(viewModelScope)

    val eightiesMoviesFlow: Flow<PagingData<MovieItem>> = Pager(
        config = PagingConfig(pageSize = MovieRepositoryImpl.NETWORK_PAGE_SIZE),
        pagingSourceFactory = {
            MoviePagingSource(
                tmdbApiService = tmdbApiServiceInstance,
                listType = MoviePagingSource.MovieListType.EIGHTIES_MOVIES,
                sortBy = "popularity.desc" // Ejemplo: populares de los 80
            )
        }
    ).flow.cachedIn(viewModelScope)

    val carpenterMoviesFlow: Flow<PagingData<MovieItem>> = Pager(
        config = PagingConfig(pageSize = MovieRepositoryImpl.NETWORK_PAGE_SIZE),
        pagingSourceFactory = {
            MoviePagingSource(
                tmdbApiService = tmdbApiServiceInstance,
                listType = MoviePagingSource.MovieListType.CARPENTER_MOVIES,
                sortBy = "release_date.desc" // Ejemplo: las más recientes de Carpenter primero
            )
        }
    ).flow.cachedIn(viewModelScope)
}

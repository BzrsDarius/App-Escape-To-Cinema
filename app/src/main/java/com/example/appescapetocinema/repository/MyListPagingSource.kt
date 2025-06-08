package com.example.appescapetocinema.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.appescapetocinema.ui.detail.MovieDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class MyListPagingSource(
    private val movieIds: List<Int>, // Lista completa de IDs de Firestore
    private val movieRepository: MovieRepository // Para obtener detalles de cada película
) : PagingSource<Int, MovieDetails>() {
    companion object {
        const val MY_LIST_PAGE_SIZE = 10 // O el tamaño que prefieras
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MovieDetails> {
        // Si es la primera carga (key == null), empezamos desde el índice 0
        val currentLoadingPageIndex = params.key ?: 0
        Log.d("MyListPagingSource", "load: Index=$currentLoadingPageIndex, LoadSize=${params.loadSize}")

        return try {
            // Calcula el rango de IDs a cargar para esta página
            // El índice de inicio es la 'key' (o 0)
            val startIndex = currentLoadingPageIndex
            // El índice de fin es el inicio + tamaño de carga, sin exceder el tamaño de la lista de IDs
            val endIndex = (startIndex + params.loadSize).coerceAtMost(movieIds.size)

            // Si el índice de inicio ya está más allá del final de la lista, no hay más datos
            if (startIndex >= movieIds.size) {
                Log.d("MyListPagingSource", "load: No más IDs para cargar, startIndex=$startIndex >= size=${movieIds.size}")
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }

            // Obtiene el "slice" de IDs para esta página
            val idsToLoad = movieIds.subList(startIndex, endIndex)
            Log.d("MyListPagingSource", "load: IDs a cargar para esta página: $idsToLoad")

            // Obtener detalles para cada ID en paralelo (usando withContext para IO)
            val movieDetailsList = withContext(Dispatchers.IO) {
                idsToLoad.mapNotNull { id ->
                    // Llamamos al repositorio para obtener detalles
                    // Manejamos Result para obtener el valor o null si falla
                    val result = movieRepository.getMovieDetails(id)
                    result.getOrNull()?.also {
                        Log.d("MyListPagingSource", "Detalles obtenidos para ID: $id")
                    } ?: run {
                        Log.w("MyListPagingSource", "Fallo al obtener detalles para ID: $id. Error: ${result.exceptionOrNull()?.message}")
                        null // Ignorar si un getMovieDetails falla
                    }
                }
            }
            Log.d("MyListPagingSource", "load: Obtenidos ${movieDetailsList.size} detalles para esta página.")

            // Clave para la página anterior: el índice de inicio de la página anterior
            // Es null si estamos en la primera página (currentLoadingPageIndex == 0)
            val prevKey = if (currentLoadingPageIndex == 0) null else currentLoadingPageIndex - params.loadSize

            // Clave para la página siguiente: el índice de inicio de la siguiente página
            // Es null si hemos llegado al final de la lista de IDs
            val nextKey = if (endIndex >= movieIds.size) null else endIndex

            Log.d("MyListPagingSource", "load: Page created. PrevKey=$prevKey, NextKey=$nextKey")
            LoadResult.Page(
                data = movieDetailsList,
                prevKey = prevKey?.coerceAtLeast(0), // Asegura que prevKey no sea negativo
                nextKey = nextKey
            )

        } catch (exception: Exception) {
            Log.e("MyListPagingSource", "load: Error cargando página", exception)
            LoadResult.Error(exception)
        }
    }

    // Ayuda a Paging a determinar qué cargar al refrescar/invalidar
    override fun getRefreshKey(state: PagingState<Int, MovieDetails>): Int? {
        // Intenta encontrar la página más cercana a la última posición accedida
        // y devuelve su clave (índice de inicio).
        // La lógica común es encontrar el item más cercano al anchorPosition,
        // luego encontrar la página que contiene ese item y devolver su prevKey + loadSize.
        // Para un PagingSource basado en índice, podemos simplemente intentar reiniciar
        // desde el principio (0) o desde la posición más cercana.
        return state.anchorPosition?.let { anchorPosition ->
            // Calcula el índice del elemento ancla en la lista de todos los IDs
            // Esta es una aproximación. Un PagingSource basado en offset/límite es más directo para esto.
            // Si state.closestItemToPosition(anchorPosition) existe, podemos encontrar su índice
            // en la lista original de movieIds, y luego determinar la clave de página.
            // Por simplicidad, si hay anchor, intentamos recargar cerca de él.
            // Si no, Paging volverá a llamar a load con params.key = null.
            val closestItemIndex = state.closestItemToPosition(anchorPosition)?.id?.let { movieDetailsId ->
                movieIds.indexOf(movieDetailsId)
            }
            closestItemIndex?.let {
                // Queremos la clave de la página que contiene este índice
                (it / state.config.pageSize) * state.config.pageSize
            }
        }
        // Si no hay anchor, Paging usará null como key, lo que resultará en que
        // load() empiece desde el índice 0 (TMDB_STARTING_PAGE_INDEX no aplica aquí)
    }
}
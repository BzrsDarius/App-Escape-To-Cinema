package com.example.appescapetocinema.ui.search

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.appescapetocinema.ui.components.MovieCard
import com.example.appescapetocinema.repository.MovieRepositoryImpl
import com.example.appescapetocinema.model.sortOptions
import com.example.appescapetocinema.repository.MovieRepository
import com.example.appescapetocinema.repository.UserProfileRepository
import com.example.appescapetocinema.repository.UserProfileRepositoryImpl
import com.example.appescapetocinema.ui.components.MovieItem
import com.example.appescapetocinema.ui.components.ShimmerMovieCardPlaceholder
import java.text.NumberFormat

// --- SearchScreen (UI - Con Filtros y Paging) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    searchResults: LazyPagingItems<MovieItem>,
    onQueryChange: (String) -> Unit,
    onSearchPerform: () -> Unit,
    onClearClick: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onGenreSelected: (Int?) -> Unit,
    onRetryGenres: () -> Unit,
    onYearChange: (String) -> Unit, // Recibe String del TextField
    onMinRatingChange: (Float?) -> Unit, // Nullable Float
    onSortByChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var sliderRatingPosition by remember(uiState.selectedMinRating) { mutableStateOf(uiState.selectedMinRating ?: 0f) }
    val ratingNumberFormat = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 } }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // --- Barra de Búsqueda ---
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 1.dp),
            placeholder = { Text("Buscar películas...") },
            leadingIcon = { Icon(Icons.Filled.Search, "Buscar", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = { if (uiState.searchQuery.isNotEmpty()) { IconButton(onClick = onClearClick) { Icon(Icons.Filled.Clear, "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant) } } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchPerform() }),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)){
            Spacer(modifier = Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically){
                Text("Min:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = sliderRatingPosition,
                    onValueChange = { sliderRatingPosition = it },
                    onValueChangeFinished = {
                        // Pasa null si es 0, sino, el valor float
                        val finalRating = sliderRatingPosition.takeIf { it > 0f }
                        onMinRatingChange(finalRating)
                    },
                    valueRange = 0f..10f,
                    steps = 19, // Pasos de 0.5 ( (10/0.5)-1 )
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(40.dp),
                    colors = SliderDefaults.colors( // Colores tema
                        thumbColor = MaterialTheme.colorScheme.secondary, // Magenta
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        activeTickColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                // Valor numérico
                Text(
                    text = if (sliderRatingPosition > 0f) ratingNumberFormat.format(sliderRatingPosition) + "+" else "Cualq.", // Muestra valor o "Cualquiera"
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary, // Magenta
                    modifier = Modifier.width(45.dp), // Ancho para "Cualq."
                    textAlign = TextAlign.End
                )
                // Botón Reset Rating (opcional)
                if (uiState.selectedMinRating != null) {
                    TextButton(onClick = { onMinRatingChange(null) }, contentPadding = PaddingValues(start=8.dp)) {
                        Text("X", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(10.dp)) // Espacio placeholder
                }
                OutlinedTextField(
                    value = uiState.selectedYear?.toString() ?: "", // Muestra año o vacío
                    onValueChange = { text ->
                        // Permite borrar o escribir números de hasta 4 dígitos
                        if (text.isEmpty() || (text.length <= 4 && text.all { it.isDigit() })) {
                            onYearChange(text) // Llama a lambda del ViewModel con el String
                        }
                    },
                    label = { Text("Año", style = MaterialTheme.typography.labelSmall) }, // Etiqueta pequeña
                    modifier = Modifier.width(90.dp).height(55.dp), // Ancho un poco más ajustado
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, // Teclado numérico
                        imeAction = ImeAction.Done // Acción "Hecho"
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide(); focusManager.clearFocus() } // Oculta teclado
                    ),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center), // Texto centrado
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

            }
            Spacer(modifier = Modifier.height(5.dp))

        // --- FILA DE CHIPS DE GÉNERO ---
        if (uiState.isLoadingGenres) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp))
        } else if (uiState.genreList.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.genreList, key = { it.id }) { genre ->
                    FilterChip(
                        selected = uiState.selectedGenreId == genre.id,
                        onClick = { onGenreSelected(genre.id) },
                        label = { Text(genre.name, style = MaterialTheme.typography.labelMedium) }, // Typo tema
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer, // Color tema
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer // Color tema
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
                            enabled = true,
                            selected = uiState.selectedGenreId == genre.id
                        )
                    )
                }
            }
        } else if (uiState.genreList.isEmpty()) {
            TextButton(onClick = onRetryGenres, modifier = Modifier.padding(horizontal=16.dp)) {
                Text("Reintentar cargar géneros")
            }
        }
            // --- Dropdown para Ordenar ---
            // Solo se habilita si hay algún filtro de descubrimiento activo (género, año o rating)
            // ya que el endpoint /search no soporta sort_by
            val filtersActive = uiState.selectedGenreId != null || uiState.selectedYear != null || uiState.selectedMinRating != null
            ExposedDropdownMenuBox(
                expanded = sortDropdownExpanded,
                onExpandedChange = {
                    if (filtersActive) sortDropdownExpanded = !sortDropdownExpanded
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = sortOptions.find { it.first == uiState.sortBy }?.second ?: "Ordenar por...", // Muestra texto legible
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ordenar por", style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().height(60.dp), // Necesario para el dropdown
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = filtersActive // Habilitado solo si hay filtros de discover
                )
                ExposedDropdownMenu(
                    expanded = sortDropdownExpanded && filtersActive,
                    onDismissRequest = { sortDropdownExpanded = false }
                ) {
                    sortOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption.second, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onSortByChange(selectionOption.first) // Llama al ViewModel
                                sortDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            // --- FIN Dropdown ---
        }

// Este es el Box que contiene la grid y los mensajes de estado superpuestos.

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            val refreshLoadState = searchResults.loadState.refresh
            val appendLoadState = searchResults.loadState.append
            val itemCount = searchResults.itemCount

            Log.d("SearchScreenState", "Recomposing. Refresh: $refreshLoadState, Items: $itemCount, Query: '${uiState.searchQuery}', Genre: ${uiState.selectedGenreId}, Year: ${uiState.selectedYear}, Rating: ${uiState.selectedMinRating}")

            // --- Lógica de la Cuadrícula de Películas ---
            // La LazyVerticalGrid se muestra si:
            // 1. No hay un error de refresh Y hay items.
            // O 2. Está en estado de carga (refresh o append) PERO ya hay items (para actualizaciones en segundo plano).
            // O 3. La carga inicial terminó (NotLoading) y hay items.
            // Básicamente, si hay items, se intenta mostrar la grid.
            // El caso de itemCount == 0 se maneja por los mensajes de texto en el 'when' de abajo.

            if (itemCount > 0 || (refreshLoadState is LoadState.Loading && itemCount > 0)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = searchResults.itemCount, // Usar searchResults.itemCount directamente
                        key = searchResults.itemKey { it.id },
                        contentType = searchResults.itemContentType { "searchResult" }
                    ) { index ->
                        val movie = searchResults[index]
                        if (movie != null) {
                            MovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                        } else {
                            // Placeholder mientras se carga un item específico de la página
                            ShimmerMovieCardPlaceholder()
                        }
                    }
                    // Footer para Carga/Error de Append
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        HandlePagingLoadStatesGridItem(
                            loadState = appendLoadState, // Usar el estado de append
                            onRetry = { searchResults.retry() }
                        )
                    }
                }
            }

            // --- Indicadores/Mensajes Superpuestos ---
            // Estos se mostrarán si la condición de arriba para la grid no se cumple totalmente
            // o si queremos superponer un indicador de carga global.
            when {
                // 1. Carga Inicial (Solo si no hay items y el estado de refresh es Loading)
                refreshLoadState is LoadState.Loading && itemCount == 0 -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Log.d("SearchScreenUI", "UI: Mostrando Indicador de Carga Global")
                }
                // 2. Error Inicial (Solo si no hay items y el estado de refresh es Error)
                refreshLoadState is LoadState.Error && itemCount == 0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp).align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Error al buscar: ${ (refreshLoadState as LoadState.Error).error.localizedMessage ?: "Error desconocido" }",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { searchResults.retry() }) { Text("Reintentar") }
                        Log.d("SearchScreenUI", "UI: Mostrando Estado de Error Inicial")
                    }
                }
                // 3. Casos de Lista Vacía (cuando la carga de refresh NO es Loading ni Error, y itemCount es 0)
                // Esto se activará cuando PagingData.empty() se procese.
                refreshLoadState is LoadState.NotLoading && itemCount == 0 -> {
                    val discoveryFiltersActive = uiState.selectedGenreId != null ||
                            uiState.selectedYear != null ||
                            uiState.selectedMinRating != null

                    if (uiState.searchQuery.isNotBlank() || discoveryFiltersActive) {
                        // Caso: Búsqueda activa o filtros activos, pero 0 resultados
                        Text(
                            "No se encontraron resultados para tu selección.",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Log.d("SearchScreenUI", "UI: Mostrando 'No se encontraron resultados'")
                    } else {
                        // Caso: Estado inicial - sin query y sin filtros de descubrimiento activos
                        Text(
                            "Busca o selecciona un filtro para empezar.",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Log.d("SearchScreenUI", "UI: Mostrando 'Busca o selecciona un filtro'")
                    }
                }
                else -> { Log.d("SearchScreenUI", "UI: Otro estado o grid con items. Refresh: $refreshLoadState, Items: $itemCount") }
            }
        } // Fin Box Contenido
    } // Fin Column principal
}


// --- HandlePagingLoadStatesGridItem (Asegúrate que está definido/importado y usa tema) ---
@Composable
fun HandlePagingLoadStatesGridItem( loadState: LoadState, onRetry: () -> Unit ) {
    Box( modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center ) {
        when (loadState) {
            is LoadState.Loading -> CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
            is LoadState.Error -> TextButton(onClick = onRetry) { Text("Reintentar") } // Usa tema
            else -> {}
        }
    }
}


// --- SearchScreenContainer (Aplicando Tema) ---
@Composable
fun SearchScreenContainer(
    navController: NavController,
    // ---- Obtener instancias de los repositorios ----
    movieRepository: MovieRepository = remember { MovieRepositoryImpl() },
    userProfileRepository: UserProfileRepository = remember { UserProfileRepositoryImpl() }
) {
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(
            movieRepository,
            userProfileRepository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val searchResultsPagingItems = viewModel.searchResultsFlow.collectAsLazyPagingItems()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    SearchScreen(
        uiState = uiState,
        searchResults = searchResultsPagingItems,
        onQueryChange = viewModel::onSearchQueryChange,
        onSearchPerform = { keyboardController?.hide(); focusManager.clearFocus(); },
        onClearClick = viewModel::clearSearchAndFilters,
        onMovieClick = { movieId ->
            keyboardController?.hide()
            focusManager.clearFocus()
            navController.navigate(Screen.Detail.createRoute(movieId))
        },
        onGenreSelected = viewModel::onGenreSelected,
        onRetryGenres = viewModel::retry,
        onYearChange = { yearString ->
            val yearInt = yearString.toIntOrNull()
            if (uiState.selectedYear != yearInt) { // Solo actualiza si el valor es realmente diferente
                viewModel.onYearSelected(yearInt)
            }
        },
        onMinRatingChange = viewModel::onMinRatingChange,
        onSortByChange = viewModel::onSortByChange
    )
}


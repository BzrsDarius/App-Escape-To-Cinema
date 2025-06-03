package com.example.appescapetocinema.ui.more

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos // O ChevronRight
import androidx.compose.material.icons.filled.* // Para los iconos de cada sección
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class MoreNavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String // Ruta de destino
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreenContainer(navController: NavController) {
    // Lista de items que aparecerán en la pantalla "Más"
    val moreItems = listOf(
        MoreNavigationItem("Noticias", Icons.Filled.Feed, Screen.News.route),
        MoreNavigationItem("Trivia", Icons.Filled.Quiz, Screen.TriviaSelection.route),
        MoreNavigationItem("Línea de Tiempo del Cine", Icons.Filled.Timeline, Screen.Timeline.route),
        MoreNavigationItem("CineBot Asistente", Screen.ChatBot.icon!!, Screen.ChatBot.route),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Más Opciones", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        MoreScreen(
            modifier = Modifier.padding(paddingValues),
            items = moreItems,
            navController = navController
        )
    }
}

@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    items: List<MoreNavigationItem>,
    navController: NavController
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items) { item ->
            MoreListItem(
                title = item.title,
                icon = item.icon,
                onClick = {
                    // Navegar a la ruta correspondiente.
                    // Estas rutas (News, TriviaSelection, Timeline) deben estar definidas
                    // en el NavHost que controla la navegación DESDE la barra inferior,
                    // es decir, el NavHost interno de MainScreen.
                    Log.d("MoreScreen", "Navegando a ${item.route} usando el NavController interno (bottomNavController)")
                    navController.navigate(item.route)
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun MoreListItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "Ir a $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
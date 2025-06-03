package com.example.appescapetocinema.ui.trivia

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// --- Pantalla de Selección de Trivia ---
val triviaCategories = listOf(
    "all" to "Todas las Categorías", // Identificador especial para todas
    "Directores" to "Directores",
    "Actores/Actrices" to "Actores/Actrices",
    "Años80" to "Años 80",
    "Años90" to "Años 90",
    "CienciaFicción" to "Ciencia Ficción",
    "PremiosOscar" to "Premios Oscar",
    "Animación" to "Animación"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriviaSelectionScreen(
    navControllerForBack: NavController,   // Este es bottomNavController (para el botón Atrás de ESTA pantalla)
    navControllerForGame: NavController    // Este es mainNavController (para ir a TriviaGameScreen)
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elige un Desafío", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d("TriviaSelection", "Botón Atrás presionado, usando navControllerForBack (bottomNavController)")
                        navControllerForBack.popBackStack() // Usa el NavController que te trajo aquí (desde MoreScreen)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors( containerColor = MaterialTheme.colorScheme.surfaceVariant )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Selecciona una Categoría:",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            triviaCategories.forEach { (categoryId, categoryName) ->
                Button(
                    onClick = {
                        // Navega a TriviaGame usando el navControllerForGame (mainNavController)
                        Log.d("TriviaSelection", "Navegando a TriviaGame con categoría $categoryId usando navControllerForGame (mainNavController)")
                        navControllerForGame.navigate(Screen.TriviaGame.createRoute(categoryId))
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(50.dp)
                ) {
                    Text(categoryName)
                }
            }
        }
    }
}
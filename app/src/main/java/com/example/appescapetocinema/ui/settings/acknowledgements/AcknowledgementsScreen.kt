package com.example.appescapetocinema.ui.settings.acknowledgements

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

data class AcknowledgementItem(
    val title: String,
    val text: String,
    val url: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcknowledgementsScreen(
    onNavigateBack: () -> Unit
) {
    val acknowledgements = listOf(
        AcknowledgementItem(
            title = "TMDb API",
            text = "Esta aplicación utiliza la API de TMDb pero no está respaldada ni certificada por TMDb.",
            url = "https://www.themoviedb.org"
        ),
        AcknowledgementItem(
            title = "MovieGlu API",
            text = "Datos de cines y carteleras proporcionados por MovieGlu.",
            url = "https://www.movieglu.com" // Verifica la URL correcta
        ),
        AcknowledgementItem(
            title = "Gemini API (Google AI Studio)",
            text = "El chatbot \"FOGLIGHT\" está potenciado por modelos de IA de Google a través de Gemini API."
        ),
        AcknowledgementItem(
            title = "Fuentes de Noticias RSS",
            text = "Sensacine, Mundiario (sección Cines y Series), EscribiendoCine."
        ),
        AcknowledgementItem(
            title = "Fuentes Tipográficas",
            text = "Orbitron y Rajdhani (Google Fonts)."
        ),
        AcknowledgementItem(
            title = "Librerías Principales",
            text = "- Jetpack Compose, Navigation, ViewModel, LiveData, Paging 3\n" +
                    "- Retrofit & OkHttp (para networking)\n" +
                    "- Coil (para carga de imágenes)\n" +
                    "- Firebase (Authentication, Firestore)\n" +
                    "- com.prof18.rssparser (para feeds RSS)\n" +
                    "- Kotlinx Serialization (para JSON)"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agradecimientos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(acknowledgements) { item ->
                AcknowledgementCard(item = item)
            }
        }
    }
}

@Composable
fun AcknowledgementCard(item: AcknowledgementItem) {
    val uriHandler = LocalUriHandler.current
    Column {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (item.url != null) {
            val annotatedString = buildAnnotatedString {
                append(item.text + " ")
                pushStringAnnotation(tag = "URL", annotation = item.url)
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.tertiary, textDecoration = TextDecoration.Underline)) {
                    append(item.url)
                }
                pop()
            }
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            try {
                                uriHandler.openUri(annotation.item)
                            } catch (e: Exception) {
                                Log.e("AcknowledgementsScreen", "Error al abrir URL: ${annotation.item}", e)
                            }
                        }
                }
            )
        } else {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
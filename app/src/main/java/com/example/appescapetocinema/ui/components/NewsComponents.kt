package com.example.appescapetocinema.ui.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler // Para abrir URLs
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appescapetocinema.R // Para tus placeholders
import com.example.appescapetocinema.model.NewsArticle // TU MODELO DE NOTICIA

@Composable
fun NewsArticleCard(
    article: NewsArticle,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Card(
        modifier = modifier
            .width(280.dp) // Ancho fijo para tarjetas en un LazyRow
            .height(IntrinsicSize.Max) // Altura se ajustará al contenido, pero no más de lo necesario
            .clickable {
                article.url?.let { url ->
                    try {
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        Log.e("NewsArticleCard", "No se pudo abrir la URL: $url", e)
                    }
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // DarkSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column {
            // Imagen (si existe)
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(article.imageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.logo)
                        .error(R.drawable.logo)
                        .build(),
                    contentDescription = article.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp) // Altura fija para la imagen
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)), // Redondear solo esquinas superiores
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                }
            }

            // Contenido de Texto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Título
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleSmall.copy( // Orbitron Medium 14sp
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface, // TextWhite
                    maxLines = 3, // Limitar líneas para el título
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Fuente y Fecha (si la fecha es útil y parseable)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.source.take(25) + if (article.source.length > 25) "..." else "", // Acortar nombre de fuente largo
                        style = MaterialTheme.typography.labelSmall.copy( // Orbitron Medium 11sp
                            // fontFamily = RajdhaniFontFamily
                        ),
                        color = MaterialTheme.colorScheme.primary, // NeonCyan
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


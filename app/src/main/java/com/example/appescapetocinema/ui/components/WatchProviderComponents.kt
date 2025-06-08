package com.example.appescapetocinema.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appescapetocinema.R
import com.example.appescapetocinema.network.dto.ProviderDto
import com.example.appescapetocinema.network.TmdbApiService
import com.valentinilk.shimmer.shimmer

@Composable
fun ProviderLogoItem(
    provider: ProviderDto,
    modifier: Modifier = Modifier,
    onProviderClick: () -> Unit = {} // Opcional si quieres hacer algo al clickear el logo
) {
    val fullLogoUrl = TmdbApiService.getPosterUrl(provider.logoPath) // Usar un tamaño pequeño para logos (w92, w154)

    Column(
        modifier = modifier
            .width(80.dp) // Ancho para cada item de proveedor
            .clickable(onClick = onProviderClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // Un poco más claro que surfaceVariant
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fullLogoUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.logo) // Un placeholder genérico para logos
                    .error(R.drawable.logo)       // O un icono de "imagen no disponible"
                    .build(),
                contentDescription = provider.providerName,
                modifier = Modifier
                    .size(64.dp) // Tamaño del logo
                    .padding(4.dp), // Pequeño padding dentro de la tarjeta del logo
                contentScale = ContentScale.Fit // Fit para que el logo se vea completo
            )
        }
        provider.providerName?.let { name ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), // Texto pequeño
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Placeholder para Shimmer de ProviderLogoItem
@Composable
fun ShimmerProviderLogoItem(modifier: Modifier = Modifier) {
    val shimmerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    Column(
        modifier = modifier
            .width(80.dp)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .shimmer(), // Asume que tienes .shimmer()
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerColor)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(10.dp)
                .fillMaxWidth(0.8f)
                .background(shimmerColor, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun WatchProviderCategorySection(
    title: String,
    providers: List<ProviderDto>?,
    countryLink: String?, // El link general para "ver más" en TMDb para ese país
    onCountryLinkClick: (String) -> Unit // Para manejar el clic en el link general
) {
    if (!providers.isNullOrEmpty()) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium, // Orbitron Medium 16sp
                    color = MaterialTheme.colorScheme.onSurface // TextWhite
                )
                // Enlace "Ver todos" si hay un link general para el país
                countryLink?.let { link ->
                    TextButton(onClick = { onCountryLinkClick(link) },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                        Text(
                            "Ver más",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary // NeonMagenta
                        )
                    }
                }
            }
            LazyRow(
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = providers.size, // Ahora pasamos el tamaño de la lista
                    key = { index -> providers[index].providerId } // Clave basada en el índice
                ) { index -> // Ahora 'index' es Int
                    val provider = providers[index] // Obtenemos el provider usando el índice
                    ProviderLogoItem(provider = provider)
                }
            }
        }
    }
}
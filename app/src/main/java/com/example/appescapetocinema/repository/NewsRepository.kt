package com.example.appescapetocinema.repository

import android.text.Html // Para limpiar HTML si es necesario en descripciones
import android.util.Log
import com.example.appescapetocinema.model.NewsArticle // Tu modelo de datos
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder // Si necesitas configurar el parser
import com.prof18.rssparser.model.RssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.* // Para Date, Locale, TimeZone
import javax.inject.Inject // Si usas Hilt
import javax.inject.Singleton // Si usas Hilt

interface NewsRepository {
    suspend fun getLatestNews(): Result<List<NewsArticle>>

    suspend fun getMovieRelatedNews(
        movieTitle: String,
        directorName: String?,
        actorName: String?
    ): Result<List<NewsArticle>>
}

@Singleton
class NewsRepositoryImpl @Inject constructor(
) : NewsRepository {

    // Lista de URLs de Feeds RSS
    private val cinemaRssFeedUrls = listOf(
        "https://www.sensacine.com/rss/noticias-cine.xml", // La que ya tenías
        "https://www.mundiario.com/cineseries/rss/cine-series/",
        "https://www.escribiendocine.com/rss/criticas",
    )

    // Instancia del Parser RSS.
    private val rssParser: RssParser by lazy {
        RssParserBuilder(
        ).build()
    }

    // Formateadores de Fecha (tus formatos existentes + GMT)
    private val rssDateFormatters = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),    // RFC 822
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),    // RFC 822 con nombre de zona
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),       // ISO 8601 UTC
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),     // ISO 8601 con offset
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH), // ISO 8601 con milisegundos
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH)  // ISO 8601 con milisegundos y offset
    ).onEach { it.timeZone = TimeZone.getTimeZone("GMT") } // Importante para consistencia si los feeds no especifican bien el offset


    override suspend fun getLatestNews(): Result<List<NewsArticle>> {
        Log.d("NewsRepositoryImpl", "getLatestNews: Obteniendo noticias generales.")
        return withContext(Dispatchers.IO) {
            try {
                val allFetchedRssItems = mutableListOf<RssItem>()
                coroutineScope {
                    val deferredFeeds = cinemaRssFeedUrls.map { url ->
                        async {
                            try {
                                val channel = rssParser.getRssChannel(url)
                                channel.items
                            } catch (e: Exception) {
                                Log.e("NewsRepositoryImpl", "Error obteniendo feed $url para getLatestNews", e)
                                emptyList<RssItem>()
                            }
                        }
                    }
                    deferredFeeds.awaitAll().forEach { items ->
                        allFetchedRssItems.addAll(items)
                    }
                }

                if (allFetchedRssItems.isEmpty()) {
                    Log.w("NewsRepositoryImpl", "getLatestNews: No se obtuvieron items de RSS.")
                    return@withContext Result.success(emptyList())
                }

                val articles = allFetchedRssItems
                    .mapNotNull { rssItem ->
                        val channelTitleFallback = try { URI(rssItem.link ?: "").host } catch (_: Exception) { "Fuente Desconocida" }
                        mapRssItemToNewsArticle(rssItem, channelTitleFallback)
                    }
                    .distinctBy { it.url } // Evitar duplicados
                    .sortedByDescending { it.publishedDate } // Ordenar por las más recientes
                    .take(30) // Tomar un número razonable para una lista general (ej. 30)

                Log.d("NewsRepositoryImpl", "getLatestNews: Éxito. Obtenidos ${articles.size} artículos.")
                Result.success(articles)

            } catch (e: Exception) {
                Log.e("NewsRepositoryImpl", "getLatestNews: Error general", e)
                Result.failure(Exception("Error al obtener últimas noticias: ${e.localizedMessage}", e))
            }
        }
    }

    override suspend fun getMovieRelatedNews(
        movieTitle: String,
        directorName: String?,
        actorName: String?
    ): Result<List<NewsArticle>> {
        Log.d("NewsRepositoryImpl", "getMovieRelatedNews para: Title='$movieTitle', Director='$directorName', Actor='$actorName'")
        return withContext(Dispatchers.IO) {
            try {
                val allFetchedRssItems = mutableListOf<RssItem>()

                // Usar corrutinas para obtener feeds en paralelo
                coroutineScope {
                    val deferredFeeds = cinemaRssFeedUrls.map { url ->
                        async { // Lanza cada obtención de feed en una corrutina separada
                            try {
                                Log.d("NewsRepositoryImpl", "Consultando feed: $url")
                                val channel = rssParser.getRssChannel(url)
                                Log.d("NewsRepositoryImpl", "Feed $url: ${channel.items.size} items.")
                                channel.items
                            } catch (e: Exception) {
                                Log.e("NewsRepositoryImpl", "Error obteniendo o parseando RSS de $url", e)
                                emptyList<RssItem>() // Devuelve lista vacía en caso de error para este feed
                            }
                        }
                    }
                    deferredFeeds.awaitAll().forEach { items ->
                        allFetchedRssItems.addAll(items)
                    }
                }


                if (allFetchedRssItems.isEmpty()) {
                    Log.w("NewsRepositoryImpl", "No se obtuvieron items de ningún feed RSS.")
                    return@withContext Result.success(emptyList())
                }

                Log.d("NewsRepositoryImpl", "Total items RSS antes de filtrar: ${allFetchedRssItems.size}")

                val keywords = listOfNotNull(
                    movieTitle.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault()),
                    directorName?.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault()),
                    actorName?.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault())
                ).filter { it.length > 2 } // Palabras clave significativas

                if (keywords.isEmpty()) {
                    Log.w("NewsRepositoryImpl", "No hay palabras clave para filtrar (título, director, actor).")
                    // Decide si devolver todo o nada. Para noticias *relacionadas*, es mejor nada.
                    return@withContext Result.success(emptyList())
                }
                Log.d("NewsRepositoryImpl", "Filtrando con palabras clave: $keywords")

                val relevantArticles = allFetchedRssItems
                    .mapNotNull { rssItem -> // Mapear y filtrar preliminarmente
                        // Usar el título del canal como fallback para el nombre de la fuente
                        val channelTitleFallback = try { URI(rssItem.link ?: "").host } catch (_: Exception) { "Fuente Desconocida" }
                        mapRssItemToNewsArticle(rssItem, channelTitleFallback)
                    }
                    .filter { newsArticle -> // Segundo paso de filtrado por relevancia
                        var isRelevant = false
                        val articleTitleLower = newsArticle.title.lowercase(Locale.getDefault())
                        val articleDescLower = newsArticle.description?.lowercase(Locale.getDefault()) ?: ""

                        for (keyword in keywords) {
                            if (articleTitleLower.contains(keyword) || articleDescLower.contains(keyword)) {
                                isRelevant = true
                                break
                            }
                        }
                        isRelevant
                    }
                    .distinctBy { it.url } // Evitar duplicados por URL
                    .sortedByDescending { it.publishedDate } // Ordenar por fecha (las más recientes primero)
                    .take(5) // Tomar las 5 más relevantes/recientes

                Log.d("NewsRepositoryImpl", "Noticias RSS relevantes filtradas: ${relevantArticles.size}")
                Result.success(relevantArticles)

            } catch (e: Exception) {
                Log.e("NewsRepositoryImpl", "Excepción general en getMovieRelatedNews (RSS)", e)
                Result.failure(Exception("Error al obtener noticias relacionadas: ${e.localizedMessage}", e))
            }
        }
    }

    // Tu función mapRssItemToNewsArticle (con pequeñas adaptaciones)
    private fun mapRssItemToNewsArticle(rssItem: RssItem, channelTitleFallback: String?): NewsArticle? {
        val id = rssItem.guid ?: rssItem.link ?: run {
            Log.w("NewsRepositoryImpl", "Artículo RSS sin guid ni link, ignorando: ${rssItem.title?.take(30)}")
            return null
        }
        val url = rssItem.link ?: return null
        val title = rssItem.title?.trim() ?: "Sin Título"

        // Intentar obtener un nombre de fuente más limpio
        val sourceName = rssItem.sourceName?.trim()?.takeIf { it.isNotEmpty() }
            ?: rssItem.author?.trim()?.takeIf { it.isNotEmpty() && it.length < 30 && !it.contains("@")} // Evitar emails como autor
            ?: channelTitleFallback?.trim()?.replaceFirst("www.", "")?.substringBefore('/') // Hostname o título del canal
            ?: "Fuente Desconocida"

        // Limpiar HTML de la descripción
        val descriptionText = rssItem.description?.trim()
            ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
            ?.replace(Regex("\\s{2,}"), " ") // Reemplazar múltiples espacios con uno solo

        val imageUrl = rssItem.image?.trim()?.takeIf { it.isNotEmpty() }
        val publishedDate = parseRssDate(rssItem.pubDate?.trim())

        return NewsArticle(
            id = id,
            title = title,
            source = sourceName,
            description = descriptionText,
            url = url,
            imageUrl = imageUrl,
            publishedDate = publishedDate
        )
    }

    private fun parseRssDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        for (formatter in rssDateFormatters) {
            try { return formatter.parse(dateString) } catch (_: Exception) { }
        }
        Log.w("NewsRepositoryImpl", "No se pudo parsear la fecha RSS: $dateString con formatos: ${rssDateFormatters.joinToString { it.toPattern() }}")
        return null
    }
}
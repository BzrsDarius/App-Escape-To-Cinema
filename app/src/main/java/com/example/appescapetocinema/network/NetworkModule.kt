package com.example.appescapetocinema.network

import com.example.appescapetocinema.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // --- Cliente OkHttp y Json Parser Compartidos (o puedes crear específicos) ---
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false // No para producción
        coerceInputValues = true // Intenta convertir tipos si es posible
        encodeDefaults = true  // Incluye valores por defecto al serializar (no tan relevante para deserializar)
        explicitNulls = false // Si un campo nullable no está, se trata como si tuviera el valor por defecto (null)
    }

    private fun createBaseOkHttpClientBuilder(): OkHttpClient.Builder {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // Timeout de conexión
            .readTimeout(30, TimeUnit.SECONDS)    // Timeout de lectura
            .writeTimeout(30, TimeUnit.SECONDS)   // Timeout de escritura
    }
    private const val YOUR_API_BASE_URL = "https://escape-to-cinema-api-750534913480.europe-southwest1.run.app/" // Para emulador Android
    private val yourApiOkHttpClient: OkHttpClient = createBaseOkHttpClientBuilder().build() // Renombrado para claridad


    // --- Cliente y Servicio para TMDb API ---
    private val tmdbOkHttpClient: OkHttpClient = createBaseOkHttpClientBuilder()
        .build()

    private val yourApiRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(YOUR_API_BASE_URL) // Tu URL de Cloud Run
        .client(yourApiOkHttpClient) // Tu OkHttpClient
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(jsonParser.asConverterFactory("application/json".toMediaType())) // LUEGO tu convertidor JSON
        .build()

    val tmdbApiService: TmdbApiService = yourApiRetrofit.create(TmdbApiService::class.java)


    // --- Cliente y Servicio para MovieGlu API ---

    private val movieGluOkHttpClient: OkHttpClient = createBaseOkHttpClientBuilder().build()

    private val movieGluRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(MovieGluApiService.BASE_URL)
        .client(movieGluOkHttpClient)
        .addConverterFactory(jsonParser.asConverterFactory("application/json".toMediaType()))
        .build()

    val movieGluApiService: MovieGluApiService = movieGluRetrofit.create(MovieGluApiService::class.java)

}
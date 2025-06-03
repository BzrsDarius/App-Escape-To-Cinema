package com.example.appescapetocinema.model

class SortOptions {
}
val sortOptions = listOf(
    "popularity.desc" to "Popularidad (Desc.)",
    "popularity.asc" to "Popularidad (Asc.)",
    "release_date.desc" to "Fecha Estreno (Desc.)",
    "release_date.asc" to "Fecha Estreno (Asc.)",
    "vote_average.desc" to "Valoración (Desc.)",
    "vote_average.asc" to "Valoración (Asc.)",
    "original_title.asc" to "Título (A-Z)",
    "original_title.desc" to "Título (Z-A)"
)

// Valor por defecto
const val DEFAULT_SORT_BY = "popularity.desc"
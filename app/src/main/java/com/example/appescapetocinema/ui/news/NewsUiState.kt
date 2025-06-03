package com.example.appescapetocinema.ui.news

import com.example.appescapetocinema.model.NewsArticle

data class NewsUiState(
    val articles: List<NewsArticle> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
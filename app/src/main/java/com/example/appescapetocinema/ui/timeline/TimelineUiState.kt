package com.example.appescapetocinema.ui.timeline

import com.example.appescapetocinema.model.TimelineEvent

data class TimelineUiState(
    val isLoading: Boolean = true,
    val events: List<TimelineEvent> = emptyList(),
    val errorMessage: String? = null
)
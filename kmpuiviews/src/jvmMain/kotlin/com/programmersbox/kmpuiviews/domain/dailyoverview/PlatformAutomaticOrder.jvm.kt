package com.programmersbox.kmpuiviews.domain.dailyoverview

// An imported local model runs first; Ollama only when there is none
actual val platformAutomaticOrder: List<SummaryEngineId> = listOf(SummaryEngineId.Gemma, SummaryEngineId.Ollama)

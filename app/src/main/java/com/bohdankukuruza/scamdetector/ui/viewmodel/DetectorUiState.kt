package com.bohdankukuruza.scamdetector.ui.viewmodel

import com.bohdankukuruza.scamdetector.data.model.AnalysisResult

/**
 * Single immutable snapshot of everything the detector screen needs to
 * render. Exposed by [DetectorViewModel] as a StateFlow.
 */
data class DetectorUiState(
    val inputText: String = "",
    val isAnalyzing: Boolean = false,
    val result: AnalysisResult? = null,
    val error: String? = null
) {
    val hasResult: Boolean get() = result != null
    val canAnalyze: Boolean get() = inputText.isNotBlank() && !isAnalyzing
}
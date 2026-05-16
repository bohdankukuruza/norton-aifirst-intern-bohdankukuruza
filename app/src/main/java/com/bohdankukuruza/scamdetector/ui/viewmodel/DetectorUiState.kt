package com.bohdankukuruza.scamdetector.ui.viewmodel

import com.bohdankukuruza.scamdetector.data.model.AnalysisResult

/**
 * Single immutable snapshot of everything the detector screen needs to
 * render. Exposed by [DetectorViewModel] as a StateFlow.
 *
 * Modelled as a sealed-style data class rather than a sealed class
 * because the screen always shows the input field — only the result
 * area changes between idle / analyzing / showing-result. Keeping it
 * a single data class lets the UI use `copy()` semantics cleanly.
 */
data class DetectorUiState(
    val inputText: String = "",
    val isAnalyzing: Boolean = false,
    val result: AnalysisResult? = null
) {
    val hasResult: Boolean get() = result != null
    val canAnalyze: Boolean get() = inputText.isNotBlank() && !isAnalyzing
}
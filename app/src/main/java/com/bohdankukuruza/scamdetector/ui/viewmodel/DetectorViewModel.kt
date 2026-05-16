package com.bohdankukuruza.scamdetector.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bohdankukuruza.scamdetector.domain.analyzer.HeuristicAnalyzer
import com.bohdankukuruza.scamdetector.domain.analyzer.ScamAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the scam detector screen.
 *
 * Owns the [DetectorUiState] and delegates the actual analysis to an
 * injected [ScamAnalyzer]. The analyzer is injected (not constructed
 * inside the ViewModel) so tests can substitute a fake — keeping the
 * ViewModel's tests focused on state transitions rather than rule
 * behaviour.
 *
 * Errors from the analyzer are caught and surfaced via
 * [DetectorUiState.error] rather than crashing the screen or leaving
 * `isAnalyzing` stuck at true. The current heuristic implementation
 * does not throw, but the [ScamAnalyzer] interface allows future
 * network-backed analyzers that can.
 */
class DetectorViewModel(
    private val analyzer: ScamAnalyzer = HeuristicAnalyzer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectorUiState())
    val uiState: StateFlow<DetectorUiState> = _uiState.asStateFlow()

    /** Called whenever the user edits the input field. */
    fun onInputChanged(newText: String) {
        _uiState.update { current ->
            // Any input change invalidates the previous result and clears
            // any error from a prior failed analysis.
            current.copy(inputText = newText, result = null, error = null)
        }
    }

    /** Called when the user taps a sample chip to auto-fill the input. */
    fun onSampleSelected(text: String) {
        _uiState.update { it.copy(inputText = text, result = null, error = null) }
    }

    /** Called when the user taps the Analyze button. */
    fun onAnalyzeClicked() {
        val current = _uiState.value
        if (!current.canAnalyze) return

        _uiState.update { it.copy(isAnalyzing = true, result = null, error = null) }

        viewModelScope.launch {
            try {
                val analysis = analyzer.analyze(current.inputText)
                _uiState.update {
                    it.copy(isAnalyzing = false, result = analysis, error = null)
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        result = null,
                        error = "Couldn't analyze the message: ${t.message ?: "unknown error"}"
                    )
                }
            }
        }
    }

    /** Resets the screen to its initial state. */
    fun onReset() {
        _uiState.update { DetectorUiState() }
    }
}
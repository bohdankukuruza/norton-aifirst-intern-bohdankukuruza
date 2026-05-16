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
 */
class DetectorViewModel(
    private val analyzer: ScamAnalyzer = HeuristicAnalyzer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectorUiState())
    val uiState: StateFlow<DetectorUiState> = _uiState.asStateFlow()

    /** Called whenever the user edits the input field. */
    fun onInputChanged(newText: String) {
        _uiState.update { current ->
            current.copy(
                inputText = newText,
                // Clear stale result as soon as the input changes so the
                // user is never looking at a verdict that no longer
                // matches what's in the field.
                result = if (newText != current.inputText) null else current.result
            )
        }
    }

    /** Called when the user taps a sample chip to auto-fill the input. */
    fun onSampleSelected(text: String) {
        _uiState.update { it.copy(inputText = text, result = null) }
    }

    /** Called when the user taps the Analyze button. */
    fun onAnalyzeClicked() {
        val current = _uiState.value
        if (!current.canAnalyze) return

        _uiState.update { it.copy(isAnalyzing = true, result = null) }

        viewModelScope.launch {
            val analysis = analyzer.analyze(current.inputText)
            _uiState.update {
                it.copy(isAnalyzing = false, result = analysis)
            }
        }
    }

    /** Resets the screen to its initial state. */
    fun onReset() {
        _uiState.value = DetectorUiState()
    }
}
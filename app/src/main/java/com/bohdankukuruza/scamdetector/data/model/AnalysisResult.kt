package com.bohdankukuruza.scamdetector.data.model

/**
 * The complete outcome of analyzing a single message.
 *
 * Returned by any [com.bohdankukuruza.scamdetector.domain.analyzer.ScamAnalyzer]
 * implementation (heuristic, AI-backed, or hybrid). Designed to be the single
 * source of truth consumed by the UI layer — the ViewModel should not need
 * any additional context to render the result.
 *
 * @property riskLevel Overall verdict for the message.
 * @property confidence How confident the analyzer is in [riskLevel], on a
 *   0.0–1.0 scale. A SAFE result with confidence 0.6 means "probably safe,
 *   but not certain"; a DANGEROUS result with confidence 0.95 means
 *   "almost certainly a scam".
 * @property signals All signals that contributed to this result, in the
 *   order they were detected. May be empty for SAFE results.
 * @property analyzedAt Wall-clock timestamp (epoch millis) when the
 *   analysis was performed. Used for display ("analyzed 2s ago") and
 *   for ordering historical results if we add history later.
 */
data class AnalysisResult(
    val riskLevel: RiskLevel,
    val confidence: Float,
    val signals: List<DetectionSignal>,
    val analyzedAt: Long
) {
    init {
        require(confidence in 0f..1f) {
            "AnalysisResult confidence must be in 0.0..1.0 but was $confidence"
        }
        require(analyzedAt > 0) {
            "AnalysisResult analyzedAt must be a positive epoch millis value"
        }
    }

    /**
     * Convenience factory for a clean SAFE result with no signals.
     * Useful for empty-input cases and as a default in tests.
     */
    companion object {
        fun safe(analyzedAt: Long): AnalysisResult = AnalysisResult(
            riskLevel = RiskLevel.SAFE,
            confidence = 1.0f,
            signals = emptyList(),
            analyzedAt = analyzedAt
        )
    }
}
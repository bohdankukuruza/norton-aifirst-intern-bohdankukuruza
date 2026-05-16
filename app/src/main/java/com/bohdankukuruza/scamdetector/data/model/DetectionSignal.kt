package com.bohdankukuruza.scamdetector.data.model

/**
 * A single piece of evidence produced by a [DetectionRule] when it
 * fires against a message.
 *
 * Signals are the building blocks of an [AnalysisResult]: the analyzer
 * collects every signal raised by its rules, then aggregates their
 * weights into an overall risk level and confidence score.
 *
 * @property ruleName Stable identifier of the rule that produced the signal
 *   (e.g. "url_shortener"). Used for logging, testing, and analytics.
 * @property weight How strongly this signal contributes to overall risk,
 *   on a 1–10 scale. 1 = weak hint, 10 = near-certain scam indicator.
 * @property explanation Short human-readable reason shown to the user
 *   (e.g. "Link uses a URL shortener (bit.ly)").
 */
data class DetectionSignal(
    val ruleName: String,
    val weight: Int,
    val explanation: String
) {
    init {
        require(weight in 1..10) {
            "DetectionSignal weight must be in 1..10 but was $weight (rule=$ruleName)"
        }
        require(ruleName.isNotBlank()) {
            "DetectionSignal ruleName must not be blank"
        }
        require(explanation.isNotBlank()) {
            "DetectionSignal explanation must not be blank"
        }
    }
}
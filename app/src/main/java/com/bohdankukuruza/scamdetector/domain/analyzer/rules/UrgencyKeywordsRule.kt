package com.bohdankukuruza.scamdetector.domain.analyzer.rules

import com.bohdankukuruza.scamdetector.data.model.DetectionSignal

/**
 * Detects manipulative urgency language commonly used in scam messages
 * to bypass careful reading.
 *
 * Scams routinely use phrases like "verify now", "account suspended",
 * "act immediately" to push the recipient into a panic response.
 * Legitimate transactional messages from banks, couriers, or platforms
 * almost never demand immediate action through such phrasing — they
 * point to a calmer in-app or web flow.
 *
 * The rule's weight scales with the *count* of distinct urgency
 * triggers found: a single occurrence is a weak hint, three or more
 * is a strong indicator.
 */
class UrgencyKeywordsRule : DetectionRule {

    override val name: String = "urgency_keywords"

    /**
     * Urgency phrases we look for. Lowercased; matched as substrings
     * against a lowercased input.
     *
     * Phrases (not single words) are preferred where possible to keep
     * false positives down — "verify your account" is a strong scam
     * cue, while bare "verify" appears in many legitimate contexts.
     */
    private val urgencyPhrases: Set<String> = setOf(
        "verify your account",
        "verify now",
        "account suspended",
        "account locked",
        "act immediately",
        "act now",
        "urgent action required",
        "immediate action",
        "click here now",
        "confirm your identity",
        "unusual activity",
        "limited time",
        "expires today",
        "final notice",
        "last warning"
    )

    override fun evaluate(text: String): DetectionSignal? {
        val normalized = text.lowercase()
        val matches = urgencyPhrases.filter { phrase ->
            normalized.contains(phrase)
        }

        if (matches.isEmpty()) return null

        val weight = when (matches.size) {
            1 -> 3
            2 -> 5
            else -> 7
        }

        val explanation = buildExplanation(matches)

        return DetectionSignal(
            ruleName = name,
            weight = weight,
            explanation = explanation
        )
    }

    /**
     * Builds a human-readable explanation listing the urgency phrases
     * that fired, capped at the first three to keep the UI readable.
     */
    private fun buildExplanation(matches: List<String>): String {
        val shown = matches.take(3).joinToString(separator = ", ") { "\"$it\"" }
        val suffix = if (matches.size > 3) ", and ${matches.size - 3} more" else ""
        return "Message uses urgency language: $shown$suffix."
    }
}
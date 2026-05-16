package com.bohdankukuruza.scamdetector.domain.analyzer

import com.bohdankukuruza.scamdetector.data.model.AnalysisResult
import com.bohdankukuruza.scamdetector.data.model.DetectionSignal
import com.bohdankukuruza.scamdetector.data.model.RiskLevel
import com.bohdankukuruza.scamdetector.domain.analyzer.rules.BrandImpersonationRule
import com.bohdankukuruza.scamdetector.domain.analyzer.rules.DetectionRule
import com.bohdankukuruza.scamdetector.domain.analyzer.rules.ExcessiveCapsRule
import com.bohdankukuruza.scamdetector.domain.analyzer.rules.SuspiciousDomainRule
import com.bohdankukuruza.scamdetector.domain.analyzer.rules.UrgencyKeywordsRule
import com.bohdankukuruza.scamdetector.domain.analyzer.rules.UrlShortenerRule

/**
 * Local, offline implementation of [ScamAnalyzer] that runs a fixed
 * set of [DetectionRule]s against the input and aggregates their
 * signals into an overall verdict.
 *
 * Aggregation strategy:
 *  1. Each rule contributes at most one [DetectionSignal] with a
 *     weight in 1..10.
 *  2. The total weight is mapped to a [RiskLevel] via fixed thresholds.
 *  3. Confidence is derived from the total weight normalised against
 *     a soft cap, then clamped to 0.0..1.0.
 *
 * The rule list is injected via the constructor so tests can supply a
 * minimal or fake rule set. Production code uses the no-arg secondary
 * constructor which wires the default rules.
 */
class HeuristicAnalyzer(
    private val rules: List<DetectionRule>,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ScamAnalyzer {

    /** Convenience constructor used by production code. */
    constructor() : this(rules = defaultRules())

    override suspend fun analyze(text: String): AnalysisResult {
        if (text.isBlank()) {
            return AnalysisResult.safe(analyzedAt = clock())
        }

        val signals: List<DetectionSignal> = rules.mapNotNull { rule ->
            rule.evaluate(text)
        }

        val totalWeight = signals.sumOf { it.weight }
        val riskLevel = riskLevelFor(totalWeight)
        val confidence = confidenceFor(totalWeight)

        return AnalysisResult(
            riskLevel = riskLevel,
            confidence = confidence,
            signals = signals,
            analyzedAt = clock()
        )
    }

    private fun riskLevelFor(totalWeight: Int): RiskLevel = when {
        totalWeight >= 12 -> RiskLevel.DANGEROUS
        totalWeight >= 5 -> RiskLevel.SUSPICIOUS
        else -> RiskLevel.SAFE
    }

    /**
     * Maps total signal weight onto a 0.0..1.0 confidence using a soft
     * cap of 20 — well above the threshold for DANGEROUS, so a result
     * with multiple strong signals reaches near-certain confidence
     * without ever exceeding the contract bound.
     */
    private fun confidenceFor(totalWeight: Int): Float {
        if (totalWeight == 0) return 1.0f
        val raw = totalWeight / 20f
        return raw.coerceIn(0f, 1f)
    }

    companion object {
        fun defaultRules(): List<DetectionRule> = listOf(
            UrlShortenerRule(),
            UrgencyKeywordsRule(),
            SuspiciousDomainRule(),
            BrandImpersonationRule(),
            ExcessiveCapsRule()
        )
    }
}
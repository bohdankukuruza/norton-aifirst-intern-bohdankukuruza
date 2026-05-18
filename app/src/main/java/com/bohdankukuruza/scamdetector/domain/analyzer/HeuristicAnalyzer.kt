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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local, offline implementation of [ScamAnalyzer] that runs a fixed
 * set of [DetectionRule]s against the input and aggregates their
 * signals into an overall verdict.
 *
 * Aggregation strategy:
 *  1. Each rule contributes at most one [DetectionSignal] with a
 *     weight in 1..10.
 *  2. The total weight is mapped to a [RiskLevel] via fixed thresholds.
 *  3. Confidence is derived from how far the total weight is from the
 *     nearest threshold boundary — clean SAFE / clean DANGEROUS verdicts
 *     produce high confidence; verdicts close to a boundary produce
 *     lower confidence, reflecting genuine ambiguity.
 *
 * Rule evaluation is dispatched to [Dispatchers.Default] so that long
 * pasted text running through five regex-based rules never blocks the
 * Main thread that the ViewModel calls us from. The dispatcher is
 * injectable so tests can use the test dispatcher.
 *
 * The rule list is injected via the constructor so tests can supply a
 * minimal or fake rule set. Production code uses the no-arg secondary
 * constructor which wires the default rules.
 */
class HeuristicAnalyzer(
    private val rules: List<DetectionRule>,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ScamAnalyzer {

    /** Convenience constructor used by production code. */
    constructor() : this(rules = defaultRules())

    override suspend fun analyze(text: String): AnalysisResult = withContext(dispatcher) {
        if (text.isBlank()) {
            return@withContext AnalysisResult.safe(analyzedAt = clock())
        }

        val signals: List<DetectionSignal> = rules.mapNotNull { rule ->
            rule.evaluate(text)
        }




        val totalWeight = signals.sumOf { it.weight }
        val riskLevel = riskLevelFor(totalWeight)
        val confidence = confidenceFor(totalWeight, riskLevel)
        println("DEBUG: totalWeight=$totalWeight, riskLevel=$riskLevel, confidence=$confidence")

        AnalysisResult(
            riskLevel = riskLevel,
            confidence = confidence,
            signals = signals,
            analyzedAt = clock()
        )
    }

    private fun riskLevelFor(totalWeight: Int): RiskLevel = when {
        totalWeight >= DANGEROUS_THRESHOLD -> RiskLevel.DANGEROUS
        totalWeight >= SUSPICIOUS_THRESHOLD -> RiskLevel.SUSPICIOUS
        else -> RiskLevel.SAFE
    }

    /**
     * Maps total signal weight onto a 0.0..1.0 confidence.
     *
     * Confidence reflects distance from the nearest threshold boundary,
     * not raw signal strength: a borderline result (weight 5 → SUSPICIOUS,
     * or weight 11 → SUSPICIOUS just under DANGEROUS) honestly reports
     * lower confidence than a clear-cut verdict.
     *
     * - SAFE with 0 signals → 0.95 (high but not perfect: absence of
     *   evidence is not evidence of absence)
     * - SAFE just below threshold → ~0.55
     * - DANGEROUS at threshold → ~0.60, scaling up with more signals,
     *   capped at 0.98
     */
    private fun confidenceFor(totalWeight: Int, riskLevel: RiskLevel): Float {
        return when (riskLevel) {
            RiskLevel.SAFE -> {
                // 0 signals → 0.95; weight just under 5 → ~0.55
                val distance = (SUSPICIOUS_THRESHOLD - totalWeight).coerceAtLeast(0)
                (0.55f + (distance / SUSPICIOUS_THRESHOLD.toFloat()) * 0.40f)
                    .coerceIn(0.55f, 0.95f)
            }
            RiskLevel.SUSPICIOUS -> {
                // mid-range — confidence peaks in the middle of the band
                val band = (DANGEROUS_THRESHOLD - SUSPICIOUS_THRESHOLD).toFloat()
                val depth = (totalWeight - SUSPICIOUS_THRESHOLD) / band
                (0.55f + depth * 0.25f).coerceIn(0.55f, 0.80f)
            }
            RiskLevel.DANGEROUS -> {
                // Scales from 0.60 at threshold to 0.98 at high weights
                val excess = (totalWeight - DANGEROUS_THRESHOLD).coerceAtLeast(0)
                (0.60f + (excess / 10f) * 0.38f).coerceIn(0.60f, 0.98f)
            }
        }
    }

    companion object {
        private const val SUSPICIOUS_THRESHOLD = 5
        private const val DANGEROUS_THRESHOLD = 12

        fun defaultRules(): List<DetectionRule> = listOf(
            UrlShortenerRule(),
            UrgencyKeywordsRule(),
            SuspiciousDomainRule(),
            BrandImpersonationRule(),
            ExcessiveCapsRule()
        )
    }
}
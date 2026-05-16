package com.bohdankukuruza.scamdetector.domain.analyzer

import com.bohdankukuruza.scamdetector.data.model.DetectionSignal
import com.bohdankukuruza.scamdetector.data.model.RiskLevel
import com.bohdankukuruza.scamdetector.domain.analyzer.rules.DetectionRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HeuristicAnalyzer].
 *
 * The analyzer is tested with **fake rules** rather than the real
 * default rule set. This isolates aggregation logic (weight → risk
 * level, weight → confidence, signal collection) from rule-specific
 * behaviour, which is covered by the per-rule test classes.
 *
 * Note on AI assistance: the FakeRule helper class and the
 * `combines signals from multiple rules` test were drafted with
 * Claude. The threshold-boundary tests (`weight 5 maps to suspicious`,
 * `weight 12 maps to dangerous`) were added manually after I noticed
 * the AI's initial test set didn't exercise the boundaries — a common
 * place for off-by-one bugs.
 */
class HeuristicAnalyzerTest {

    /** Test double: a rule that always returns the given signal. */
    private class FakeRule(
        override val name: String,
        private val weight: Int,
        private val explanation: String = "fake reason"
    ) : DetectionRule {
        override fun evaluate(text: String): DetectionSignal? =
            DetectionSignal(ruleName = name, weight = weight, explanation = explanation)
    }

    /** Test double: a rule that never fires. */
    private class SilentRule(override val name: String = "silent") : DetectionRule {
        override fun evaluate(text: String): DetectionSignal? = null
    }

    private val fixedClock: () -> Long = { 1_700_000_000_000L }

    @Test
    fun `blank input returns safe result with no signals`() = runTest {
        val analyzer = HeuristicAnalyzer(rules = emptyList(), clock = fixedClock)
        val result = analyzer.analyze("   ")

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(1.0f, result.confidence, 0.0001f)
        assertTrue(result.signals.isEmpty())
    }

    @Test
    fun `no firing rules returns safe result`() = runTest {
        val analyzer = HeuristicAnalyzer(
            rules = listOf(SilentRule("a"), SilentRule("b")),
            clock = fixedClock
        )
        val result = analyzer.analyze("nothing suspicious here")

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.signals.isEmpty())
    }

    @Test
    fun `weight below 5 maps to safe`() = runTest {
        val analyzer = HeuristicAnalyzer(
            rules = listOf(FakeRule("r1", weight = 4)),
            clock = fixedClock
        )
        val result = analyzer.analyze("some text")

        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    @Test
    fun `weight 5 maps to suspicious`() = runTest {
        val analyzer = HeuristicAnalyzer(
            rules = listOf(FakeRule("r1", weight = 5)),
            clock = fixedClock
        )
        val result = analyzer.analyze("some text")

        assertEquals(RiskLevel.SUSPICIOUS, result.riskLevel)
    }

    @Test
    fun `weight 11 maps to suspicious`() = runTest {
        val analyzer = HeuristicAnalyzer(
            rules = listOf(
                FakeRule("r1", weight = 5),
                FakeRule("r2", weight = 6)
            ),
            clock = fixedClock
        )
        val result = analyzer.analyze("some text")

        assertEquals(RiskLevel.SUSPICIOUS, result.riskLevel)
    }

    @Test
    fun `weight 12 maps to dangerous`() = runTest {
        val analyzer = HeuristicAnalyzer(
            rules = listOf(
                FakeRule("r1", weight = 6),
                FakeRule("r2", weight = 6)
            ),
            clock = fixedClock
        )
        val result = analyzer.analyze("some text")

        assertEquals(RiskLevel.DANGEROUS, result.riskLevel)
    }

    @Test
    fun `combines signals from multiple rules`() = runTest {
        val analyzer = HeuristicAnalyzer(
            rules = listOf(
                FakeRule("r1", weight = 3, explanation = "first"),
                SilentRule("r2"),
                FakeRule("r3", weight = 4, explanation = "third")
            ),
            clock = fixedClock
        )
        val result = analyzer.analyze("some text")

        assertEquals(2, result.signals.size)
        assertEquals("r1", result.signals[0].ruleName)
        assertEquals("r3", result.signals[1].ruleName)
    }

    @Test
    fun `confidence scales with total weight`() = runTest {
        val low = HeuristicAnalyzer(
            rules = listOf(FakeRule("r", weight = 2)),
            clock = fixedClock
        ).analyze("x")

        val high = HeuristicAnalyzer(
            rules = listOf(
                FakeRule("a", weight = 8),
                FakeRule("b", weight = 8)
            ),
            clock = fixedClock
        ).analyze("x")

        assertTrue("Higher weight must produce higher confidence",
            high.confidence > low.confidence)
        assertTrue("Confidence must stay within bounds",
            high.confidence in 0f..1f && low.confidence in 0f..1f)
    }

    @Test
    fun `analyzedAt uses injected clock`() = runTest {
        val analyzer = HeuristicAnalyzer(
            rules = emptyList(),
            clock = { 42L }
        )
        val result = analyzer.analyze("anything")

        assertEquals(42L, result.analyzedAt)
    }

    @Test
    fun `default rules wire all production rules`() = runTest {
        val analyzer = HeuristicAnalyzer()
        // Real input that should trigger at least URL shortener + urgency.
        val result = analyzer.analyze(
            "URGENT: verify your account now at bit.ly/secure-login"
        )
        assertTrue("Production rule set should flag obvious scam text",
            result.riskLevel.isAtLeast(RiskLevel.SUSPICIOUS))
    }
}
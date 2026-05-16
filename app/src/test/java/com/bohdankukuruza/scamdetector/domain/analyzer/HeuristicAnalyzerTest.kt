package com.bohdankukuruza.scamdetector.domain.analyzer

import com.bohdankukuruza.scamdetector.data.model.DetectionSignal
import com.bohdankukuruza.scamdetector.data.model.RiskLevel
import com.bohdankukuruza.scamdetector.domain.analyzer.rules.DetectionRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
 * Tests use [UnconfinedTestDispatcher] so suspending analyze() runs
 * synchronously inside runTest, keeping assertions deterministic.
 *
 * Note on AI assistance: the FakeRule helper class and the
 * `combines signals from multiple rules` test were drafted with
 * Claude. The threshold-boundary tests were added manually after I
 * noticed the AI's initial test set didn't exercise the boundaries.
 * The confidence-curve tests were rewritten after an AI code review
 * pointed out that the original `confidenceFor` returned 1.0f for
 * any SAFE result regardless of how close to the threshold — the
 * impl was changed and these tests pin the new distance-from-boundary
 * semantics.
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
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun analyzer(rules: List<DetectionRule>) = HeuristicAnalyzer(
        rules = rules,
        clock = fixedClock,
        dispatcher = testDispatcher
    )

    @Test
    fun `blank input returns safe result with no signals`() = runTest {
        val result = analyzer(emptyList()).analyze("   ")

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.signals.isEmpty())
        assertTrue("Safe verdict should be confident but not perfect",
            result.confidence in 0.9f..1.0f)
    }

    @Test
    fun `no firing rules returns safe result`() = runTest {
        val result = analyzer(listOf(SilentRule("a"), SilentRule("b")))
            .analyze("nothing suspicious here")

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.signals.isEmpty())
    }

    @Test
    fun `weight 4 maps to safe`() = runTest {
        val result = analyzer(listOf(FakeRule("r1", weight = 4))).analyze("text")
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    @Test
    fun `weight 5 maps to suspicious at lower boundary`() = runTest {
        val result = analyzer(listOf(FakeRule("r1", weight = 5))).analyze("text")
        assertEquals(RiskLevel.SUSPICIOUS, result.riskLevel)
    }

    @Test
    fun `weight 11 maps to suspicious at upper boundary`() = runTest {
        val result = analyzer(
            listOf(FakeRule("r1", weight = 5), FakeRule("r2", weight = 6))
        ).analyze("text")
        assertEquals(RiskLevel.SUSPICIOUS, result.riskLevel)
    }

    @Test
    fun `weight 12 maps to dangerous at boundary`() = runTest {
        val result = analyzer(
            listOf(FakeRule("r1", weight = 6), FakeRule("r2", weight = 6))
        ).analyze("text")
        assertEquals(RiskLevel.DANGEROUS, result.riskLevel)
    }

    @Test
    fun `combines signals from multiple rules in order`() = runTest {
        val result = analyzer(listOf(
            FakeRule("r1", weight = 3, explanation = "first"),
            SilentRule("r2"),
            FakeRule("r3", weight = 4, explanation = "third")
        )).analyze("text")

        assertEquals(2, result.signals.size)
        assertEquals("r1", result.signals[0].ruleName)
        assertEquals("r3", result.signals[1].ruleName)
    }

    @Test
    fun `confidence is high for clean safe result with zero signals`() = runTest {
        val result = analyzer(listOf(SilentRule())).analyze("hi mum")
        assertTrue("Clean SAFE should be ~0.95 confidence",
            result.confidence >= 0.90f)
    }

    @Test
    fun `confidence is lower for safe result near suspicious threshold`() = runTest {
        val clean = analyzer(listOf(SilentRule())).analyze("hi")
        val borderline = analyzer(listOf(FakeRule("r", weight = 4))).analyze("hi")

        assertTrue("Borderline SAFE should be less confident than clean SAFE",
            borderline.confidence < clean.confidence)
        assertEquals(RiskLevel.SAFE, borderline.riskLevel)
    }

    @Test
    fun `confidence scales up for strongly dangerous results`() = runTest {
        val justOverThreshold = analyzer(
            listOf(FakeRule("a", weight = 6), FakeRule("b", weight = 6))
        ).analyze("text")

        val muchOverThreshold = analyzer(
            listOf(
                FakeRule("a", weight = 9),
                FakeRule("b", weight = 9),
                FakeRule("c", weight = 8)
            )
        ).analyze("text")

        assertTrue("Strongly dangerous should be more confident than borderline",
            muchOverThreshold.confidence > justOverThreshold.confidence)
    }


    @Test
    fun `confidence stays within contract bounds`() = runTest {
        val low = analyzer(listOf(FakeRule("r", weight = 2))).analyze("text")
        val mid = analyzer(listOf(FakeRule("a", weight = 6))).analyze("text")
        val high = analyzer(
            listOf(FakeRule("a", weight = 10), FakeRule("b", weight = 10))
        ).analyze("text")

        assertTrue("low confidence in bounds", low.confidence in 0f..1f)
        assertTrue("mid confidence in bounds", mid.confidence in 0f..1f)
        assertTrue("high confidence in bounds", high.confidence in 0f..1f)
    }


    @Test
    fun `analyzedAt uses injected clock`() = runTest {
        val analyzer = HeuristicAnalyzer(
            rules = emptyList(),
            clock = { 42L },
            dispatcher = testDispatcher
        )
        val result = analyzer.analyze("anything")

        assertEquals(42L, result.analyzedAt)
    }

    @Test
    fun `default rules wire all production rules`() = runTest {
        val analyzer = HeuristicAnalyzer()
        val result = analyzer.analyze(
            "URGENT: verify your account now at bit.ly/secure-login"
        )
        assertTrue("Production rule set should flag obvious scam text",
            result.riskLevel.isAtLeast(RiskLevel.SUSPICIOUS))
    }
}
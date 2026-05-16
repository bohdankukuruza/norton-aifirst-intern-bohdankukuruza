package com.bohdankukuruza.scamdetector.domain.analyzer.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [UrlShortenerRule].
 *
 * These tests exercise the rule in isolation — no analyzer, no UI,
 * no dependencies. They lock in two things:
 *  1. The rule fires for known shortener domains.
 *  2. The rule stays silent for unrelated text, so it doesn't pollute
 *     the analyzer's signal list with false positives.
 *
 * Note on AI assistance: the bulk of the bit.ly happy-path case
 * (testFiresOnBitlyLink) was drafted by Claude from a one-line prompt
 * and accepted with minor edits. The case-insensitivity test
 * (testIsCaseInsensitive) was also AI-suggested — I had not originally
 * thought to cover uppercase input, and adding it caught a real gap
 * (the rule does lowercase the input, but I had no test asserting it).
 */
class UrlShortenerRuleTest {

    private lateinit var rule: UrlShortenerRule

    @Before
    fun setUp() {
        rule = UrlShortenerRule()
    }

    @Test
    fun `returns null for empty input`() {
        val signal = rule.evaluate("")
        assertNull("Empty input should produce no signal", signal)
    }

    @Test
    fun `returns null for plain text without any URL`() {
        val signal = rule.evaluate("Hi mum, just checking in. See you tomorrow.")
        assertNull("Innocent text should not raise an urgency signal", signal)
    }

    @Test
    fun `returns null for legitimate full-length URL`() {
        val signal = rule.evaluate("Track your order at https://www.amazon.com/orders/123")
        assertNull("Full-length URLs should not be flagged as shortened", signal)
    }

    @Test
    fun `fires on bitly link`() {
        val signal = rule.evaluate("Click bit.ly/free-prize to claim your reward")

        assertNotNull("bit.ly link must produce a signal", signal)
        assertEquals("url_shortener", signal!!.ruleName)
        assertEquals(7, signal.weight)
        assertTrue(
            "Explanation should mention the matched shortener",
            signal.explanation.contains("bit.ly")
        )
    }

    @Test
    fun `fires on tinyurl link`() {
        val signal = rule.evaluate("See https://tinyurl.com/abc123 for details")
        assertNotNull(signal)
        assertEquals("url_shortener", signal!!.ruleName)
    }

    @Test
    fun `is case insensitive`() {
        val lower = rule.evaluate("link: bit.ly/x")
        val mixed = rule.evaluate("link: Bit.Ly/x")
        val upper = rule.evaluate("link: BIT.LY/X")

        assertNotNull("lowercase should match", lower)
        assertNotNull("mixed case should match", mixed)
        assertNotNull("uppercase should match", upper)
    }

    @Test
    fun `weight stays within contract bounds`() {
        val signal = rule.evaluate("Open t.co/abc for info")
        assertNotNull(signal)
        // DetectionSignal contract: weight in 1..10. Defensive assertion
        // in case the rule's hardcoded weight ever drifts out of range.
        assertTrue("Weight must be in 1..10", signal!!.weight in 1..10)
    }
}
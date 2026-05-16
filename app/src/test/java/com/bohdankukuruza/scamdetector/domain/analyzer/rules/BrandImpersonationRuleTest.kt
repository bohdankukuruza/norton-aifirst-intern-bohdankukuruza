package com.bohdankukuruza.scamdetector.domain.analyzer.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BrandImpersonationRule].
 *
 * The first half of the suite covers happy-path and basic negative
 * cases. The second half — `does not match brand domain as a suffix`
 * onward — are **regression tests** added in response to an AI code
 * review that flagged two real bugs in earlier versions:
 *
 *  1. Substring `contains` matching of legitimate domains, so a URL
 *     like `amazon.com.evil-phish.tk` was treated as legitimate.
 *  2. Substring `contains` matching of brand names, so short brand
 *     keys like `ups` fired on `groups` and `aib` on `caribbean`.
 *
 * Both bugs were exactly the class of issue this detector exists to
 * catch, so they get explicit pinning tests rather than just code
 * fixes.
 *
 * Note on AI assistance: the four regression test cases below were
 * suggested verbatim by the AI code review. I accepted them as-is.
 */
class BrandImpersonationRuleTest {

    private lateinit var rule: BrandImpersonationRule

    @Before
    fun setUp() {
        rule = BrandImpersonationRule()
    }

    @Test
    fun `returns null for empty input`() {
        assertNull(rule.evaluate(""))
    }

    @Test
    fun `returns null when no URLs present`() {
        assertNull(rule.evaluate("Hi, just calling to confirm dinner with the Amazon team tomorrow."))
    }

    @Test
    fun `returns null when brand mentioned with legitimate link`() {
        val signal = rule.evaluate(
            "Your Amazon order has shipped. Track it at https://www.amazon.com/orders/123"
        )
        assertNull("Legitimate amazon.com link must not be flagged", signal)
    }

    @Test
    fun `flags amazon mention with unrelated domain`() {
        val signal = rule.evaluate(
            "Amazon: your package is held. Update at https://amzn-delivery.top/track"
        )
        assertNotNull("Amazon mention with non-amazon domain must be flagged", signal)
        assertEquals("brand_impersonation", signal!!.ruleName)
    }

    @Test
    fun `flags paypal mention with unrelated domain`() {
        val signal = rule.evaluate(
            "PayPal security alert: verify at paypal-secure-check.xyz"
        )
        assertNotNull(signal)
    }

    // --- Regression tests for bugs raised by AI code review ---

    @Test
    fun `does not match brand domain as a suffix (regression)`() {
        // Bug: earlier version used String.contains so URLs ending in
        // ".amazon.com" but hosted elsewhere were classified as legit.
        // The malicious URL "amazon.com.evil-phish.tk" must now fire.
        val signal = rule.evaluate(
            "Amazon order update: confirm at amazon.com.evil-phish.tk/login"
        )
        assertNotNull(
            "amazon.com.evil-phish.tk must be flagged — it is not actually amazon.com",
            signal
        )
    }

    @Test
    fun `does not falsely match brand suffix inside attacker domain (regression)`() {
        // Bug: previously "notapple.com" would match legitimate apple.com
        // by suffix contains. Now suffix matching uses a leading-dot
        // guard so only proper subdomains match.
        val signal = rule.evaluate(
            "Apple ID locked: verify identity at https://notapple.com/account"
        )
        assertNotNull(
            "notapple.com must not satisfy the apple.com legitimacy check",
            signal
        )
    }

    @Test
    fun `does not fire on substring brand match in unrelated word (regression)`() {
        // Bug: brand keys like "ups" were substring-matched, firing on
        // any text containing "groups", "setups", "startups" etc.
        // Now matched via \b word boundary regex.
        val signal = rule.evaluate(
            "Your team groups have been updated. View them at https://example.com/groups"
        )
        assertNull(
            "The word 'groups' must not trigger the 'ups' brand rule",
            signal
        )
    }

    @Test
    fun `does not fire on aib substring inside caribbean (regression)`() {
        // Bug: "aib" brand key was substring-matched against the word
        // "Caribbean", producing a false positive.
        val signal = rule.evaluate(
            "Win a trip to the Caribbean! Enter at https://travel-deals.top/win"
        )
        assertNull(
            "The word 'Caribbean' must not trigger the 'aib' brand rule",
            signal
        )
    }
}
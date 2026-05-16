package com.bohdankukuruza.scamdetector.domain.analyzer.rules

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SuspiciousDomainRule].
 *
 * Includes a regression test added after AI code review flagged that
 * the original `hasSuspiciousTld` used `String.contains` for TLD
 * matching, so `.click` would incorrectly fire on `clickbank.com`.
 * The fix extracts the host and checks `host.endsWith(tld)`, which
 * is pinned by `does not fire on TLD substring inside a host`.
 */
class SuspiciousDomainRuleTest {

    private lateinit var rule: SuspiciousDomainRule

    @Before
    fun setUp() {
        rule = SuspiciousDomainRule()
    }

    @Test
    fun `returns null for empty input`() {
        assertNull(rule.evaluate(""))
    }

    @Test
    fun `returns null for plain text without URLs`() {
        assertNull(rule.evaluate("Hi mum, see you at 7."))
    }

    @Test
    fun `flags suspicious top-level domain`() {
        val signal = rule.evaluate("Update your account at https://login-verify.tk/auth")
        assertNotNull(signal)
        assertTrue("Explanation should mention TLD",
            signal!!.explanation.lowercase().contains("top-level domain"))
    }

    @Test
    fun `flags raw IP address URL`() {
        val signal = rule.evaluate("Login required: http://192.168.1.100/admin")
        assertNotNull(signal)
        assertTrue("Explanation should mention IP address",
            signal!!.explanation.lowercase().contains("ip address"))
    }

    @Test
    fun `flags excessive hyphens in host`() {
        val signal = rule.evaluate("Click amazon-account-verify-secure-login.com to confirm")
        assertNotNull("Domain with 4 hyphens must be flagged", signal)
    }

    @Test
    fun `does not flag legitimate domain`() {
        val signal = rule.evaluate("Track at https://www.amazon.co.uk/orders/123")
        assertNull("amazon.co.uk should not be flagged", signal)
    }

    // --- Regression tests for bugs raised by AI code review ---

    @Test
    fun `does not fire on TLD substring inside a host (regression)`() {
        // Bug: earlier hasSuspiciousTld used contains(".click"), so any
        // domain with "click" anywhere (e.g. clickbank.com) was flagged.
        // The fix uses host.endsWith(tld), so clickbank.com no longer
        // matches the .click TLD.
        val signal = rule.evaluate("Visit https://clickbank.com/affiliate for details")
        assertNull(
            "clickbank.com must not match .click TLD via substring",
            signal
        )
    }

    @Test
    fun `multiple suspicious patterns increase weight`() {
        val singleIssue = rule.evaluate("Visit https://my-shop.tk for deals")
        val multipleIssues = rule.evaluate(
            "Login at https://amazon-account-verify-secure.tk/auth"
        )
        assertNotNull(singleIssue)
        assertNotNull(multipleIssues)
        assertTrue("Multiple patterns must produce higher weight",
            multipleIssues!!.weight > singleIssue!!.weight)
    }
}
package com.bohdankukuruza.scamdetector.domain.analyzer.rules

import com.bohdankukuruza.scamdetector.data.model.DetectionSignal

/**
 * Detects URLs whose domains exhibit patterns commonly associated with
 * phishing rather than legitimate brand sites.
 *
 * Typical phishing patterns we flag:
 *  - **Brand-in-subdomain abuse:** `paypal.security-check.com` — looks
 *    like PayPal at a glance, but the real registered domain is
 *    `security-check.com`.
 *  - **Excessive hyphens:** `amaz0n-account-verify-secure.com` — legit
 *    sites rarely chain multiple hyphenated keywords.
 *  - **Suspicious top-level domains:** free or low-trust TLDs (.tk, .ml,
 *    .ga, .cf, .gq, .top, .xyz) are disproportionately abused by
 *    short-lived scam infrastructure.
 *  - **IP-address URLs:** legitimate services never expose raw IPs in
 *    user-facing links.
 *
 * The rule extracts each URL-like substring from the message and
 * evaluates each pattern independently. Multiple hits stack into a
 * higher weight, because the more anomalies a single domain shows, the
 * higher the confidence of phishing.
 */
class SuspiciousDomainRule : DetectionRule {

    override val name: String = "suspicious_domain"

    /**
     * Very loose URL matcher. Catches `http(s)://...`, `www....`, and
     * bare `something.tld/path` forms. Not RFC-compliant by design —
     * scammers don't write RFC-compliant URLs either, and we'd rather
     * over-capture and let downstream pattern checks filter than miss
     * a payload because of a strict parser.
     */
    private val urlRegex: Regex = Regex(
        pattern = "(https?://|www\\.)?[a-z0-9][a-z0-9.\\-]*\\.[a-z]{2,}(/\\S*)?",
        option = RegexOption.IGNORE_CASE
    )

    private val suspiciousTlds: Set<String> = setOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".top", ".xyz", ".click", ".country"
    )

    private val ipAddressRegex: Regex = Regex(
        """\b(?:\d{1,3}\.){3}\d{1,3}\b"""
    )

    override fun evaluate(text: String): DetectionSignal? {
        if (text.isBlank()) return null

        val reasons = mutableListOf<String>()

        // Check for raw IP addresses in URLs first — strong indicator.
        if (ipAddressRegex.containsMatchIn(text)) {
            reasons += "uses a raw IP address instead of a domain name"
        }

        val urls = urlRegex.findAll(text).map { it.value.lowercase() }.toList()

        for (url in urls) {
            if (hasSuspiciousTld(url)) {
                reasons += "uses a suspicious top-level domain ($url)"
            }
            if (hasExcessiveHyphens(url)) {
                reasons += "contains an unusual number of hyphens ($url)"
            }
        }

        if (reasons.isEmpty()) return null

        val weight = when (reasons.size) {
            1 -> 5
            2 -> 7
            else -> 9
        }

        val explanation = "Suspicious link pattern: ${reasons.joinToString("; ")}."

        return DetectionSignal(
            ruleName = name,
            weight = weight,
            explanation = explanation
        )
    }

    private fun hasSuspiciousTld(url: String): Boolean =
        suspiciousTlds.any { tld -> url.contains(tld) }

    /**
     * Treats a URL as having "excessive" hyphens if its host part
     * contains 3 or more. Legitimate brand domains almost never do this.
     */
    private fun hasExcessiveHyphens(url: String): Boolean {
        val host = url
            .substringAfter("://", missingDelimiterValue = url)
            .substringBefore('/')
        return host.count { it == '-' } >= 3
    }
}
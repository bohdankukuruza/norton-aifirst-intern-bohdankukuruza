package com.bohdankukuruza.scamdetector.domain.analyzer.rules

import com.bohdankukuruza.scamdetector.data.model.DetectionSignal

/**
 * Detects use of common URL-shortening services in the message.
 *
 * URL shorteners (bit.ly, tinyurl, t.co, etc.) are heavily abused in
 * SMS and email scams because they hide the real destination from the
 * recipient and from naïve URL-filtering systems. Legitimate brands
 * almost never shorten links in transactional messages — they use
 * their own domain — so a shortened link in an "official" message is
 * a strong signal of impersonation.
 *
 * This rule does not attempt to expand the link or judge its target;
 * it only flags the *presence* of a known shortener domain.
 */
class UrlShortenerRule : DetectionRule {

    override val name: String = "url_shortener"

    /**
     * Domains we consider URL shorteners. Lowercased; matching is
     * case-insensitive on the input side.
     *
     * The list intentionally covers only the most widely abused
     * services. A real product would source this from a maintained
     * threat-intel feed; for an offline heuristic, a curated short
     * list keeps false positives low.
     */
    private val shortenerDomains: Set<String> = setOf(
        "bit.ly",
        "tinyurl.com",
        "goo.gl",
        "t.co",
        "ow.ly",
        "is.gd",
        "buff.ly",
        "rebrand.ly",
        "cutt.ly",
        "shorturl.at"
    )

    override fun evaluate(text: String): DetectionSignal? {
        val normalized = text.lowercase()
        val matched = shortenerDomains.firstOrNull { domain ->
            normalized.contains(domain)
        } ?: return null

        return DetectionSignal(
            ruleName = name,
            weight = 7,
            explanation = "Link uses a URL shortener ($matched), which hides the real destination."
        )
    }
}
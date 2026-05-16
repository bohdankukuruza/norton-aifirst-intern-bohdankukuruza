package com.bohdankukuruza.scamdetector.domain.analyzer.rules

import com.bohdankukuruza.scamdetector.data.model.DetectionSignal

/**
 * Detects messages that name a well-known brand but link to a domain
 * unrelated to that brand — a classic phishing pattern (e.g. an
 * "Amazon" delivery notification pointing to amzn-track.xyz).
 *
 * ### Host matching
 * Earlier versions of this rule used `String.contains` to compare URLs
 * against legitimate domains, which incorrectly classified
 * `amazon.com.evil-phish.tk` as legitimate because the URL "contains"
 * `amazon.com`. The current implementation extracts the host of each
 * URL and matches against legitimate domains using suffix comparison
 * with a leading-dot guard (`host == legit || host.endsWith(".$legit")`),
 * which is the standard approach when not pulling in a full public
 * suffix list.
 *
 * ### Brand mention matching
 * Brand keys are matched with word-boundary regexes rather than
 * substring contains — otherwise short keys like `ups` would fire on
 * `groups` and `aib` on `caribbean`.
 */
class BrandImpersonationRule : DetectionRule {

    override val name: String = "brand_impersonation"

    private val brandDomains: Map<String, Set<String>> = mapOf(
        "amazon" to setOf("amazon.com", "amazon.co.uk", "amazon.ie", "amzn.to"),
        "paypal" to setOf("paypal.com", "paypal.me"),
        "netflix" to setOf("netflix.com"),
        "apple" to setOf("apple.com", "icloud.com", "itunes.com"),
        "microsoft" to setOf("microsoft.com", "live.com", "outlook.com", "office.com"),
        "google" to setOf("google.com", "gmail.com", "youtube.com"),
        "dhl" to setOf("dhl.com", "dhl.de"),
        "fedex" to setOf("fedex.com"),
        "ups" to setOf("ups.com"),
        "an post" to setOf("anpost.ie", "anpost.com"),
        "revolut" to setOf("revolut.com"),
        "aib" to setOf("aib.ie"),
        "bank of ireland" to setOf("bankofireland.com")
    )

    private val urlRegex: Regex = Regex(
        pattern = "(https?://|www\\.)?[a-z0-9][a-z0-9.\\-]*\\.[a-z]{2,}(/\\S*)?",
        option = RegexOption.IGNORE_CASE
    )

    override fun evaluate(text: String): DetectionSignal? {
        if (text.isBlank()) return null

        val normalized = text.lowercase()
        val urls = urlRegex.findAll(normalized).map { it.value }.toList()
        if (urls.isEmpty()) return null

        val hosts = urls.map(::extractHost)

        val impersonated = brandDomains.entries.firstOrNull { (brand, legitimateDomains) ->
            val brandMentioned = mentionsBrand(normalized, brand)
            val linksToLegit = hosts.any { host ->
                legitimateDomains.any { legit -> hostMatches(host, legit) }
            }
            brandMentioned && !linksToLegit
        } ?: return null

        val (brand, _) = impersonated
        val brandDisplay = brand.replaceFirstChar { it.uppercase() }
        val suspiciousUrl = urls.first()

        return DetectionSignal(
            ruleName = name,
            weight = 8,
            explanation = "Message mentions $brandDisplay but links to an unrelated domain ($suspiciousUrl)."
        )
    }

    /**
     * Extracts the host portion of a URL: strips scheme, `www.` prefix,
     * and any path. Lowercased input expected.
     */
    private fun extractHost(url: String): String =
        url.substringAfter("://", missingDelimiterValue = url)
            .removePrefix("www.")
            .substringBefore('/')

    /**
     * Suffix match with leading-dot guard so `amazon.com` does not
     * match `amazon.com.evil-phish.tk` and `apple.com` does not match
     * `notapple.com`.
     */
    private fun hostMatches(host: String, legitimate: String): Boolean =
        host == legitimate || host.endsWith(".$legitimate")

    /**
     * Word-boundary match for brand names. Brand keys may contain
     * spaces (e.g. "an post"), so we anchor on `\b` rather than
     * splitting on whitespace.
     */
    private fun mentionsBrand(text: String, brand: String): Boolean {
        val escaped = Regex.escape(brand)
        return Regex("\\b$escaped\\b").containsMatchIn(text)
    }
}
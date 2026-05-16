package com.bohdankukuruza.scamdetector.domain.analyzer.rules

import com.bohdankukuruza.scamdetector.data.model.DetectionSignal

/**
 * Detects messages that name a well-known brand but link to a domain
 * unrelated to that brand — a classic phishing pattern (e.g. an
 * "Amazon" delivery notification pointing to amzn-track.xyz).
 */
class BrandImpersonationRule : DetectionRule {

    override val name: String = "brand_impersonation"

    /**
     * Map of brand name (as it appears in scam text) to the set of
     * legitimate domains that brand actually uses. If the message
     * mentions the brand but no link matches its legitimate domains,
     * we flag it.
     */
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

        val impersonated = brandDomains.entries.firstOrNull { (brand, legitimateDomains) ->
            val brandMentioned = normalized.contains(brand)
            val linksToLegit = urls.any { url ->
                legitimateDomains.any { legit -> url.contains(legit) }
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
}
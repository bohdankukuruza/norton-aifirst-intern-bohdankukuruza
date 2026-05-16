package com.bohdankukuruza.scamdetector.data.model

/**
 * Represents the assessed danger level of an analyzed message.
 *
 * Ordered from least to most dangerous so callers can compare with
 * standard ordering operators (e.g. `riskLevel >= SUSPICIOUS`).
 *
 * @property displayName Human-readable label shown in the UI.
 * @property colorHex Brand-aligned hex color used by the risk badge.
 */
enum class RiskLevel(
    val displayName: String,
    val colorHex: String
) {
    SAFE(displayName = "Safe", colorHex = "#2E7D32"),
    SUSPICIOUS(displayName = "Suspicious", colorHex = "#F9A825"),
    DANGEROUS(displayName = "Dangerous", colorHex = "#C62828");

    /**
     * Returns true if this risk level is at least as severe as [other].
     * Useful for guard clauses like `if (result.riskLevel.isAtLeast(SUSPICIOUS))`.
     */
    fun isAtLeast(other: RiskLevel): Boolean = this.ordinal >= other.ordinal
}
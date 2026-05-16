package com.bohdankukuruza.scamdetector.domain.analyzer.rules

import com.bohdankukuruza.scamdetector.data.model.DetectionSignal

/**
 * Detects messages with an abnormally high proportion of uppercase
 * letters — a low-effort but reliable cue for shouty scam SMS like
 * "WINNER! CLAIM YOUR PRIZE NOW".
 *
 * Only counts alphabetic characters to avoid bias from numbers and
 * punctuation. Ignores very short messages where a single capitalised
 * word would skew the ratio.
 */
class ExcessiveCapsRule : DetectionRule {

    override val name: String = "excessive_caps"

    private val minimumLetters: Int = 20
    private val capsRatioThreshold: Float = 0.5f

    override fun evaluate(text: String): DetectionSignal? {
        val letters = text.filter { it.isLetter() }
        if (letters.length < minimumLetters) return null

        val upperCount = letters.count { it.isUpperCase() }
        val ratio = upperCount.toFloat() / letters.length

        if (ratio < capsRatioThreshold) return null

        val percentage = (ratio * 100).toInt()
        val weight = when {
            ratio >= 0.8f -> 5
            ratio >= 0.65f -> 4
            else -> 3
        }

        return DetectionSignal(
            ruleName = name,
            weight = weight,
            explanation = "Message is $percentage% uppercase, typical of shouty scam alerts."
        )
    }
}
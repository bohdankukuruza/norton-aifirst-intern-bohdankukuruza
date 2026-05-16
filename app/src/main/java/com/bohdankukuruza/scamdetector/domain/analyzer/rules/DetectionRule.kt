package com.bohdankukuruza.scamdetector.domain.analyzer.rules

import com.bohdankukuruza.scamdetector.data.model.DetectionSignal

/**
 * A single, focused heuristic that inspects a message and decides whether
 * it exhibits one specific scam indicator.
 *
 * Each rule is intentionally narrow — for example, "does this message
 * contain a URL shortener?" or "does this message use urgency keywords?".
 * The analyzer composes many rules together; rules themselves never
 * combine signals or assign overall risk.
 *
 * ### Adding a new rule
 * 1. Create a new file in this package.
 * 2. Implement [DetectionRule] with a stable [name] and the detection
 *    logic in [evaluate].
 * 3. Register the rule in `HeuristicAnalyzer`'s rule list.
 * 4. Add a unit test in `src/test/.../rules/`.
 *
 * No other code needs to change — the analyzer iterates over the rule
 * list, so new rules plug in without touching aggregation logic.
 */
interface DetectionRule {

    /**
     * Stable, lower_snake_case identifier for this rule.
     *
     * Used for logging, analytics, and as the [DetectionSignal.ruleName]
     * field. Must not change across versions — external systems may
     * depend on it.
     */
    val name: String

    /**
     * Inspects [text] and returns a [DetectionSignal] if this rule's
     * scam indicator is present, or `null` if not.
     *
     * Returning `null` (rather than a zero-weight signal) keeps the
     * aggregated signal list focused on actual evidence and avoids
     * polluting the UI with "rule X did not fire" entries.
     *
     * Implementations must be pure: same input always yields the same
     * output, no side effects, no I/O. This makes rules trivially
     * testable and safe to run in parallel.
     */
    fun evaluate(text: String): DetectionSignal?
}
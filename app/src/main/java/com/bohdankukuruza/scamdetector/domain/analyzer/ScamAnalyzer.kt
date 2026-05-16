package com.bohdankukuruza.scamdetector.domain.analyzer

import com.bohdankukuruza.scamdetector.data.model.AnalysisResult

/**
 * Top-level contract for analyzing a message and producing a scam-risk
 * verdict.
 *
 * This interface intentionally hides *how* analysis is performed. The
 * default implementation, [HeuristicAnalyzer], evaluates a fixed set of
 * local rules and aggregates their signals. A future implementation
 * could delegate to a remote LLM (e.g. the Anthropic API) without
 * changing any UI or ViewModel code — both would share this same
 * contract.
 *
 * ### Why an interface and not a single class
 * Keeping analysis behind an interface gives us three properties:
 * 1. **Swappability.** Heuristic, AI-backed, or hybrid analyzers can be
 *    interchanged at the DI boundary.
 * 2. **Testability.** ViewModels can be tested against a fake analyzer
 *    that returns scripted results, without touching real rules or
 *    network.
 * 3. **Honest seams.** The boundary between "what we ask for" and
 *    "how we get it" is explicit, which keeps the rest of the code
 *    free of analyzer-specific assumptions.
 */
interface ScamAnalyzer {

    /**
     * Analyzes [text] and returns a complete [AnalysisResult].
     *
     * Declared as `suspend` so implementations may perform asynchronous
     * work (e.g. network calls to an LLM) without changing the call
     * site. Pure local implementations like [HeuristicAnalyzer] simply
     * return immediately — `suspend` imposes no runtime cost for them.
     *
     * Implementations must handle empty or whitespace-only input
     * gracefully, returning a SAFE result rather than throwing.
     *
     * @param text The user-supplied message, URL, or snippet to analyze.
     * @return A populated [AnalysisResult] — never null.
     */
    suspend fun analyze(text: String): AnalysisResult
}
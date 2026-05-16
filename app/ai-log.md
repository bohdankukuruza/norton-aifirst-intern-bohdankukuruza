# AI Interaction Log

This log documents how I used Claude (Anthropic) as my primary AI coding
assistant throughout this project. Each entry captures a meaningful prompt,
what the AI produced, and what I did with the output — accepted, modified,
or rejected.

Tool used: **Claude** (claude.ai web interface).

---

## Prompt 1 — Architectural foundation and data model design

**Context:** Starting the project. I wanted the AI to set the overall
direction for the data layer before I wrote any code, so that all subsequent
decisions would follow from a coherent architecture rather than ad-hoc
choices.

**Prompt:**

> [Тут потім вставимо реальний промпт який ти даси Claude. Поки залиш заглушку.]

**What the AI produced:** A three-layer architecture (data / domain / ui),
suggesting `RiskLevel` enum, `DetectionSignal` and `AnalysisResult` data
classes, and a `DetectionRule` interface for pluggable rules.

**What I did with it:**
- **Accepted:** the overall three-layer split and the rule-per-class pattern.
- **Modified:** added `init` blocks with `require()` checks to the data
  classes — the AI did not include input validation, but I wanted
  fail-fast semantics so an invalid signal can never exist.
- **Modified:** added a `companion object` factory `AnalysisResult.safe()`
  because the empty-signal SAFE result was being constructed in multiple
  places and the verbose form hurt readability.
- **Rejected:** the AI initially suggested `weight: Float` for signal
  weight. I changed it to `Int` in 1..10 — easier to reason about,
  easier to test, and the precision wasn't meaningful.

---     
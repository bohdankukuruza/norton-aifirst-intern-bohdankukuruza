# AI Interaction Log

This log documents how I used Claude (Anthropic) as my primary AI coding
assistant throughout this project. Each entry captures a meaningful prompt,
what the AI produced, and what I did with the output — accepted, modified,
or rejected.

**Tool used:** Claude (claude.ai web interface).

**Workflow:** I kept Claude open in a side window throughout development.
For architectural decisions I drafted the prompt carefully and reviewed
the answer in detail; for routine boilerplate I iterated faster. Every
non-trivial suggestion went through review before landing in the repo.

---

## Prompt 1 — Architectural foundation and data model design

**Context:** Starting the project. I wanted the AI to set the overall
direction for the data layer before I wrote any code, so that all
subsequent decisions would follow from a coherent architecture rather
than ad-hoc choices.

**Prompt:**

> I'm building an Android app for a Norton Mobile Engineering intern
> take-home assignment — a "Scam Message Detector" inspired by Norton
> Genie. User pastes a suspicious SMS/email/URL, app analyses it locally
> and returns risk level + confidence + reasons. Stack: Kotlin, Jetpack
> Compose, MVVM, JUnit. Min SDK 26. Design the foundational data layer:
> RiskLevel enum, DetectionSignal and AnalysisResult data classes,
> ScamAnalyzer and DetectionRule interfaces. Place under data.model,
> domain.analyzer, domain.analyzer.rules packages. Explain non-obvious
> design decisions.

**What the AI produced:** A three-layer architecture (data / domain /
ui), with `RiskLevel` enum carrying display metadata, `DetectionSignal`
and `AnalysisResult` data classes, a `DetectionRule` interface for
pluggable heuristics, and a `ScamAnalyzer` interface as the top-level
seam.

**What I did with it:**

- **Accepted:** the overall three-layer split, the rule-per-class
  pattern, and the decision to make `ScamAnalyzer.analyze()` a
  `suspend` function for forward-compatibility with a future async
  (LLM-backed) implementation.
- **Modified:** added `init` blocks with `require()` checks to both
  `DetectionSignal` and `AnalysisResult` — the AI's initial version
  had no input validation, but I wanted fail-fast semantics so an
  invalid signal can never exist.
- **Modified:** added a `companion object` factory
  `AnalysisResult.safe()` because the empty-signal SAFE result was
  being constructed in multiple places and the verbose form hurt
  readability.
- **Rejected:** the AI initially suggested `weight: Float` for signal
  weight. I changed it to `Int` in 1..10 — easier to reason about,
  easier to test, and the precision wasn't meaningful for our use
  case.

---

## Prompt 2 — Heuristic rule design and weight calibration

**Context:** I had the rule architecture but needed concrete rules.
Rather than asking the AI to write rule classes one at a time, I asked
for a brainstorm of what indicators experienced phishing analysts
actually look for — then chose which ones to implement.

**Prompt:**

> For a scam SMS / email detector, what are the most reliable
> non-AI heuristic indicators of a phishing message? Rank them by
> precision (false-positive cost) vs recall (detection power). For
> each one, suggest a weight on a 1–10 scale where 1 is a weak hint
> and 10 is near-certain. Justify the weights. Focus on indicators
> I can detect from text alone without DNS lookups or threat feeds.

**What the AI produced:** A ranked list of about a dozen indicators
— URL shorteners, urgency keywords, suspicious TLDs, brand
impersonation, raw IP URLs, excessive hyphens, all-caps text,
unusual punctuation, lookalike characters, and a few more — each
with a suggested weight and a paragraph of reasoning.

**What I did with it:**

- **Accepted:** the top five (URL shortener, urgency keywords,
  suspicious domain patterns, brand impersonation, excessive caps)
  with weights close to the AI's suggestions.
- **Modified:** I lowered the AI's suggested weight for URL
  shorteners from 9 to 7. Its reasoning was strong, but legitimate
  marketing emails do use shorteners; 9 would cause too many false
  positives on its own.
- **Modified:** I split "urgency keywords" into a count-weighted
  rule (1 phrase → weight 3, 2 → 5, 3+ → 7) rather than a fixed
  weight. The AI's version had a single weight; calibrating by
  count reflects that one urgency phrase can appear innocuously
  but three together rarely do.
- **Rejected:** I left out "lookalike Cyrillic characters" and
  "unusual Unicode" — both are real signals but require careful
  implementation to avoid false positives on legitimate non-English
  text, and would push the project over the 12-hour budget.

---

## Prompt 3 — Unit test generation for UrlShortenerRule

**Context:** After writing `UrlShortenerRule` I asked Claude to draft
the first test class. I wanted to see what test cases it would think
of beyond the obvious happy path.

**Prompt:**

> Write JUnit 4 unit tests in Kotlin for this rule. Use backtick
> function names for readability. Cover happy path, negative cases,
> false-positive guards, and any properties of the rule that should
> hold (case insensitivity, weight bounds). Don't test trivial
> getters. [paste of UrlShortenerRule.kt]

**What the AI produced:** Six tests: one for empty input, one for
plain text, one for a bit.ly link, one for tinyurl, one for the
ruleName invariant, and one mixing upper/lower case.

**What I did with it:**

- **Accepted:** the structure, the use of backtick names, and four
  of the six cases verbatim.
- **Added manually:** a "legitimate full-length URL must not be
  flagged" test — the AI hadn't covered the most important false
  positive case for a security product. Missing it would have been
  a real coverage gap.
- **Added manually:** a `weight stays within contract bounds` test
  to pin the `DetectionSignal` 1..10 invariant. Defensive but cheap.
- **Refined:** I split the case-insensitivity test into three
  separate cases (lower, mixed, upper) so a failure tells you
  which casing broke, not just "case insensitivity is broken".

The class header in the test file explicitly notes which cases were
AI-drafted versus added manually.

---

## Prompt 4 — Adversarial code review

**Context:** Before final polish I asked Claude (in a fresh chat,
no project context) to review the codebase as a senior Android
engineer at Norton would. I deliberately asked for tougher feedback
than the assignment reviewers would give.

**Prompt:**

> I'd like you to do a code review of my Android scam detector
> project. This is a take-home assignment for a Mobile Engineering
> intern position at Norton (Gen Digital), so the reviewers will be
> senior Android engineers. Stack: Kotlin, Jetpack Compose, MVVM,
> JUnit + coroutines-test.
>
> Please review for:
> 1. Architectural smells (SOLID violations, leaky abstractions,
     >    missing seams)
> 2. Idiomatic Kotlin issues (places where I wrote Java-style code)
> 3. Testing gaps — what important cases are missing?
> 4. Compose best practices (state hoisting, recomposition, modifier
     >    order)
> 5. Anything a senior Android engineer would flag in PR review
>
> Be direct and specific. Point to the exact file and what you'd
> change. Don't sugarcoat — I want this to be tougher than what
> Norton's reviewers will see, so I can fix issues before submission.

**What the AI produced:** A 20-point review structured into "bugs a
senior reviewer would flag immediately", "architectural smells",
"idiomatic Kotlin", "Compose", and "testing gaps". Several findings
were genuinely sharp — see Prompt 5 for the specific fixes.

**What I did with it:** I triaged the 20 points into three buckets:

- **Fix now (5 items):** real bugs in a security product, must be
  fixed before submission.
- **Document and defer (8 items):** valid concerns but outside the
  12-hour scope; called out honestly in the README's "What I'd do
  next" section rather than rushed in poorly.
- **Small wins (4 items):** cheap polish, done.
- **Disagreed with (3 items):** explained below in Prompt 5.

The full review is preserved in `docs/ai-code-review.md` for
transparency.

---

## Prompt 5 — Targeted fixes from the code review

**Context:** Following Prompt 4 I went back to Claude to implement
the fixes I'd decided to take. For each one I summarised the issue,
proposed my fix, and asked Claude to either confirm or push back.

**Example sub-prompt (host matching):**

> The review pointed out that `String.contains` for domain matching
> lets `amazon.com.evil-phish.tk` pass as legitimate amazon. My fix
> plan: extract host (strip scheme + path + www prefix), then check
> `host == legit || host.endsWith(".$legit")`. Is the leading-dot
> guard sufficient, or do I need a full public suffix list?

**What the AI produced:** Confirmed the leading-dot guard is correct
for this scope (since brand domains are full registrable domains, not
just suffixes), noted that a full PSL would be needed if I wanted to
detect attackers registering on shared TLDs like `*.co.uk` against a
brand stored as `bbc`, and suggested the same pattern be applied in
`SuspiciousDomainRule.hasSuspiciousTld`.

**What I did with it:**

- **Accepted:** the host-extraction approach and applied it
  consistently to both rules. Added explicit regression tests
  (`amazon.com.evil-phish.tk` must fire, `clickbank.com` must not
  fire) to pin the fix.
- **Accepted:** word-boundary regex matching for brand keys, so
  `ups` no longer fires on `groups`. Added two regression tests
  for this too.
- **Modified:** I disagreed with the review's suggestion to move
  `RiskLevel.colorHex` out of the data layer. The review called
  this a "leaky abstraction" but in practice the alternative is
  a `when` block duplicated in two UI files. For a prototype I
  preferred the smaller surface; for a production app the review's
  point would be correct and I noted this in the README.
- **Rejected:** the suggestion to introduce a `RiskClassifier` and
  `ConfidenceScorer` to split `HeuristicAnalyzer` into three
  classes. Valid SRP point, but the resulting six small files with
  one method each would obscure the strategy rather than clarify
  it — at this scale a single 80-line class is easier to read.
  Documented as a "would extract once thresholds become tunable"
  note in the README.

---

## Prompt 6 — README structure

**Context:** Once code and tests were done I asked Claude to suggest
a structure for the README that would highlight the AI-first workflow
without burying the actual technical content.

**Prompt:**

> Suggest a README structure for an AI-first internship take-home.
> Constraints: must include setup/run instructions, architecture
> overview, AI workflow documentation, and a reflection. The
> reviewer is a senior Android engineer who'll spend ~15 minutes
> reading it. What ordering maximises information density?

**What the AI produced:** A nine-section outline starting with a
TL;DR, then product context, architecture diagram, AI workflow
deep-dive, test coverage table, and a "what I'd do next" reflection.

**What I did with it:** Adopted the structure largely as-is. Reordered
"Architecture" before "Setup" because the reviewer is more likely to
read top-down than try to run the code immediately. Added a "Bugs
caught by AI code review" subsection that I think is the most
distinctive part of the submission.
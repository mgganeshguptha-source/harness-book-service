---
name: build-context
description: >
  Builds a structured context.md file for GitHub Copilot from a JIRA story,
  screenshot, or Figma export. Use this whenever a developer pastes a user
  story, mentions building context.md, says they have a story to prepare for
  Copilot, or shares a design alongside a story. Works across Angular
  frontend, Spring Boot backend, and full stack work, and across new
  development, enhancements, and bug fixes. The skill enforces a hard quality
  bar — it efuses to output context with vague language like "should work
  better", "displayed properly", or "performance should be acceptable", and
  it asks targeted questions until acceptance criteria are testable. The
  final context is written to a timestamped file under
  .github/story-context-files/, not just shown in chat.

  Acceptance criteria are written in EARS syntax (WHEN/WHILE/IF-THEN ... SHALL),
  carry stable identifiers (AC-1, AC-2.1) so tasks and validation can cite them,
  and are marked `[ASSUMED]` when the model supplies a criterion the story never
  asked for.

  The context ends with a Story Quality Score (six dimensions, reported for
  trend measurement, never gating) and a feasibility verdict (GO / NO_GO with a
  named blocker class) that the harness enforces.

  In NON-INTERACTIVE / CI mode (no human to answer questions), the skill does
  NOT ask questions. Instead it records every gap as a `[NEEDS CLARIFICATION]`
  line in Section 8 of context.md and writes the file immediately. A downstream
  harness gate blocks progression while any `[NEEDS CLARIFICATION]` remains.
---

# Build Context Skill

Helps a developer turn a JIRA story (and any attached design) into a clean
`context.md` file Copilot can act on. The output describes **what** the work
is — not **how** to implement it. Implementation discovery happens later in
the analysis prompt step.

Stack assumed: **Angular** on the frontend, **Java Spring Boot microservices**
on the backend.

---

## Why context.md exists

When developers paste a raw JIRA story into Copilot, results are inconsistent
because stories are usually written for humans, not models — vague acceptance
criteria, missing edge cases, implicit assumptions. A good `context.md`
captures the *what* with enough specificity that Copilot can reason about the
*how* without guessing.

Two failure modes this skill is designed to prevent:

1. **The empty form.** A context.md filled with vague criteria like "search
   should work correctly" — useless, because that line could describe any
   search behaviour. **A context.md that contains banned phrases (see below)
   is worse than no context.md at all** — it gives Copilot false confidence
   and produces wrong implementations.
2. **The over-specified spec.** File paths, class names, or implementation
   steps written into context.md — which prematurely narrows Copilot's
   analysis and produces solutions that don't fit the existing codebase.

Everything below serves these two goals.

---

## Banned phrases — never appear in any output

These phrases (and close variants) must not appear in Expected Behaviour or
Acceptance Criteria. If you find yourself writing one, stop and ask the
developer for the specific behaviour instead.

| Banned | Why | Replace with |
|---|---|---|
| "should work better" / "work correctly" / "work properly" | Untestable | The exact input → exact output mapping |
| "displayed properly" / "displayed clearly" | Untestable | The specific fields shown, ordering, layout |
| "performance should be acceptable" / "promptly" / "quickly" | Untestable | A concrete number (P95 < Xms, render < Ys) |
| "handle errors properly" / "gracefully" | Untestable | The specific error response or UI state |
| "relevant results" / "appropriate behaviour" | Untestable | The exact match rule or behaviour |
| "improve" / "enhance" without saying *what* about it changes | Restates the story title | The specific delta from current to expected |

**This is a hard preflight check.** Before outputting context.md, scan for
these phrases. If any are present, do not output — go back and ask one more
targeted question.

**Banned-phrase scan applies inside `[NEEDS CLARIFICATION]` blocks too.**
A clarification line that says *"Search should work better - specify match
rule"* still propagates the banned phrase into the document. The
clarification must describe only the **missing dimension**, not parrot
back the vague phrase it's replacing.

| Bad clarification (banned phrase leaks through) | Good clarification (names the missing dimension) |
|---|---|
| `[NEEDS CLARIFICATION]: Performance is acceptable - specify metric` | `[NEEDS CLARIFICATION]: Performance target — P95 latency, result-set size, concurrency` |
| `[NEEDS CLARIFICATION]: Search should work better - specify match rule` | `[NEEDS CLARIFICATION]: Match rule — case sensitivity, match type, fields searched` |
| `[NEEDS CLARIFICATION]: Results displayed properly - specify layout` | `[NEEDS CLARIFICATION]: Result display — column order, sort, mobile layout` |

**Banned-TOPIC scan (process/metadata clarifications).** Separately from the
banned-phrase scan, before outputting `context.md`, scan every `[NEEDS
CLARIFICATION]` line for process/project/staffing topics and DELETE any that
match — they are never valid clarifications because they cannot be answered from a
story and do not block implementation. A clarification is INVALID (remove it) if
it asks about any of: owner, code owner, responsible team, PR reviewer, approver,
sign-off, assignee, ticket/Jira/issue linkage, sprint, milestone, timeline,
deadline, due date, priority, estimate, branch name, PR/commit naming, release or
deployment process, or "who" should do/review/own anything. These are project
metadata, not implementation gaps. For any such section the story leaves empty,
write `N/A (not required for implementation)` in the body — do NOT emit a
`[NEEDS CLARIFICATION]`. Only technical/behavioural ambiguities (see CI-mode rule
6) may remain as clarifications after this scan.

---

## Acceptance criteria — syntax, numbering, and provenance

Three rules govern every AC. They exist so a downstream Validate step can check
the delivered code **per criterion**, and so a human can tell at a glance which
criteria came from the story and which the model supplied.

### Rule 1 — EARS phrasing

Write each AC using EARS (Easy Approach to Requirements Syntax). Pick the
simplest pattern that fits; never force a trigger onto a requirement that is
always true.

| Pattern | Shape | Use when |
|---|---|---|
| Ubiquitous | THE \<system\> SHALL \<response\> | Always true, no trigger |
| Event-driven | WHEN \<trigger\>, THE \<system\> SHALL \<response\> | A discrete event causes it |
| State-driven | WHILE \<state\>, THE \<system\> SHALL \<response\> | True for the duration of a state |
| Unwanted behaviour | IF \<condition\>, THEN THE \<system\> SHALL \<response\> | Error, invalid input, failure |
| Optional | WHERE \<feature is present\>, THE \<system\> SHALL \<response\> | Conditional on a feature or config |

The system name is the component under test in the reader's language — *the
search endpoint*, *the owner list page*, *the catalog client* — never a class or
file name (see "What never goes in context.md").

**One sentence, at most three preconditions.** Beyond three the sentence stops
being readable and the requirement is really several requirements — split it. If
a criterion genuinely needs a matrix of conditions, put a short table under the
AC rather than nesting clauses.

**ONE RESPONSE PER CRITERION.** This is the rule most often broken, and it is
broken by punctuation rather than by length. A criterion containing a semicolon,
a second `IF`, or a second `THEN` is two criteria wearing one identifier:

```
BAD   AC-5: IF the catalog returns empty, THEN return 404;
             IF the author is blank or over 256 characters, THEN return 400.

GOOD  AC-5: IF the catalog returns no match, THEN THE endpoint SHALL return 404.
      AC-6: IF the author is blank, THEN THE endpoint SHALL return 400.
      AC-7: IF the author exceeds 256 characters, THEN THE endpoint SHALL return 400.
```

Splitting costs nothing; bundling breaks the thing the identifiers exist for.
A downstream Validate step returns ONE verdict per identifier, so a bundled
criterion where the 404 works and the blank check does not can only come back as
a false MET or an unexplained NOT_MET — and neither says which half failed. The
same applies to the coverage matrix in the plan: a bundled criterion maps to a
step that only half-delivers it, and nothing surfaces the gap.

Two responses joined by "and" are also two criteria, even when both are true at
once. If you find yourself writing "and returns" or "and logs", stop and split.

**EARS is phrasing, not proof.** It makes a criterion unambiguous and
individually testable. It does not verify that the code satisfies it — that is
the Validate step's job. An EARS-shaped AC is evidence of clear intent and
nothing more.

**Include negative criteria where a wrong implementation is plausible.** If the
story rules something out, say so: *THE search endpoint SHALL NOT perform
partial or contains matching.* An unstated exclusion gets implemented anyway.

### Rule 2 — Stable, addressable numbering

Every AC carries an identifier so tasks, tests, and validation can cite it.

- Flat stories: `AC-1`, `AC-2`, `AC-3` …
- Stories split into several user stories (below): `AC-2.1`, `AC-2.2` — group
  number first, criterion second.

**Identifiers are permanent.** When a story is revised and the context
regenerated, keep existing ACs on their original numbers, append new ones at the
end, and mark removed ones `AC-4: [WITHDRAWN]` rather than renumbering. A
renumber silently invalidates every downstream reference — a task that says
"implements AC-3" now points at a different requirement, and nothing surfaces
the change.

### Rule 3 — Provenance: mark what the story did not ask for

Once both are in SHALL form, a criterion the model invented reads exactly like
one the business asked for. Structured phrasing makes invented requirements look
authoritative, so provenance has to be explicit.

Every AC is one of three kinds:

| Marker | Meaning | Effect |
|---|---|---|
| *(none)* | Traceable to the story, the developer's answers, or an instruction file | Normal |
| `[ASSUMED]` | The model added this; the story never mentioned the topic | Written and flagged, does not block |
| `[NEEDS CLARIFICATION]` | A requested behaviour is under-specified and cannot be implemented without a decision | Blocks — the harness halts |

**Choosing between the two markers — the test is what the story asked for:**

- The story asks for a behaviour but leaves a dimension of it open →
  `[NEEDS CLARIFICATION]`. *The story wants author validation but never states a
  maximum length.*
- The story never mentions the topic at all and the model believes it is needed
  → `[ASSUMED]`. *The story asks for a filter dropdown; the model adds
  accessibility criteria nobody requested.*

`[ASSUMED]` is **not** an escape hatch from the clarification gate. Marking a
missing dimension of a requested behaviour as `[ASSUMED]` bypasses the gate and
ships a guess as a requirement. When genuinely torn, use
`[NEEDS CLARIFICATION]` — a halted run costs one re-run; an invented requirement
implemented as fact costs a rewrite.

**Test the marker against its own basis before writing it.** If the basis you
are about to write names the story — "story decision #2", "stated in the story",
"per the acceptance criteria above" — then the story DID mention the topic and
the criterion is NOT assumed. Remove the marker. A basis that traces to the story
and a marker that says the story never mentioned it cannot both be true, and the
contradiction is visible in a single line.

Marking a traceable criterion `[ASSUMED]` is not a harmless excess of caution. It
tells the developer to consider deleting a requirement the business actually
asked for, and it pollutes the assumed-ratio check — enough false positives and a
well-specified story trips the one-third cap for no reason.

Format, with the basis stated so a reviewer can judge it quickly:

```
- AC-7: [ASSUMED] WHILE the specialty filter has keyboard focus, THE filter
  SHALL display a visible focus indicator meeting WCAG 2.1 AA contrast.
  Basis: accessibility standard in copilot-instructions.md; not requested in the story.
```

List every assumed criterion in **Section 9 — Assumptions** as well, so a
reviewer can scan them without reading the full AC list.

**Cap: if more than a third of the ACs are `[ASSUMED]`, stop.** The story is a
seed, not a specification, and the context is mostly the model's invention. In
interactive mode, tell the developer the story needs refining first. In CI mode,
write the file and add one `[NEEDS CLARIFICATION]` line stating the story is too
thin to specify.

### When to split into multiple user stories

Split when the story genuinely covers **distinct actors or distinct concerns**,
each with its own user story line and AC group. A filter feature serving a pet
owner browsing results and a developer needing a repository method is two
concerns; say so.

Do **not** split to inflate the count. Three ACs under one honest user story
beat fifteen spread across five invented ones. Every group must trace to
something the story or the developer actually asked for — a group that exists
because the template had room for it is invention with extra structure.

---

## Workflow

### 0. Mode detection — interactive vs non-interactive (CI)

**Before anything else, determine the run mode.**

- **Interactive mode (default):** a human is present to answer questions (local
  Copilot Chat, IDE, terminal). Use the full questioning workflow in steps 1–8 below.
- **Non-interactive / CI mode:** the skill is invoked by an automated harness with
  no human to answer (e.g. GitHub Actions). **CI mode requires an EXPLICIT positive
  signal** — at least one of: a system/harness instruction stating the run is
  non-interactive or "CI mode", or the invoking prompt explicitly saying "CI mode" /
  "do not ask questions". **The mere absence of a chat channel is NOT sufficient** —
  if you are unsure, default to interactive mode and ask.

**When in non-interactive / CI mode, follow these rules instead of asking questions:**

1. **Never ask a question. Never wait for input. Never block on a human.**
2. Draft `context.md` from the story as far as it is specific enough to support.
3. For **every gap** that you would normally ask about (steps 4–5 below), do NOT
   guess and do NOT fill with vague language. Instead write one precise
   `[NEEDS CLARIFICATION]` line in **Section 8 — Clarifications Needed**, naming the
   exact missing dimension (same quality bar as the table in "Banned phrases":
   name the missing dimension, never parrot a vague phrase).
4. The banned-phrase rules still apply in full — a `[NEEDS CLARIFICATION]` line must
   describe the missing dimension, not restate a vague phrase.
5. Sections you CAN complete from the story, complete normally. Only genuinely
   ambiguous items become `[NEEDS CLARIFICATION]`.
6. **Clarifications name TECHNICAL or BEHAVIOURAL gaps only.** A `[NEEDS
   CLARIFICATION]` line is *only* for an implementation-relevant ambiguity the
   coding phase cannot proceed without: input→output mapping, match/validation
   rules, error responses, data shape, edge-case behaviour, API contract, reactive
   pattern choice, and the like. It is **NEVER** for process, project, or
   staffing metadata — do NOT raise clarifications about code owner, PR reviewer,
   responsible team, approvers, sign-off, ticket/Jira linkage, sprint, timeline,
   deadline, priority, branch/PR naming, or release process. None of these are
   answerable from a story, none block implementation, and each one halts the
   harness for nothing. If a template section asks for such metadata and the story
   doesn't supply it, leave it blank or write `N/A (not required for
   implementation)` — never a `[NEEDS CLARIFICATION]`.
7. **Any criterion you add that the story never mentioned is marked
   `[ASSUMED]`** (see "Acceptance criteria — syntax, numbering, and
   provenance"). CI mode is where invention is most dangerous: there is no human
   in the loop to notice that an AC nobody asked for has appeared in SHALL form.
   Do not use `[ASSUMED]` to sidestep a genuine `[NEEDS CLARIFICATION]` — a
   missing dimension of a *requested* behaviour always blocks.
8. Write the file immediately to `.github/story-context-files/` and stop. Do not
   ask for approval.

**Why:** a downstream harness gate scans the written context for
`[NEEDS CLARIFICATION]`. If any remain, the harness halts the run and surfaces them
to a human, who resolves them (by editing the story) and re-runs. So in CI the
clarification loop happens *between* runs, not *during* one — but ambiguity is never
silently guessed. The marker is the contract between this skill and the harness.

---


### 1. Check for copilot-instructions.md

Before reading the story, check whether `.github/copilot-instructions.md`
exists in the repo. This file holds the standard backend and frontend
constraints the team has agreed on.

- **Exists** — read it and use those constraints in section 6.
- **Missing** — show this warning verbatim to the developer (do not put
  backticks around context.md or copilot-instructions.md anywhere in this
  message — Copilot Chat tries to auto-link backticked filenames and
  produces broken vscode-file:// URLs):

  ```markdown
  > ## ⚠️ Missing copilot-instructions.md
  >
  > I don't see **.github/copilot-instructions.md** in this repo.
  >
  > **What this means:** the context.md will use generic Spring Boot
  > and Angular defaults for the Constraints section. The team's actual
  > coding standards are not being applied.
  >
  > **Recommendation:** create copilot-instructions.md before running
  > the analysis prompt. Otherwise Copilot may produce code that doesn't
  > match your conventions.
  >
  > **Continue with defaults, or pause to set up copilot-instructions.md first?**
  ```

  Continue only if the developer says yes.

### 2. Read the story and assess specificity

Read the story carefully. If a screenshot or Figma export is attached, list
the visible UI elements (fields, buttons, states, validation indicators) so
the developer can see what was extracted.

Then make a quick judgement call about how specific the story is:

- **Specific story** — concrete fields named, behaviours defined, edge
  cases hinted at, current behaviour described in detail.
- **Vague story** — uses words like "improve", "better", "faster" without
  saying *what* specifically changes. Common in stories written by POs who
  delegate the detail to the team.

If the story is **vague throughout** (more than half the sections would
need to be filled by guessing), enter **strict mode**: do not attempt to
draft context.md. Tell the developer:

> Your story is high-level — that's normal for a refinement story, but it
> doesn't have enough detail yet for a useful context.md. I'll ask 4–6
> targeted questions to fill the specifics. Each one matters, so please
> take a moment with each.

Strict mode means: ask the questions in section 4 even if the story
"covers" them at a vague level. A vague answer is functionally a missing
answer.

### 3. Identify layer and story type silently

From the story content, infer:

- **Layer** — backend (endpoint, repository, query), frontend (form, screen,
  component), or full stack (both). Ask only if genuinely ambiguous.
- **Story type** — bug fix ("fix/broken"), new development ("create/add"),
  or enhancement ("improve/extend").

State your inference back briefly so the developer can correct it:

> Read your story — looks like a full stack enhancement to owner search.
> Let me check a few specifics before I write the context.md.

### 4. Ask targeted questions for the gaps

For each section of the template, ask one question if the story doesn't
already nail it. **In strict mode, treat vague coverage as a gap.**

The questions below are the ones that matter most. Ask them one at a time,
each with a concrete example answer in the right stack — the example does
the teaching.

#### Backend questions

Each question has a completeness rubric — accept the answer only when
every dimension is filled in. Examples in italics.

- **Exact match rule** (search/filter stories).
  - Required dimensions: case sensitivity, match type (starts-with /
    contains / exact), fields searched.
  - Thin: "case-insensitive contains match" → ask "across which fields —
    lastName only, or firstName + lastName + middleName?"
  - Complete: *"case-insensitive contains match across firstName, lastName,
    and middleName"*
- **Empty / no-results behaviour**.
  - Required dimensions: HTTP status, response body shape, UI consequence.
  - Thin: "200 with empty array" → ask "and what does the UI show — empty
    state message, or stay as-is?"
  - Complete: *"200 with empty `content` array and `totalElements: 0`;
    UI shows neutral empty state"*
- **Pagination defaults**.
  - Required dimensions: page size, default sort, out-of-range behaviour.
  - Thin: "page size 20" → ask "default sort? What if the requested page
    number is past the last page — return empty content or 400?"
  - Complete: *"page size 20, sort by lastName ascending, out-of-range
    page returns 200 with empty content"*
- **Performance target** (whenever the story or developer mentions speed).
  - **Required dimensions: metric, target value, AND load context.** A
    P95 number with no load context is meaningless — P95 < 500ms at 100
    rows is trivial; at 100,000 rows is non-trivial.
  - Thin: "P95 < 500ms" → ask "at what result-set size and concurrency?
    For example: 1000-owner result set under typical clinic load
    (~10 concurrent users)?"
  - Complete: *"P95 < 500ms at 1000-owner result set under ~10 concurrent
    users (typical clinic load)"*

#### Frontend questions (any story with a UI)

These four are almost always missing from JIRA stories — but some of them
may genuinely be unchanged from current behaviour. Start with this gate:

> **Is the search/form interaction model changing in this story?**
> If yes — I need to confirm trigger, loading, empty, and error states.
> If no, keep the current behaviour — I'll mirror what Current Behaviour
> describes and skip ahead.

If the answer is "yes" or "unsure", or if the story is **new development**
(no current behaviour to mirror), ask all four below. Each has a
completeness rubric — accept the answer only when every dimension listed
is filled in.

- **Trigger model** — keystroke / Enter / button click.
  - Required dimensions: trigger event, min character count (if keystroke),
    debounce delay (if keystroke).
  - Thin: "on keystroke" → ask "after how many characters and what debounce?"
  - Complete: "on keystroke after 3 characters with 300ms debounce, plus
    a Search button for keyboard/screen-reader users"
- **Loading state** — what's shown during the in-flight request.
  - Required dimensions: visual (spinner / skeleton / disabled), placement,
    when it appears.
  - Thin: "spinner" → ask "where — over the results area, in the input?
    Shown only if the request takes > Xms?"
  - Complete: "spinner overlay on the results area, shown if the request
    exceeds 200ms"
- **Empty state** — zero results.
  - Required dimensions: message text (literal), styling category
    (error vs neutral empty state), any call to action.
  - Thin: "not found" → ask "is 'not found' the literal text? Styled as
    an error (red, alert), or as a neutral empty state? Any CTA like
    'Add new owner'?"
  - Complete: "neutral empty state with text 'No owners match your
    search.' and a secondary 'Clear search' link"
- **Error state** — API call fails (network, 5xx).
  - Required dimensions: notification type, retry mechanism, what
    happens to the user's input.
  - Thin: "show error" → ask "toast or inline? Can the user retry?
    Is their search input preserved?"
  - Complete: "toast 'Could not reach server, please retry' with a
    Retry button; search input preserved"

#### Full stack drift check

For bug fixes and enhancements, every behaviour mentioned in **Current
Behaviour** must be explicitly addressed in **Expected Behaviour** — kept,
changed, or removed. Silent drops cause Copilot to either preserve the
old behaviour by accident or remove it without anyone confirming the
change was wanted.

Walk through Current and ask about anything not addressed in Expected:

> Your current behaviour mentions [single-match results redirect to the
> detail page]. Your expected behaviour describes a list view but doesn't
> say what happens on a single match. Does the redirect stay, or do
> single matches now display in the list?

### 5. Validate every answer — vague, thin, or complete

**First, check whether the developer actually answered the question.** If
the response doesn't address the question — silence, single characters
(`*`, `* * *`, `.`), filler words (`next`, `skip`, `ok`, `sure`), emoji
only, or off-topic content — do not treat it as an answer and do not
move to the next question. Stop and ask:

> I don't see an answer to the previous question. Three options — which
> would you like?
> 1. **Take another pass** — I'll re-ask, with a different example
> 2. **Mark as NEEDS CLARIFICATION** — I'll flag it for your BA/PO and
>    move on
> 3. **Skip this section entirely** — only if the section isn't relevant
>    to your story

Wait for an explicit choice before proceeding. Empty replies are not
answers — they're missing answers, and missing answers must be
acknowledged, not silently swallowed.

Once an actual answer is received, classify it:

- **Vague** — uses language from the banned phrases table or its variants
  ("works better", "displayed properly", "performance acceptable"). Push
  back with a concrete example of what specific looks like in their stack.
- **Thin** — specific in form but missing one or more required dimensions
  from the question's rubric. Ask one follow-up for the missing dimension.
  Don't treat this as a "push back" — it's a normal continuation. Example:
  *"Got the latency target. What result-set size — 1000 owners? Concurrency?"*
- **Complete** — all dimensions filled. Accept and move on.

**Aggressive validation is the default for these teams.** Thin answers
must be sharpened, not accepted with a [NEEDS CLARIFICATION] flag. The
skill earns its keep by recognising what specific looks like — if it
accepts thin answers, developers may as well fill the template by hand.

**Two-round cap.** Ask for the same dimension at most twice. If after the
second ask the developer still can't supply it ("I genuinely don't know"),
*then* capture as `[NEEDS CLARIFICATION]` and move on. Better one
flagged item than blocking the developer.

**Developer override — scope matters.** Two override types exist; never
conflate them:

- **Per-question skip** — applies only to the current question. Triggers:
  *"skip this one"*, *"NEEDS CLARIFICATION for this"*, *"I don't know"*,
  *"mark this one"*. The skill flags this question as `[NEEDS CLARIFICATION]`
  and continues with the next question normally.
- **Global override** — applies to all remaining questions. Triggers
  (must be unambiguous): *"skip the rest"*, *"good enough, generate it"*,
  *"draft what you have"*, *"stop asking, just write the file"*. The skill
  generates the file with every remaining unanswered dimension wrapped as
  `[NEEDS CLARIFICATION]`.
- **Ambiguous phrasing** — for example *"where you don't have answer, mark
  NEEDS CLARIFICATION and move ahead"*, *"NEEDS CLARIFICATION for missing
  ones"*, *"flag what you don't know"*. **Do not act on these.** Ask:

  > To make sure I get this right — do you mean:
  > 1. Skip *only the current question* and continue asking the rest, or
  > 2. Skip *all remaining questions* and generate the file with whatever
  >    you've answered so far?

  Wait for an explicit choice. Honesty over compliance: the gaps must be
  visible, but only after the developer confirms the scope.

For all override paths, the file must still pass the Step 7 preflight —
a context.md full of clarifications is acceptable; a context.md with
banned phrases is not.

**Cap on clarifications:** if more than five `[NEEDS CLARIFICATION]` items
accumulate, stop and tell the developer:

> Five+ items in Clarifications means the story isn't ready for Copilot
> yet. Recommend going back to the BA/PO to refine before we continue —
> otherwise the context.md is mostly questions, not specifications.

### 5a. Inputs-completeness gate — before drafting

Before writing the context.md draft, run this check on the *answers
collected*, not the output:

For each question asked in step 4, walk its completeness rubric:
- Are all required dimensions present in the answer?
- If no — go back and ask for the missing dimension (within the
  two-round cap).
- If yes — proceed.

This is different from the preflight in step 7. The preflight checks the
*output* for banned phrases. This gate checks the *inputs* for completeness
*before* the output is even drafted. Both matter — one catches vagueness,
the other catches thinness.

### 6. Apply technical constraints

If `copilot-instructions.md` was found in step 1, use those constraints.
Otherwise use these defaults and remind the developer they're defaults:

**Backend (Spring Boot):** Constructor injection, standard error response
format, Jakarta Validation on inputs, JUnit 5 + Mockito for tests, paginated
list responses.

**Frontend (Angular):** Existing component library (no new design tokens),
keyboard navigation, ARIA labels on interactive elements, responsive 375px
to 1440px, Jasmine/Karma component tests.

**Full stack:** Both sets, plus *API contract changes must remain backward
compatible* unless the story explicitly says otherwise.

**Performance targets go in Constraints, not Acceptance Criteria.** A line
like "P95 response time < 500ms at 1000-owner result set under typical
clinic load" is a non-functional requirement verified by load testing,
not by a unit or integration test. Putting it in AC misleads the
developer (and Copilot) into thinking it's something to assert in test
code. Add a dedicated *Performance target* line under Constraints instead.
Same applies to: bundle size limits, memory ceilings, time-to-interactive
targets, and any other NFR.

**Always include load context with any latency target.** "P95 < 500ms"
alone is not a target — it's a half-target. The skill must capture metric,
value, AND the load conditions (result-set size and concurrency) before
the target is written into Constraints. If the developer can't supply the
load context, flag as `[NEEDS CLARIFICATION]` rather than write an
unfalsifiable performance line.

### 7. Preflight before output

Before showing context.md, scan it against this checklist. Every item
must pass — if any fail, go back to section 4 and ask one more question.

- [ ] No banned phrases anywhere (see top of skill) — including text
      inside `[NEEDS CLARIFICATION]` blocks
- [ ] No file paths, class names, component names, or table names
- [ ] Every AC has a concrete input → output mapping someone could write
      a test for
- [ ] Every behaviour mentioned in Current Behaviour is addressed in
      Expected Behaviour (kept / changed / removed) — for bug fixes and
      enhancements
- [ ] For frontend or full stack stories where the interaction model is
      changing or new: all four UX states covered with all required
      dimensions (trigger / loading / empty / error)
- [ ] Edge cases name specific scenarios ("empty input string", "lastName
      with apostrophe") not categories ("invalid input")
- [ ] Performance / bundle / memory targets are in Constraints, not in
      Acceptance Criteria, AND include load context (result-set size,
      concurrency) — not just a P95 number
- [ ] Out of Scope has at least 3 explicit exclusions
- [ ] Every AC uses an EARS pattern, is one sentence, and has at most three
      preconditions
- [ ] No AC contains a semicolon, a second IF/THEN, or an "and" joining two
      responses — each of those is two criteria and must be split
- [ ] No `[ASSUMED]` criterion has a basis that traces to the story; if the
      basis names a story clause or decision, the marker is wrong and comes off
- [ ] Every AC has a stable identifier (`AC-1`, `AC-2.1`) and no existing
      identifier has been renumbered
- [ ] Every AC not traceable to the story, the developer's answers, or an
      instruction file is marked `[ASSUMED]` with its basis stated — and no
      missing dimension of a *requested* behaviour has been marked `[ASSUMED]`
      instead of `[NEEDS CLARIFICATION]`
- [ ] `[ASSUMED]` criteria are under a third of the total AC count
- [ ] No standing glossary of pre-existing domain terms (those belong in
      instruction files) — only terms this story introduces
- [ ] Story Quality Score present, all six dimensions scored, deductions named
      where marks were lost
- [ ] Design trigger present in canonical form (`**DESIGN REQUIRED: YES**` or
      `**DESIGN REQUIRED: NO**`) with a one-line reason, naming the existing
      precedent when the answer is NO
- [ ] Feasibility verdict present in canonical form (`**VERDICT: GO**` or
      `**VERDICT: NO_GO**`), assessed only AFTER the ACs were final
- [ ] No AC was changed, narrowed, or dropped because of a feasibility finding
- [ ] Every `[BLOCKER]` names one of the four blocking classes — unclassifiable
      concerns are written as notes, not blockers

### 7a. Score the story, then assess feasibility

Two final passes, in this order, both AFTER the acceptance criteria are final.

#### Story Quality Score — reported, never gating

Score the context you have just drafted on six dimensions, 0–20 each, and write
the result into the file. The score exists to make story quality **measurable
over time** — the trend across a team's stories is the useful signal, not any
single number.

| Dimension | Full marks when | Deduct when |
|---|---|---|
| Clarity | No banned phrases; every term concrete | Vague quantifier, unresolved pronoun |
| Testability | Every AC has an observable outcome | An AC nobody could verify by running the system |
| Traceability | Every AC traces to the story, an answer, or an instruction file | Untraced criterion missing its `[ASSUMED]` marker |
| Atomicity | Every AC is one behaviour and ONE response, ≤3 preconditions | Any AC with a semicolon, a second IF/THEN, or two responses joined by "and" — deduct even if the AC reads clearly |
| Completeness | Every required section substantive | Section present but thin or generic |
| Edge coverage | Failure, empty, and boundary cases named | Only the happy path described |

**This score does not gate anything, and must not.** You are marking your own
work: a model scoring the context it just wrote will cluster near the top, so a
threshold would be a gate that never fires — which is worse than no gate,
because the log then looks like assurance. The hard stops stay where they are:
`[NEEDS CLARIFICATION]`, the `[ASSUMED]` one-third cap, and feasibility below.

Deduct honestly. A context scoring 100/100 on a thin story is a worse artifact
than one scoring 70 with the weak dimensions named, because the second tells a
developer where to look.

#### Design trigger — does this story need a technical design?

One line, decided here because this is the only phase that has read both the
story and the codebase. It gates nothing; the harness reads it to decide whether
to run the design phase at all.

Answer **YES** when any of these hold:

| Trigger | Example |
|---|---|
| Crosses a service boundary | Needs data another service owns; a new downstream call |
| Changes a published contract | New or altered endpoint, response shape, event schema |
| Introduces a pattern the repo lacks | First use of caching, messaging, streaming, a scheduler |
| Has more than one defensible structural answer | Where validation lives; extend an entity or add one |
| Touches data ownership or transaction boundaries | New table, changed persistence scope |
| Carries a backward-compatibility concern | Existing consumers must keep working |

Answer **NO** when none do — when the approach is already settled by a pattern
the codebase establishes. That is the common case, and answering NO to it is
correct rather than lazy: a design document produced for every story trains
everyone to stop reading them, so the ones that matter get skimmed too.

Give the reason either way, in one line, naming the precedent when the answer is
NO so a reviewer can disagree with it:

```
**DESIGN REQUIRED: NO**
Follows the same controller to service to CatalogClient shape as getBookById.
```

```
**DESIGN REQUIRED: YES**
Publishes a new event contract other services will consume, and the service has
no messaging pattern today.
```

**When genuinely torn, answer YES.** A short design for a story that did not
need one costs a few thousand tokens. A missing design for one that did means
the decision still gets made — silently, inside the coding phase, by a model
optimising for the immediate task, where nobody sees it and nobody weighed the
alternatives.

**Decide by the triggers, not by impression.** The same story judged twice must
get the same answer, and it will not if the decision rests on how substantial the
story happened to feel on a given reading. Walk the trigger table row by row and
answer YES if any row fires — the triggers are the decision procedure, not a
prompt for one.

Two rows are misread often enough to be worth naming:

- **"Crosses a service boundary"** means the STORY introduces a dependency on
  something outside this service. Adding a method to a client that already
  exists, already has a base URL, and already calls that service does NOT cross a
  boundary — the boundary was crossed before this story began. Answer NO on that
  row.
- **"Introduces a pattern the repo does not have"** means a pattern genuinely
  absent. A fourth endpoint shaped like three existing ones introduces nothing,
  however new the endpoint is.

State which trigger fired, by name, in the reason line. "Adds a new client method
and a reactive path" names no trigger and is the shape of an answer arrived at by
impression. If no trigger can be named, the answer is NO.

#### Feasibility — GO or NO_GO

Now assess whether **this repository** can build the story as specified. This is
a different question from clarity: a perfectly unambiguous story can still be
impossible here.

**Do this only after the ACs are final, and never edit an AC because of what you
find.** Assessing feasibility while still drafting corrupts the specification —
criteria drift toward whatever is easy to build, the context still looks clean,
and nobody can see that the story quietly shrank. If the story is not feasible,
the answer is NO_GO, never a narrowed AC.

Check, against the code you inventoried:

- Does every entity, service, client, and method the ACs need already exist, or
  is it created by this story?
- Does anything contradict a contract this service already publishes?
- Does the concern belong to this service at all?
- Does anything require a pattern the stack forbids (a blocking call in a
  reactive service, a direct DB read where a client is mandated)?

Write the verdict in the canonical form. `GO` needs one line of basis; `NO_GO`
needs at least one classified blocker:

```
## Feasibility
**VERDICT: NO_GO**
[BLOCKER]: (MISSING_DEPENDENCY) AC-3 requires CatalogClient.fetchBookByAuthor,
which does not exist and is not created by this story.
```

Only four classes may block. Prefix every `[BLOCKER]` with one:

| Class | Means |
|---|---|
| `MISSING_DEPENDENCY` | Needs an entity, service, or method this repo does not have and this story does not add |
| `CONTRACT_CONFLICT` | Contradicts an API contract this service already publishes |
| `SCOPE_MISMATCH` | The concern belongs to a different service |
| `STACK_INCOMPATIBLE` | Requires a pattern the stack or framework forbids |

**If you cannot name one of those four classes, the verdict is GO.** A NO_GO
citing an unclassified concern is downgraded to GO by the harness and merely
reported — deliberately. A halt has to rest on a checkable claim someone can
argue with, not on a misgiving. Write the concern as a note under the verdict
instead; it will be surfaced without stopping the run.

**"Hard" is not "infeasible."** A story that is large, unfamiliar, or touches
many files is a GO. NO_GO means it cannot be built here as written — the remedy
is re-scoping, sequencing behind another ticket, or moving the work to the
service that owns it, none of which the coding phase can do.

---

### 8. Write the context file

When all checks pass, write the context to a file — do not output it
only inline in chat.

**Location:** `.github/story-context-files/`

**Filename:** `STORY-DESCRIPTION-context-YYMMDD-HHMMSS.md`
where STORY-DESCRIPTION is a short lowercase hyphenated summary of the
story (2–4 words), and the timestamp is the date and time of generation
in YYMMDD-HHMMSS format.

**Example:** `.github/story-context-files/doctor-removal-context-260517-143022.md`

Never overwrite an existing context file — the timestamp ensures every
story's context is preserved even on the same branch.

After writing the file, tell the developer:

> Context written to .github/story-context-files/STORY-DESCRIPTION-context-YYMMDD-HHMMSS.md
>
> Resolve any [NEEDS CLARIFICATION] items with your BA or PO before
> running the build-prompt-steps skill.
>
> Review the [ASSUMED] criteria in Section 9 — I added those; the story
> didn't ask for them. Confirm or delete each one before building.
>
> Story Quality Score: NN/100. Feasibility: GO / NO_GO.
> A NO_GO means this repo cannot build the story as written — re-scope it,
> sequence it behind the work that adds the missing piece, or move it to the
> service that owns the concern.
>
> When you run build-prompt-steps, attach this context file to the chat.

(Do not wrap the filename in backticks in the message to the developer
— Copilot Chat auto-links backticked filenames into broken
vscode-file:// URLs.)

---

## What never goes in context.md

These four belong in the analysis prompt step that runs *after* context.md,
because Copilot needs to discover them by reading the codebase — not be told
upfront:

- File names or paths (`OwnerSearchController.java`, `appointment.component.ts`)
- Class, component, or service names
- Database table or column names
- Implementation approach ("use a new service", "add a guard", "extract a method")

**A project glossary also does not belong here.** Domain terms that outlive the
story — the ubiquitous nouns of the codebase — belong in a governed instruction
file (`applyTo: **`), defined once and auto-injected into every story. Restating
them per story is how five stories end up with five slightly different
definitions of the same noun, and the spec becomes a drift surface rather than a
source of truth.

Define a term inside context.md only when **the story itself introduces it** —
a new UI control, a new state, a new concept that does not yet exist in the
codebase. Those go in Section 1 alongside the story summary, not in a standing
glossary section.

If a developer offers any of these, redirect briefly:

> File and class discovery happens in the analysis step. Keeping context.md
> stack-agnostic lets Copilot see the whole picture before narrowing in.

---

## Visual designs (frontend / full stack)

When a screenshot or Figma export is attached:

1. Describe what you see — fields, buttons, states, copy.
2. Use it to fill Expected Behaviour and Acceptance Criteria where possible.
3. Then run through the four frontend questions (trigger, loading, empty,
   error) — designs almost never cover all four.

---

## Examples

### Calibrating acceptance criteria

| Vague (banned) | Specific — Angular | Specific — Spring Boot |
|---|---|---|
| "Search should work" | "Typing 3+ characters filters the results list within 300ms debounce; clearing the input restores the full list" | "GET /api/v1/owners?lastName=Smith returns only owners whose lastName contains 'Smith' (case-insensitive)" |
| "Form should validate" | "Submit button stays disabled until all required fields are valid; first invalid field receives focus on attempted submit" | "POST /api/v1/appointments returns 400 with field-level errors for any invalid input, no partial save" |
| "Errors handled properly" | "Network failure shows toast 'Could not reach server, please retry' and re-enables the submit button" | "Downstream timeout returns 503 with `error: 'UPSTREAM_UNAVAILABLE'`, never a 500" |
| "Performance acceptable" | "Initial render under 1s on throttled 3G; list re-renders < 100ms per filter change" | "P95 response time under 500ms at 1000-owner result set" |

### Calibrating expected behaviour

| Vague (banned) | Specific |
|---|---|
| "Search returns relevant results" | "Returns owners whose lastName contains the term, case-insensitively, paginated to 20 per page, sorted by lastName ascending" |
| "Form behaves correctly" | "On submit: validates all fields, calls POST /appointments, shows spinner during call, navigates to confirmation on 201, shows inline error toast on 4xx/5xx" |
| "Search is faster" | "Results update within 300ms of last keystroke; spinner shown if API call exceeds 200ms" |

---

## Reference files

- `assets/context-template.md` — Full template with section descriptions and
  worked examples for backend, frontend, and full stack across all three
  story types. Read this when you need to remind yourself what a finished
  context.md looks like or pull a section header.

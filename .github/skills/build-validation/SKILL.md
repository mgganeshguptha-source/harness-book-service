---
name: build-validation
description: >
  Checks the delivered code against the acceptance criteria, one criterion at a
  time, and writes a per-AC verdict to .harness/validation.md. Use when a
  developer asks whether an implementation actually meets the story, asks for an
  AC conformance check or acceptance validation, or when the harness runs its
  validation phase.

  This is NOT a code review. Code review asks "is this code correct and safe?"
  and reads the diff. Validation asks "does AC-4 hold?" and reads the criterion
  first, then goes looking for the code that satisfies it. The difference
  matters because a reviewer can honestly pass code that is well-written, safe
  and idiomatic while a criterion is silently unimplemented — nothing in the
  diff is wrong, so nothing in a diff-shaped review catches it.

  Every acceptance criterion from context.md gets exactly one verdict: MET,
  NOT_MET or UNVERIFIABLE, each with the evidence that supports it. A criterion
  with no verdict is a failure of the check, not an omission.
---

# Build Validation Skill

Takes the acceptance criteria from `context.md` and the code as delivered, and
rules on each criterion independently. Writes `.harness/validation.md`.

---

## Why validation exists separately from code review

The code review phase reads the **diff** and asks whether what is there is
correct, safe and idiomatic. It blocks on five classes of defect: correctness,
contract, reactive/concurrency safety, security, error handling.

None of those classes can catch a criterion that was never implemented. The code
that exists is fine; the problem is code that does not exist. A reviewer reading
a clean, well-structured diff has no reason to return anything but PASS, and a
story can reach a pull request with AC-4 quietly missing.

The two checks also start from opposite ends:

| | Code review | Validation |
|---|---|---|
| Starts from | The diff | The acceptance criteria |
| Asks | Is this code right? | Does this criterion hold? |
| Misses | A criterion nobody implemented | A defect in code no criterion covers |
| Output | One verdict for the change | One verdict per criterion |

They are complementary, and neither substitutes for the other. Both run.

**Validation is also the last place a specification defect can surface cheaply.**
A criterion that cannot be verified usually means it was never testable as
written — and that is a context.md problem discovered while it still costs one
edit, rather than after release when it costs an argument about what was agreed.

---

## Inputs

| Input | Where from | Required |
|---|---|---|
| `context.md` | `.github/story-context-files/` — newest | Yes |
| The delivered code | The repository, as it now stands | Yes |
| `.harness/prompt-steps.md` | The AC coverage matrix | If present |
| `.harness/design.md` | Decisions and their AC references | If present |
| Test results | Surefire reports, coverage output | If present |

---

## What this skill does not do

- **Does not judge code quality.** Style, structure, naming, idiom — all belong
  to code review. A criterion satisfied by ugly code is still satisfied, and
  saying so here duplicates a check that already ran and blurs what this verdict
  means.
- **Does not re-derive requirements.** Criteria come from context.md verbatim.
  Never reword one, never merge two, never invent a criterion the story does not
  carry. If a criterion seems wrong, that is a finding to report, not a licence
  to fix it here.
- **Does not write or modify code.** Validation observes. Fixing is another
  phase's job, and a validator that edits the thing it is judging has no
  standing.
- **Does not write tests.** A criterion with no test is an `UNVERIFIABLE`
  finding, not an invitation to go and write one.

---

## The three verdicts

Every AC gets exactly one.

### MET
The criterion holds, and you can point at what makes it hold.

Evidence must be **specific**: the class and method that implements it, the test
that exercises it, the observable behaviour. "Implemented in the service layer"
is not evidence — it is a restatement. "`BookServiceImpl.fetchByAuthor` returns
`Mono.empty()` for a blank author; `BookControllerTest.blankAuthorReturns400`
asserts 400" is evidence.

### NOT_MET
The criterion does not hold. Say precisely what is missing or wrong, and where.

Include this verdict even when the gap looks small or looks deliberate. A
criterion partially satisfied is `NOT_MET` — there is no partial credit, because
"mostly implemented" is exactly the state that ships and then surprises someone.

### UNVERIFIABLE
You cannot establish either way from what is available.

This is a real verdict, not a way of avoiding a decision. Use it when the
criterion depends on runtime behaviour nothing exercises, on an environment not
present, or on a downstream system that is stubbed. **Never record UNVERIFIABLE
because checking would take effort** — read the code first.

Say what *would* verify it. An UNVERIFIABLE with no route to resolution is a
dead end for whoever reads it next.

**Do not round UNVERIFIABLE up to MET.** A criterion nobody can check is the one
most likely to be broken, precisely because nothing has ever exercised it.

---

## Workflow

### 1. Extract every criterion

Read the newest `context.md`. Take every AC verbatim with its identifier.

Note the markers:

| Marker | What validation does |
|---|---|
| *(none)* | Validate normally |
| `[ASSUMED]` | Validate normally, **and** flag in the summary that this criterion was never requested — a developer confirming the story may want it gone rather than implemented |
| `[WITHDRAWN]` | Skip — do not validate, do not count |

**Every non-withdrawn AC gets a verdict.** No sampling, no "the rest look fine".
A validation that covers most criteria tells you nothing about the ones it
skipped, and those are where the gap will be.

### 2. Establish where each criterion should be satisfied

If `prompt-steps.md` has an AC coverage matrix, use it — it already maps each
criterion to the steps meant to deliver it, so you know where to look.

Pay attention to any criterion the matrix marked **"no step required"**. That
was a claim that existing behaviour or scope already satisfies it. Validation is
where that claim gets tested, and it is the claim most likely to be wrong,
because nobody wrote anything to make it true.

Without a matrix, locate the relevant code yourself from the criterion.

### 3. Rule on each criterion

For each AC, find the code, find the test if one exists, and decide. Work
criterion by criterion rather than file by file: reading the code first and
matching criteria to it afterwards is how an unimplemented criterion gets
overlooked, because nothing on screen is missing.

**Read the criterion literally.** If it says 400 for a blank author, a 422 is
`NOT_MET`. If it says case-insensitive, a case-sensitive match is `NOT_MET` even
though the happy path passes. The criterion is the specification; your sense of
what would be reasonable is not.

**A passing test is evidence, not proof.** Check that the test asserts what the
criterion actually requires. A test named `blankAuthorReturns400` that asserts
only "no exception thrown" verifies nothing, and cited as evidence it converts a
gap into a false MET — the worst outcome this skill can produce.

### 4. Report specification defects separately

While validating you may find that a criterion is untestable as written,
contradicts another criterion, or is ambiguous enough that two readings give
different verdicts.

Record these under **Specification findings**, distinct from the verdicts. They
are not implementation failures and must not be counted as `NOT_MET` — the code
may be perfectly correct against a defensible reading. They are defects in
context.md, and this is the cheapest moment they will ever surface.

### 5. Preflight before writing

- [ ] Every non-withdrawn AC in context.md has exactly one verdict
- [ ] Every AC id matches context.md exactly — none reworded, merged or invented
- [ ] Every MET cites specific evidence — a named class, method, or test, not a
      restatement of the criterion
- [ ] Every NOT_MET says what is missing and where
- [ ] Every UNVERIFIABLE says what would resolve it
- [ ] No verdict rests on a test that does not assert what the criterion requires
- [ ] `[ASSUMED]` criteria flagged in the summary
- [ ] No code-quality commentary — that is code review's job
- [ ] Specification defects recorded separately from verdicts

### 6. Write the file

Write `.harness/validation.md` in the structure below, then tell the developer:

> Validation written to .harness/validation.md — N met, N not met, N unverifiable.

Name the NOT_MET criteria explicitly in that message. A count alone makes it too
easy to skim past a failure.

---

## Structure of validation.md

The verdict line is machine-read by the harness gate, so its form is fixed.

```markdown
# Validation — <FEATURE-ID>

**Source:** <context filename>
**Criteria checked:** N  (met N · not met N · unverifiable N)

**VERDICT: PASS**

## Per-criterion verdicts

### AC-1 — MET
<criterion, verbatim>
**Evidence:** BookController.getBookByAuthor implements the BooksApi interface
method and returns Mono<ResponseEntity<BookResponse>>. BookControllerTest
.returnsBookForAuthor asserts a 200 and the response body.

### AC-4 — NOT_MET
<criterion, verbatim>
**Gap:** The blank-author check runs in the controller after the downstream call
rather than before it, so a blank path segment reaches the catalog service. The
criterion requires rejection before the call.
**Where:** BookController.getBookByAuthor, line ~48.

### AC-6 — UNVERIFIABLE
<criterion, verbatim>
**Why:** No test exercises the multiple-match path, and the stubbed catalog
client returns a single result for every input, so the "first result" behaviour
is never reached.
**Would resolve it:** a test with a stub returning two books, asserting the
first is returned.

## Assumed criteria

AC-6 is marked [ASSUMED] in context.md — the story never asked for it. Confirm
it is wanted before treating its verdict as meaningful.

## Specification findings

Defects in the criteria themselves, not in the code:

- AC-5 and AC-7 disagree on the response for an over-length author: AC-5 says
  400, AC-7 implies 404. The code returns 400; a different reading of AC-7 would
  make that wrong.

## Summary

<Two or three sentences: what holds, what does not, and what a human should do
next. Written for someone who will read only this paragraph.>
```

### The VERDICT line

Fixed form, one per file. The harness gate reads it:

| Line | When |
|---|---|
| `**VERDICT: PASS**` | Every criterion MET |
| `**VERDICT: CHANGES_REQUESTED**` | One or more NOT_MET |
| `**VERDICT: INCONCLUSIVE**` | No NOT_MET, but one or more UNVERIFIABLE |

**INCONCLUSIVE is not PASS.** Do not use PASS when a criterion could not be
checked — an unverified criterion is unknown, not satisfied, and collapsing the
two is how a gap reaches production wearing a green tick.

---

## Worked example

```markdown
# Validation — BOOK-1

**Source:** BOOK-1-context-260812-0914.md
**Criteria checked:** 7  (met 6 · not met 1 · unverifiable 0)

**VERDICT: CHANGES_REQUESTED**

## Per-criterion verdicts

### AC-1 — MET
WHEN a GET request is made to /books/by-author/{author}, THE book service SHALL
return Mono<ResponseEntity<BookResponse>>.
**Evidence:** BookController.getBookByAuthor overrides the BooksApi interface
method with that return type. BookControllerTest.returnsBookForAuthor asserts
200 and the body.

### AC-2 — MET
THE book service SHALL delegate the lookup to BookService without blocking.
**Evidence:** The controller calls bookService.findByAuthor and returns the Mono
unchanged. No .block() or .collectList() anywhere in the changed files.

### AC-3 — MET
THE book service SHALL match the author name case-insensitively and exactly.
**Evidence:** CatalogClientImpl.fetchBookByAuthor lowercases before comparison.
CatalogClientImplTest.matchesRegardlessOfCase covers "TOLKIEN" and "tolkien".

### AC-4 — NOT_MET
IF the author parameter is blank, THEN THE book service SHALL return 400 without
calling the catalog service.
**Gap:** Validation happens inside the reactive chain after
catalogClient.fetchBookByAuthor is invoked. A blank author therefore reaches the
downstream call before being rejected. The status returned is correct; the
"without calling the catalog service" half of the criterion is not satisfied.
**Where:** BookServiceImpl.findByAuthor — the filter is applied to the result
publisher rather than before the call.

### AC-5 — MET
IF the author exceeds 256 characters, THEN THE book service SHALL return 400.
**Evidence:** Length check precedes the downstream call in
BookServiceImpl.findByAuthor. BookServiceImplTest.rejectsOverLengthAuthor
asserts 400 and verifies the client was never called.

### AC-6 — MET
IF the catalog returns no match, THEN THE book service SHALL return 404.
**Evidence:** switchIfEmpty maps to a 404 ResponseEntity.
BookControllerTest.returns404WhenEmpty asserts the status.

### AC-7 — MET
THE book service SHALL NOT perform partial or contains matching on the author.
**Evidence:** The query uses equality on the lowercased value; no LIKE or
contains anywhere in the changed files. Marked "no step required" in the
coverage matrix — the claim holds.

## Assumed criteria

None.

## Specification findings

None.

## Summary

Six of seven criteria hold. AC-4 fails on its second clause: the blank-author
rejection returns the right status but does so after the downstream call, so the
catalog service is still contacted with a blank path segment. The fix is to move
the check ahead of the call in BookServiceImpl.findByAuthor, which is where AC-5
already does it correctly — so the pattern to follow is in the same method.
```

Note what this catches that a code review would not. Nothing in that diff is
badly written; the validation-after-call ordering is idiomatic reactive code and
returns the correct status. It fails only because the criterion says *without
calling the catalog service*, and only a check that reads the criterion first
would notice.

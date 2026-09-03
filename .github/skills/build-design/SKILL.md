---
name: build-design
description: >
  Turns an approved context.md into a technical design — the HOW, decided and
  written down before any code is planned or written. Use when a developer asks
  for a design, a technical approach, or an architecture decision for a story,
  or when the harness runs its design phase. Produces a single
  .harness/design.md: what already exists and is being touched, the chosen
  approach, the decisions taken with the alternatives rejected, contract
  changes, data and state, expected impact, and risks.

  Every decision cites the acceptance criteria it serves (AC-1, AC-2.1), so
  traceability runs unbroken from criterion to decision to task to test.

  BROWNFIELD FIRST. This designs a change to a system that already exists, so
  it starts from what is there and states what stays untouched — not from a
  blank page.

  BY EXCEPTION, not by default. Most stories follow a pattern the codebase
  already has and need no design phase. This runs when a story crosses service
  boundaries, changes a published contract, introduces a pattern the repo does
  not yet use, or carries a structural decision with more than one defensible
  answer. When none of those hold, the skill says so and writes nothing — an
  empty design document is worse than no design document, because it looks like
  a decision was made.
---

# Build Design Skill

Takes an approved `context.md` and produces `.harness/design.md`: the technical
approach, with the decisions made explicit and the alternatives recorded.

Stack assumed: **Java Spring Boot microservices** (reactive WebFlux) on the
backend, **Angular** on the frontend.

---

## Why design.md exists

`context.md` says **what** the change must do. `prompt-steps.md` says **which
tasks** deliver it. Between them sits a question neither answers: **how**, and
**why that way rather than the alternatives**.

For most stories that gap is harmless — the codebase already answers it. A new
endpoint following the same shape as four existing endpoints has no meaningful
design question, and inventing one wastes tokens and reviewer attention.

The gap matters when the answer is not obvious. Then a decision does get made —
silently, inside the coding phase, by a model optimising for the immediate task.
Nobody sees it, nobody weighed the alternatives, and it is discovered at review
time when it is expensive to reverse. What this skill does is move that decision
earlier and write it down where a human can disagree with it cheaply.

**It also creates the artifact that outlives the story.** Six months on, the
useful question is not what the code does — that is readable — but *why it was
done this way and what else was considered*. That is what a design document is
for, and it is the thing least likely to exist.

---

## Run it, or skip it — decide first

**This skill runs by exception.** Before writing anything, decide whether the
story needs a design at all.

Write a design when **any** of these hold:

| Trigger | Example |
|---|---|
| Crosses a service boundary | Needs data another service owns; introduces a new downstream call |
| Changes a published contract | New or altered endpoint, request/response shape, event schema |
| Introduces a pattern the repo does not have | First use of caching, streaming, a scheduler, a new library |
| Has more than one defensible structural answer | Where validation lives; whether to extend an entity or add one; sync vs async |
| Touches data ownership or transaction boundaries | New table, changed persistence, altered transactional scope |
| Carries a migration or backward-compatibility concern | Existing consumers must keep working across the change |

**Skip when none of them do.** A story that adds a method alongside three
identical ones, fixes a bug inside one class, or adjusts validation on an
existing field has its design already settled by the codebase.

When skipping, write **only** this to `.harness/design.md` and stop:

```markdown
# Design — <FEATURE-ID>

**NOT REQUIRED.** This story follows a pattern the codebase already
establishes: <one sentence naming the existing precedent, e.g. "the same
controller → service → client shape as getBookById">.

No cross-service call, contract change, new pattern, or open structural
decision. The approach is determined by existing convention, and
prompt-steps.md can plan directly against it.
```

Say *why* it was skipped, not just that it was. A bare "not required" is
unfalsifiable; naming the precedent lets a reviewer disagree in one line.

**When genuinely uncertain, write the design.** A short design for a story that
did not need one costs a few thousand tokens. A missing design for one that did
costs a decision made invisibly inside the coding phase.

---

## Inputs

| Input | Where from | Required |
|---|---|---|
| `context.md` | `.github/story-context-files/` — newest file | Yes |
| The repository | Read the code that will be touched | Yes |
| `copilot-instructions.md` | `.github/` — house standards | If present |
| Instruction files | `.github/instructions/` — stack conventions | If present |

**Read the code before designing.** A design written from the story alone is a
guess about a system you have not looked at. Find the existing precedents, the
classes the change touches, and the conventions already in force.

---

## What never goes in design.md

- **Acceptance criteria.** They live in context.md. Cite them by id; never
  restate them, and never alter one. A design that quietly narrows an AC to
  whatever is convenient produces a document that looks consistent while the
  requirement has shrunk.
- **Task breakdowns or ordering.** That is `prompt-steps.md`. Design decides the
  approach; the plan sequences the work.
- **Code.** Method signatures and interface changes yes — implementation bodies
  no. If you are writing a method body, you are coding, not designing.
- **Restated project conventions.** "Use constructor injection" belongs in an
  instruction file. Reference it; do not copy it. Copied conventions drift from
  the governed original and nobody notices which is authoritative.
- **Effort estimates or timelines.** Not a technical decision.

---

## Workflow

### 1. Read the inputs

Read the newest `context.md` in full. Extract:

- Every AC with its identifier — you will cite these
- Any AC marked `[ASSUMED]`, which is a criterion nobody asked for and may
  disappear when the developer reviews it
- The feasibility verdict and any `[BLOCKER]` notes
- Constraints and out-of-scope items

Read `copilot-instructions.md` and the instruction files relevant to the stack.

**If context.md still has open `[NEEDS CLARIFICATION]` items, stop.** Designing
around an unresolved question means picking an answer to it silently, which is
exactly what the clarification gate exists to prevent.

### 2. Inventory what already exists

Before proposing anything, establish what is there:

- The classes, interfaces and endpoints the change touches
- The existing precedent — the closest thing the codebase already does
- Who owns the data involved
- What contracts this service already publishes that the change could break

This is what makes a brownfield design different from a greenfield one, and it
is the section most often skipped. A design that does not say what exists cannot
credibly say what changes.

### 3. Decide the trigger

Apply the table in *"Run it, or skip it"*. If no trigger fires, write the
NOT REQUIRED file and stop.

### 4. Make the decisions

For each open question, record: what was chosen, which ACs it serves, what else
was considered, and why the alternatives lost.

**The alternatives are the point.** A decision without them is an assertion —
a reader cannot tell whether options were weighed or the first idea was taken.
Recording the rejected option is also what makes the decision reversible later,
when the constraint that killed it has changed.

**State the trade-off honestly.** Every real decision costs something. A
decision recorded with no downside is either trivial or under-examined, and
writing "no drawbacks" is almost always the latter.

### 5. Assess impact and risk

List the files expected to change, and — just as important — what stays
untouched. The second half is what makes scope creep visible when the coding
phase writes somewhere unexpected.

Record risks plainly: what could go wrong, what is unverified, what assumption
the design rests on. A design with no risks section has not been examined.

### 6. Preflight before writing

- [ ] Every decision cites at least one AC id, or states plainly that it is
      structural and serves all of them
- [ ] Every AC id cited exists in context.md — none invented or renumbered
- [ ] Every decision names at least one rejected alternative and why
- [ ] Every decision states its trade-off
- [ ] The existing-system section names real classes, endpoints or tables read
      from the repository — not plausible-sounding invented ones
- [ ] No acceptance criteria restated, altered, or narrowed
- [ ] No task ordering, no implementation code, no estimates
- [ ] Anything the design needs but could not determine is written as
      `[NEEDS CLARIFICATION]`, not quietly assumed
- [ ] Sections with nothing to say are marked `Not applicable — <reason>`,
      never padded

**On empty sections:** say why they are empty. "Data & state: not applicable —
this story adds no persistence and changes no entity" is informative. A section
padded with generic prose to look complete is worse than an absent one, because
it hides that nothing was decided.

### 7. Write the file

Write to `.harness/design.md`, overwriting any previous version. Then tell the
developer:

> Design written to .harness/design.md — N decisions recorded.
>
> Review the decisions and the rejected alternatives before prompt-steps
> runs. A decision you disagree with is far cheaper to change now than
> after the code exists.

If any `[NEEDS CLARIFICATION]` was raised, name it explicitly in that message.

---

## Structure of design.md

```markdown
# Design — <FEATURE-ID>: <short title>

**Source:** <context filename>
**Design required because:** <which trigger fired, one line>

## 1. Existing system

What is there today that this change touches. Name real classes, interfaces,
endpoints, tables. Include the closest existing precedent and what the change
must not break.

## 2. Approach

The chosen approach in prose — a reader should understand the shape of the
change from this section alone, without reading the decisions.

## 3. Decisions

### D1 — <the question being decided>
**Serves:** AC-2, AC-5
**Chosen:** <what was decided>
**Why:** <the reasoning>
**Alternatives considered:**
- <option> — rejected because <reason>
- <option> — rejected because <reason>
**Trade-off:** <what this costs>

### D2 — ...

## 4. Contract changes

New or altered interface methods, endpoints, request/response shapes, event
schemas. Signatures only — no bodies. State explicitly when nothing changes,
and note any backward-compatibility obligation.

## 5. Data & state

Entities, ownership, transaction boundaries, persistence. Mark
`Not applicable — <reason>` when the story touches none.

## 6. Impact

**Expected to change:** <files or components>
**Explicitly unchanged:** <what stays as it is — including anything a reader
might assume is in scope>

## 7. Risks

What could go wrong, what is unverified, what assumption this rests on.

## 8. Open questions

`[NEEDS CLARIFICATION]: <the specific missing decision>` — or "None."
```

---

## Worked example — a design that was needed

```markdown
# Design — BOOK-2: publish book-availability events

**Source:** BOOK-2-context-260815-1042.md
**Design required because:** introduces a messaging pattern the service does
not currently use, and publishes a contract other services will consume.

## 1. Existing system

`BookService` (sample-book-service-application) serves reads through
`CatalogClient`, a WebClient wrapper over the catalog service. All current
downstream interaction is synchronous request/response over HTTP; the service
publishes no events and has no broker dependency.

`BooksApi` is the generated interface in sample-book-service-openapi-code and
is write-excluded — contract changes are made in the spec, not by hand.

Closest precedent: none. This is the service's first publisher.

Must not break: the three existing read endpoints and their consumers.

## 2. Approach

Publish availability changes as domain events to a Kafka topic owned by this
service, emitted from the service layer after a successful state change rather
than from the controller. Consumers subscribe; no synchronous callback is
added. The event carries the book identifier and the new availability state,
not the full book — consumers already have read access for the rest.

## 3. Decisions

### D1 — Where the event is emitted from
**Serves:** AC-1, AC-3
**Chosen:** the service layer, after the state change succeeds.
**Why:** the controller should not know the service publishes events, and
emitting before the change commits would announce a state that may not exist.
**Alternatives considered:**
- Controller — rejected: leaks a persistence concern into the web layer and
  makes the event unreachable from any non-HTTP caller.
- Repository/entity listener — rejected: fires on every write including ones
  that are not domain-meaningful, and is hard to test in isolation.
**Trade-off:** a caller that bypasses the service layer will not emit. Accepted
because nothing currently does, and the write-boundary gate would flag it.

### D2 — Event payload shape
**Serves:** AC-2
**Chosen:** identifier plus new state only.
**Why:** keeps the contract narrow, so consumers do not become coupled to
fields they do not need and this service can evolve the rest of the model.
**Alternatives considered:**
- Full book representation — rejected: every field becomes part of a published
  contract, and changing any of them becomes a breaking change.
**Trade-off:** a consumer needing more must make a read call. Accepted: the
consumers named in the story already hold read access.

## 4. Contract changes

New published event `BookAvailabilityChanged` on topic
`book-service.availability.v1`:

    { bookId: string, available: boolean, occurredAt: ISO-8601 }

No HTTP contract changes. No change to BooksApi.

## 5. Data & state

No new entity, no schema change. Availability already exists on the book
record; this publishes transitions of it rather than storing anything new.

## 6. Impact

**Expected to change:** BookServiceImpl (emit), a new publisher component, the
service's messaging configuration, the topic in infrastructure config.
**Explicitly unchanged:** BookController, BooksApi and the generated module,
CatalogClient, every existing read path.

## 7. Risks

- No broker exists in this service's deployment yet — the infrastructure
  dependency is real and outside this story's control.
- At-least-once delivery means consumers must be idempotent. That obligation
  belongs to them and is documented in the event contract, not enforced here.
- Emitting after commit means a crash between commit and publish loses the
  event. Accepted for this story; an outbox pattern would remove it and is
  noted as a follow-up rather than built now.

## 8. Open questions

None.
```

---

## Worked example — a design that was not needed

```markdown
# Design — BOOK-1

**NOT REQUIRED.** This story follows a pattern the codebase already
establishes: the same controller → service → CatalogClient shape as
getBookById, on the same interface, with the same reactive conventions.

No cross-service call, contract change, new pattern, or open structural
decision. The approach is determined by existing convention, and
prompt-steps.md can plan directly against it.
```

This is the common case. Recognising it is as valuable as writing a good
design — a design phase that produces a document for every story trains
everyone to stop reading them.

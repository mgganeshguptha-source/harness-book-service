# Plan for: Cache catalog book lookups (BOOK-2)

**Source:** cache-catalog-book-lookups-context-260903-081928.md + .github/copilot-instructions.md
**Stack:** Backend — Spring Boot, WebFlux (reactive)
**Total steps:** 9
**Unresolved clarifications:** None

---

## Acceptance criteria coverage

| AC | Criterion (abbreviated) | Covered by |
|---|---|---|
| AC-1 | repeated lookup within 10m served from cache (no downstream call) | Step 5 |
| AC-2 | miss calls CatalogClient and stores successful non-empty result | Step 4 |
| AC-3 | expired (>10m) entry triggers downstream call and refresh | Step 6 |
| AC-4 | empty/not-found not cached; later lookup calls downstream | Step 4, Step 7 |
| AC-5 | downstream error not cached and propagates | Step 4, Step 8 |
| AC-6 | no blocking Reactor calls introduced | Step 3 (design), Step 9 (convention drift) |
| AC-7 | controllers unchanged | Step 2 (verify) |
| AC-8 | unit tests with StepVerifier covering hit/miss/expiry/empty/error; 90% line coverage for changed classes | Step 8 |

Every AC is assigned to one or more steps above. None are omitted.

---

## Before you execute any step

1. Keep cache-catalog-book-lookups-context-260903-081928.md in your Copilot Chat context throughout the plan.
2. .github/copilot-instructions.md is available to Copilot; ensure it is present in the repo before asking Copilot to generate code.
3. Execute steps in order. Step 1 confirms the impacted file set. Do not implement code until Step 3 (design) is chosen.
4. This is the planning phase only: do NOT create or edit any .java or test source files now. Implementation will follow this plan in the coding phase. The only file written by this run is this plan file.

---

## Pre-flight

Assumptions (must be true or the plan must be revised):

1. Stack assumption: Backend uses Spring Boot with Reactor (WebFlux) and Spring DI. Existing CatalogClient interface and CatalogClientImpl (WebClient backed) are present in sample-book-service-application and BookServiceImpl injects CatalogClient by constructor.
2. Behaviour preservation: Controller behaviour (BookController) remains unchanged; BookServiceImpl signature remains unchanged (it still injects CatalogClient). Caching is transparent to controllers (AC-7).
3. Non-functional handling: TTL and capacity constraints (10 minutes, 1000 entries, LRU) are treated as functional constraints verified by unit tests and design review; memory/heap considerations will be documented in PR description.
4. No assumed ACs beyond context.md — no `[ASSUMED]` criteria discovered.

If any assumption is false, stop and update the context file or raise the issue before coding.

---

## Impacted Files

(Seeded by Step 1 inventory. Do not change IDs after Step 1.)

| ID | Path | Role |
|----|------|------|
| F1 | sample-book-service-application/src/main/java/com/example/book/controller/BookController.java | Controller — must remain unchanged (AC-7) |
| F2 | sample-book-service-application/src/main/java/com/example/book/service/BookService.java | Service interface |
| F3 | sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java | Service implementation — currently injects CatalogClient and calls fetchBook |
| F4 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java | CatalogClient interface (fetchBook signature) |
| F5 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java | Existing downstream caller (WebClient-backed) |
| F6 | sample-book-service-application/src/main/java/com/example/book/webclient/CachingCatalogClient.java | NEW: proposed caching decorator implementing CatalogClient (adds cache between BookService and CatalogClientImpl) |
| F7 | sample-book-service-application/src/test/java/com/example/book/CachingCatalogClientTest.java | NEW: unit tests for cache (StepVerifier) covering hit/miss/expiry/empty/error |

> Notes:
> - F6 and F7 are new files the implementation phase will add. All other files are existing and will be referenced only to confirm wiring and to add tests.
> - Full paths appear only here and in Step 1's seed prompts (per plan rules). Later steps will refer to files by ID (F1..F7).

---

## Step 1 — Inventory: confirm impacted files and non-code artifacts

**Goal:** Confirm the exact file set the change touches (source, tests, and any required non-code files such as migration scripts — none expected here). Populate the Impacted Files table above (IDs F1..F7). This step is enabling only.
**Implements:** — (enabling step, no AC)
**Depends on:** —

**Suggested prompt:**

> Planning from: cache-catalog-book-lookups-context-260903-081928.md
>
> Produce a concise inventory of files that must be read or changed to implement an in-memory reactive TTL + capacity-limited (LRU) cache for CatalogClient.fetchBook(String bookId). Start with these seed paths (they may be adjusted):
> - sample-book-service-application/src/main/java/com/example/book/controller/BookController.java
> - sample-book-service-application/src/main/java/com/example/book/service/BookService.java
> - sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java
> - sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java
> - sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java
>
> Add any additional source, config, or test files you genuinely need (including test classes, DI/config classes, or helper utilities). For each file you list, include one-line: current role, why it is required, and whether it will be modified or only read. Do not propose code edits yet — only list files. Confirm whether a new file implementing a CatalogClient decorator (CachingCatalogClient) is appropriate. Also check for existing cache libraries used elsewhere in the repo and list them if present.

**Review checkpoint:** Confirm the Impacted Files table above matches the inventory output. If additional files are required (e.g., a config class to register the decorator) add them as new IDs and update later steps.

---

## Step 2 — Design decision: caching approach and API surface

**Goal:** Choose the caching approach that is reactive, non-blocking, memory-bounded (LRU), and integrates cleanly with existing wiring so controllers remain unchanged.
**Implements:** — (enabling step, no AC)
**Depends on:** Step 1

**Suggested prompt:**

> Starting from cache-catalog-book-lookups-context-260903-081928.md and the confirmed file list (F1..F5), propose 3 implementation options to add a reactive in-memory cache for CatalogClient.fetchBook(bookId):
> 1) A CatalogClient decorator class (CachingCatalogClient implements CatalogClient) that wraps the existing CatalogClientImpl and is registered as the CatalogClient bean (preferred, non-invasive).
> 2) A separate CacheService injected into BookServiceImpl and called explicitly by BookServiceImpl before calling CatalogClient (more invasive, changes service code).
> 3) Use a third-party cache (Caffeine) with an adapter exposing Reactor types (explain reactive integration, e.g., use Caffeine's AsyncLoadingCache + Mono.fromFuture or a custom non-blocking adapter).
>
> For each option, list pros, cons, how it satisfies ACs (notably AC-6 and AC-7), and recommend one. The recommendation must follow repo conventions (constructor injection, small controller changes disallowed). If recommending Caffeine, show a small pseudo-signature for the adapter method that returns Mono<CatalogBookDto>. Do not write actual Java code yet — options only.

**Review checkpoint:** Pick the recommended option. The plan below assumes the chosen approach is Option 1: a CatalogClient decorator (CachingCatalogClient) registered as the CatalogClient bean so BookServiceImpl and controllers are unchanged.

---

## Step 3 — Design: cache semantics, concurrency, and reactive contract

**Goal:** Capture the exact cache semantics and method signatures to implement (TTL, LRU, no-empty caching, error handling, and concurrent in-flight dedup semantics if implemented). Ensure reactive contract preserves Mono semantics and does not block.
**Implements:** — (enabling step, no AC)
**Depends on:** Step 2

**Suggested prompt:**

> Using the decorator approach (CachingCatalogClient implementing CatalogClient), produce a design doc snippet with the following:
> - class name and package: F6 (com.example.book.webclient.CachingCatalogClient)
> - constructor signature: CachingCatalogClient(CatalogClient delegate) — constructor injection
> - the CatalogClient.fetchBook signature (copy verbatim from F4) and the intended behavior when called:
>   - If a fresh entry exists for bookId (written < 10 minutes ago), return Mono.just(cachedValue) without calling delegate.
>   - On cache miss: call delegate.fetchBook(bookId) and on success if non-empty, write to cache and return the Mono result. If the delegate returns an empty/404-equivalent result, do not cache and return empty.
>   - On delegate error: propagate error, do not cache.
> - Cache properties: TTL = 10 minutes write-based; maxSize = 1000; eviction = LRU.
> - Reactive integration: prefer Caffeine AsyncLoadingCache with values stored as the underlying DTO, and adapt to Reactor via Mono.fromFuture. If choosing Caffeine, show the adapter method signature: Mono<Optional<CatalogBookDto>> getCached(String bookId, Supplier<CompletableFuture<Optional<CatalogBookDto>>> loader).
> - Concurrency: document an optional in-flight dedup approach (single upstream call for concurrent misses) and how to test it; mark it as optional but recommended to reduce thundering-herd.
>
> Return the proposed method signatures and a short before/after wiring snippet showing how Spring DI will return the caching decorator as the CatalogClient bean (no controller changes).

**Review checkpoint:** Confirm the method signatures and reactive adaptation mechanism (Caffeine Async + Mono.fromFuture or custom non-blocking map) are acceptable. If not, revise.

---

## Step 4 — Implementation plan (developer prompt): add the caching decorator class (F6)

**Goal:** Implement the CachingCatalogClient decorator that implements CatalogClient and provides reactive caching per the design.
**Implements:** AC-2, AC-4, AC-5, AC-6 (partial — design enforces non-blocking usage)
**Depends on:** Step 2, Step 3

**Suggested prompt (paste into Copilot Chat when implementing):**

> Using the design approved earlier (cache-catalog-book-lookups-context-260903-081928.md + the design in Step 3), implement the new class F6: com.example.book.webclient.CachingCatalogClient that:
>
> - Implements CatalogClient (F4) and is constructed with the delegate CatalogClient (constructor injection).
> - Uses a non-blocking, capacity-limited cache with TTL=10 minutes and maxSize=1000 (LRU eviction). You may use Caffeine's AsyncLoadingCache adapted to Reactor (Mono.fromFuture) or a custom non-blocking ConcurrentMap with timestamp eviction and an atomic in-flight map to dedupe concurrent loads.
> - Behavior details:
>   - On lookup: if cache hit and entry age < 10 minutes, return Mono.just(cachedValue).
>   - On miss: call delegate.fetchBook(bookId); if result is present/non-empty, store in cache and return result; if empty, do not cache; if error, propagate and do not cache.
> - Important: do NOT call .block(), .collectList(), or any blocking API. Use Mono/Flux adapters only.
> - Register this class as the CatalogClient bean so BookServiceImpl continues to inject CatalogClient and is unchanged. Use @Primary or explicit @Bean wiring as appropriate. Use constructor injection everywhere.
>
> Provide thorough unit tests (see Step 8) after implementing.
>
**Review checkpoint:** Implementation compiles and adds no blocking Reactor calls. The decorator is wired as the CatalogClient bean and BookController behavior is unchanged.

**Developer note (to include in PR):** provide the following before/after wiring snippet in the PR description (do not apply here):

Before (BookServiceImpl constructor unchanged):

```java
// BookServiceImpl.java (excerpt)
public BookServiceImpl(CatalogClient catalogClient, /* other deps */) {
    this.catalogClient = catalogClient;
}
```

After (wiring via @Configuration or @Primary on decorator):

```java
// CachingCatalogClient.java (new)
public class CachingCatalogClient implements CatalogClient {
    private final CatalogClient delegate;
    public CachingCatalogClient(CatalogClient delegate) { this.delegate = delegate; }
    // fetchBook(...) implements caching logic as designed
}

// Bean registration (example)
@Bean
@Primary
public CatalogClient cachingCatalogClient(CatalogClientImpl real) {
    return new CachingCatalogClient(real);
}
```

---

## Step 5 — Unit test design: hit and miss tests (F7)

**Goal:** Add unit tests that verify cache hit and miss behaviors using mocked CatalogClient and StepVerifier.
**Implements:** AC-1, AC-2, AC-6 (assert no blocking), AC-7 (controllers unchanged via indirect verification)
**Depends on:** Step 4

**Suggested prompt:**

> Create unit tests in F7 (CachingCatalogClientTest) that:
> - Mock the delegate CatalogClient to return a Mono with a CatalogBookDto for a given bookId.
> - On first call (cache miss), assert that delegate.fetchBook was invoked and the returned Mono emits the expected value.
> - On second call within TTL, assert that delegate.fetchBook is NOT called again and the cached value is returned. Use StepVerifier to subscribe and assert.
> - Ensure tests do not call blocking APIs and mock returns are provided as Mono.just(...).
>
> Test skeleton (to be implemented):
>
> ```java
> @Test
> void cacheHit_returnsCachedValueWithoutCallingDelegateAgain() {
>   // given mocked delegate returns Mono.just(book)
>   // when first call -> StepVerifier.assertNext(book)
>   // when second call -> verify(delegate.fetchBook called only once)
> }
> ```

**Review checkpoint:** Tests assert delegate invocation counts and use StepVerifier; no blocking calls appear in test code or implementation.

---

## Step 6 — Unit test design: expiry behavior

**Goal:** Verify entries older than 10 minutes are treated as expired and a subsequent lookup refreshes the cache (calls delegate again).
**Implements:** AC-3, AC-6
**Depends on:** Step 4, Step 5

**Suggested prompt:**

> Add a unit test that simulates passage of time to trigger expiry. If using Caffeine, prefer the library's Scheduler/test utilities or inject a Clock/TimeProvider into CachingCatalogClient so tests can advance time deterministically. The test must:
> - Insert an entry (via a mocked delegate) and verify it's returned while fresh.
> - Advance the injected clock by >10 minutes.
> - Call again and assert the delegate was invoked a second time and the cache refreshed.
>
> If using an injected Clock, show the constructor change in F6's design to accept Clock for testability. Do not edit production code here — this is a design note for implementation.

**Review checkpoint:** Tests deterministically simulate expiry without Thread.sleep or blocking.

---

## Step 7 — Unit test design: empty/not-found and error paths

**Goal:** Verify that empty/not-found responses are not cached (AC-4) and downstream errors propagate and are not cached (AC-5).
**Implements:** AC-4, AC-5, AC-6
**Depends on:** Step 4

**Suggested prompt:**

> Add tests that:
> - Mock delegate.fetchBook(bookId) to return Mono.empty() (or an Optional.empty / 404 mapping used in code) and assert that subsequent calls still invoke the delegate (no caching of empty).
> - Mock delegate.fetchBook(bookId) to return Mono.error(new RuntimeException("downstream")) and assert that the error propagates via StepVerifier and that no cache entry is stored.
>
> Include verification of delegate invocation counts.

**Review checkpoint:** Empty responses and errors are not stored in the cache; tests use StepVerifier and no blocking.

---

## Step 8 — Coverage and wiring tests; verify controllers unchanged

**Goal:** Ensure changed classes reach at least 90% line coverage and that BookController wiring is unaffected (AC-7, AC-8).
**Implements:** AC-7, AC-8
**Depends on:** Steps 4..7

**Suggested prompt:**

> Create or update tests to reach coverage targets:
> - Unit tests for CachingCatalogClient (F7) should cover happy path, hit, miss, expiry, empty, error — aim for >90% line coverage for F6 and related small wiring.
> - A lightweight integration-style unit test (mocking CatalogClient) that exercises BookController -> BookServiceImpl -> CatalogClient (decorator) to confirm controllers behave identically (status codes and payloads unchanged). Do not start the full server; use WebTestClient bound to the controller with mocked BookService to assert no behavioural change.
>
> Make sure tests use StepVerifier where appropriate and do not use blocking calls. If coverage tooling is already in repo, run it locally to confirm thresholds.

**Review checkpoint:** Coverage reports show >=90% for changed classes; controller behaviour tests pass.

---

## Step 9 — Convention drift review and PR notes

**Goal:** Review all changed files (F6 + tests) for adherence to repo conventions (.github/copilot-instructions.md) and OWASP/HIPAA/logging rules. Prepare PR description and verification checklist. This is a final review step before opening a PR.
**Implements:** AC-6 (final confirmation of no blocking), AC-8 (PR readiness), AC-7 (confirm controller unchanged)
**Depends on:** Steps 4..8

**Suggested prompt:**

> Review the changed files for:
> - Constructor injection for all new components (no field injection).
> - No blocking Reactor calls (.block(), .collectList() etc.) anywhere in the request path.
> - Proper exception propagation for downstream errors (no swallowing or wrapping that changes semantics).
> - No logging of PHI or sensitive values; include correlationId on error ProblemDetails if applicable (follow existing controller advice).
> - If using Caffeine AsyncLoadingCache + CompletableFuture -> Mono.fromFuture adaptation, ensure errors map to Reactor errors (no silent suppression).
>
> Produce a short PR checklist to copy into the PR body (files changed, reasoning, test summary, memory considerations, optional in-flight dedup discussion). Also include the exact list of files edited (F6 path and F7 path) and the suggested commit title and message.

**Review checkpoint:** Confirm conventions and readiness; update PR description with the before/after wiring snippet from Step 4 and include guidance on optional runtime tuning (maxSize, TTL) and memory tradeoffs.

---

## Done criteria

Before opening a PR, confirm all of the following:

- [ ] F6 (CachingCatalogClient) implemented and registered as the CatalogClient bean via constructor-injected wiring (no controller changes). Include the before/after wiring snippet in PR description.
- [ ] Unit tests (F7) implemented and passing locally: hit, miss, expiry, empty-not-cached, and error paths; use StepVerifier and mocked CatalogClient.
- [ ] No blocking Reactor calls introduced (search for .block(), .collectList(), .toFuture().get(), Thread.sleep() in changed files).
- [ ] Empty/not-found responses are not cached; errors propagate unchanged.
- [ ] Coverage: changed classes (F6 and related lines) meet >= 90% line coverage.
- [ ] PR description includes rationale, memory considerations for a 1000-entry cache, and optional in-flight dedup rationale if implemented.
- [ ] Code follows repo rules: constructor injection, SLF4J logging patterns, no PHI logging, and passes repository linters/tests.

---

## Implementation notes and suggested file-level snippets (for developer to include in PR)

1) New class (F6) — proposed signature and key method (text only; do not apply in planning phase):

```java
package com.example.book.webclient;

public class CachingCatalogClient implements CatalogClient {
    private final CatalogClient delegate;
    // consider injecting Clock for testable TTL behavior
    public CachingCatalogClient(CatalogClient delegate /*, Clock clock */) {
        this.delegate = delegate;
    }

    @Override
    public Mono<CatalogBookDto> fetchBook(String bookId) {
        // pseudocode: check async cache; on miss call delegate.fetchBook(bookId)
        // on non-empty success, write to cache; on empty do not cache; on error propagate
    }
}
```

2) Example bean wiring snippet (PR description):

```java
@Bean
@Primary
public CatalogClient catalogClientCaching(CatalogClientImpl real) {
    return new CachingCatalogClient(real);
}
```

3) Test skeletons (F7) — examples to implement using StepVerifier:

```java
@Test
void missThenHit_withinTTL() {
  // mock delegate.fetchBook -> Mono.just(book)
  // StepVerifier: first subscription yields book and mock called once
  // second subscription yields book and mock still called only once
}

@Test
void expiry_afterTTL_callsDelegateAgain() {
  // use injected Clock or Caffeine test scheduler to advance time >10m
}

@Test
void emptyNotCached_and_errorPropagates() {
  // mock delegate.fetchBook -> Mono.empty() and Mono.error(...)
}
```

---

## Notes on optional in-flight deduplication

- Recommended but optional: maintain a ConcurrentHashMap<bookId, Mono<CatalogBookDto>> of in-flight loads so concurrent misses share the same upstream call. If implemented, ensure the map entry is removed on terminal signal (onError/onComplete) and avoid memory leaks.
- Tests should include a concurrency test that fires parallel requests and verifies delegate.calledOnce behavior. This test is helpful but may be flaky if not written with determinism; prefer using virtualized schedulers or explicit hooks in the loader.

---

## Final message

Plan written to .harness/prompt-steps.md

To execute: open this file and follow steps 1..9 in order. Each step's Suggested prompt is crafted to paste into Copilot Chat (or your IDE assistant) when implementing. Keep cache-catalog-book-lookups-context-260903-081928.md attached to any Copilot session used to implement these steps.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-09-03T09:08:07
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CachingCatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-application/src/test/java/com/example/book/CachingCatalogClientTest.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CachingCatalogClient.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-09-03T09:08:07

## Section 1 — What Are We Trying to Achieve
Reduce load on the catalog service by caching book lookups in the book service. Cache only CatalogClient.fetchBook(String bookId) results to avoid repeated downstream calls for the same book. The cache must be reactive, in-memory, TTL-based (10 minutes), LRU-evicted at capacity 1000, and sit between BookService and CatalogClient so controllers are unchanged.

New terms introduced by this story:
- Cache entry: the stored result of CatalogClient.fetchBook(bookId).
- LRU cap: least-recently-used eviction when cache reaches 1000 entries.

---

## Section 2 — Current Behaviour
The service currently has no caching. Every request to fetch a book calls CatalogClient.fetchBook(bookId) and returns the downstream result. No cache abstraction, no eviction policy, and no in-memory cache exist in the codebase per the story.

---

## Section 3 — Expected Behaviour
Lookups through CatalogClient.fetchBook(bookId) are served from an in-memory cache when a fresh entry exists. On a cache miss, the call falls through to CatalogClient.fetchBook(bookId), and a successful (non-empty) result is stored in the cache. Cache entries expire 10 minutes after being written. Capacity limited to 1000 entries with LRU eviction. Empty (not-found) catalog responses are not cached. Catalog errors are not cached and propagate unchanged. All behavior must remain reactive (no blocking calls introduced). Controllers remain unchanged.

---

## Section 4 — Acceptance Criteria
- AC-1: WHEN a second lookup for the same bookId occurs within 10 minutes of a successful cached write, THEN THE book lookup SHALL be served from the cache and SHALL NOT call CatalogClient.fetchBook.

- AC-2: WHEN a lookup for a bookId is not present in the cache, THEN THE service SHALL call CatalogClient.fetchBook(bookId) and SHALL store the successful non-empty result in the cache.

- AC-3: IF a cached entry for a bookId is older than 10 minutes, THEN THE subsequent lookup SHALL call CatalogClient.fetchBook(bookId) again (cache miss due to expiry) and SHALL refresh the cache entry on success.

- AC-4: IF CatalogClient.fetchBook(bookId) returns an empty/not-found result, THEN THE service SHALL NOT cache the empty result and a later lookup SHALL call CatalogClient.fetchBook(bookId) again.

- AC-5: IF CatalogClient.fetchBook(bookId) fails with an error, THEN THE service SHALL NOT cache anything and SHALL propagate the error unchanged.

- AC-6: THE implementation SHALL NOT introduce any blocking Reactor calls (e.g., no .block(), no .collectList() on the request path).

- AC-7: THE controller layer SHALL be unchanged by this story (the cache sits between BookService and CatalogClient).

- AC-8: Unit tests using StepVerifier SHALL exercise hit, miss, expiry, empty-not-cached, and error paths; changed classes SHALL reach at least 90% line coverage.

---

## Section 5 — Edge Cases
- Rapid concurrent requests for the same missing bookId: first request triggers downstream call and populates cache; concurrent callers should not cause duplicate catalogue calls if a cache population in-flight deduplication is implemented (implementation detail — OK to document in tests). Basis: avoid thundering-herd on miss.
- Exact bookId keying: bookId is used as-is; no normalization; different string variants are distinct keys.
- Very frequent lookups causing eviction: least-recently-used eviction must remove the least recently accessed entries when capacity 1000 is reached.
- Empty / not-found responses: must not be cached; repeated lookups continue to call catalog until a successful non-empty result is returned.
- Downstream transient errors: errors propagate; no stale fallback is used.
- System restart: in-memory cache is ephemeral — cold start behaves as cache-empty.

---

## Section 6 — Constraints
- Reactive stack (Spring WebFlux / Reactor): cache implementation must be non-blocking and compatible with Reactor types.
- Constructor injection for any new component (no field injection).
- Unit tests: JUnit 5 + Reactor StepVerifier; CatalogClient must be mocked; tests make no external calls.
- Do not modify generated OpenAPI modules or OpenAPI specs.
- TTL: 10 minutes after write; Capacity: 1000 entries; Eviction: LRU.
- Do not implement distributed caching or metrics/monitoring as part of this story.

---

## Section 7 — Out of Scope
- Caching author lookups or any CatalogClient methods other than fetchBook(String bookId).
- Distributed/shared caching across service instances.
- Cache metrics, monitoring dashboards, admin or invalidation endpoints.
- Any change to the catalog service itself.
- Editing OpenAPI spec or generated modules.
- Introducing resilience policies such as retries/circuit-breakers as part of the cache change.

---

## Section 8 — Clarifications Needed
_None — the story provided explicit decisions resolving prior clarifications._

---

## Section 9 — Assumptions
- No backward-compatibility change to controller contracts is required; controllers remain unaware of the cache per explicit decision.
- In-flight deduplication on cache-miss is acceptable as an implementation detail to avoid duplicate downstream calls, but not required by the ACs; tests may assert reasonable behavior for concurrent miss scenarios.

---

## Section 10 — Story Quality Score
| Dimension | Score | Basis |
|---|---|---|
| Clarity | 18/20 | Story is specific; TTL, capacity, and key are defined. Minor deduction: thundering-herd mitigation left as implementation detail rather than explicit requirement. |
| Testability | 20/20 | ACs map directly to observable behaviors and tests using StepVerifier. |
| Traceability | 20/20 | All ACs and constraints trace to the story content supplied. |
| Atomicity | 20/20 | Each AC is one behaviour and one observable outcome. |
| Completeness | 18/20 | Most non-functional dimensions specified; metrics and monitoring explicitly out of scope. |
| Edge coverage | 18/20 | Common failure modes listed; concurrency/thundering-herd details left as implementation choice. |
| **Total** | **112/120** | |

Deductions: minor gaps around explicit in-flight deduplication guidance and whether a cache-populate-in-progress should be observable/awaited by concurrent callers.

---

## Section 11 — Design trigger
**DESIGN REQUIRED: YES**
Reason: The repository has no existing cache abstraction or in-memory cache pattern per the story. Adding a reactive, non-blocking cache component (with TTL and LRU eviction) introduces a new architectural pattern that should be reviewed (threading, memory, Reactor integration, and test strategy). The change also touches the BookService → CatalogClient call path and should be designed to ensure reactive correctness and testability.

---

## Section 12 — Feasibility
**VERDICT: GO**
Basis: CatalogClient.fetchBook(bookId) and BookService call site are assumed to exist per the story. The requested behavior is implementable with an in-memory, Reactor-compatible cache (e.g., Caffeine with asynchronous/reactive access adaptor or a custom ConcurrentLinkedHashMap-based structure exposing Reactor types) without modifying controllers. Unit tests can mock CatalogClient and use StepVerifier to validate hit/miss/expiry/empty/error paths. No external dependencies or OpenAPI edits are required.

Notes: If the codebase uses a particular Reactor-friendly cache library already, prefer that for consistency; otherwise document the chosen library in the change set and add memory/heap considerations to the PR description.

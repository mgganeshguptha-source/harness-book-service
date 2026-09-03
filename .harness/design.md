# Design — BOOK-2: Cache catalog book lookups

**Source:** .github/story-context-files/cache-catalog-book-lookups-context-260903-081928.md
**Design required because:** introduces a caching pattern the service does not currently use (in-memory TTL + capacity-bound LRU) and changes how CatalogClient responses are obtained by BookService (a structural decision about where caching is implemented and how to integrate it into the reactive flow).

## 1. Existing system

What is there today that this change touches:
- Controller: com.example.book.controller.BookController — delegates to BookService and must remain unchanged (AC-7).
- Service: com.example.book.service.BookServiceImpl (implements BookService) — currently calls CatalogClient.fetchBook(bookId) and maps the DTO to BookResponse.
- Client interface: com.example.book.webclient.CatalogClient — exposes Mono<CatalogBookDto> fetchBook(String bookId) and fetchBookByAuthor(String author).
- Client implementation: com.example.book.webclient.CatalogClientImpl — WebClient-based downstream caller. Closest precedent: synchronous caching or decorator pattern does not exist in this repo; no cache abstraction exists.
- Model: com.example.book.model.CatalogBookDto (the cached payload shape).

Must not break: BookController API/behaviour, generated OpenAPI modules, or the CatalogClient contract (signatures remain the same).

## 2. Approach

Add a reactive, in-memory, capacity-limited cache between BookService and the existing CatalogClient implementation by providing a caching decorator that implements CatalogClient and is wired as the CatalogClient bean BookServiceImpl receives.

The decorator will use an in-process LRU cache with time-based expiry (10 minutes) and a maximum of 1000 entries. For correctness and to avoid duplicate concurrent downstream calls on cache-miss, use an async-capable cache (see Decisions). The decorator returns Monos and integrates without blocking (.block()/collectList() are not used). Empty results from the catalog are not cached; errors are propagated unchanged.

This keeps controllers unchanged (AC-7), preserves the CatalogClient interface (no signature changes), and concentrates the caching concern in a single, testable component.

## 3. Decisions

### D1 — Where the cache sits
**Serves:** AC-1, AC-2, AC-7
**Chosen:** implement the cache as a CatalogClient decorator (e.g., com.example.book.webclient.CachingCatalogClient implements CatalogClient) that wraps the existing CatalogClientImpl and is registered as the CatalogClient bean consumed by BookServiceImpl (use constructor injection; mark as @Primary or configure wiring so BookServiceImpl gets the decorator).
**Why:** keeps the controller and service method signatures unchanged, centralises caching logic, and makes the cache transparent to callers that already depend on CatalogClient. The service layer remains the logical place to own caching concerns while making the replacement transparent to BookServiceImpl callers (BookController unchanged per AC-7).
**Alternatives considered:**
- Modify BookServiceImpl to call a separate cache service class directly — rejected: requires changing service wiring and scattering caching logic into service code (more invasive) and increases risk of forgetting to use the cache from other call sites.
- Add caching at controller layer — rejected: violates AC-7 (controller must remain unchanged) and mixes transport concerns with caching.
- Add cache inside CatalogClientImpl (internal to downstream client) — rejected: hides caching in a lower-level component that may be shared by consumers who should not cache, and complicates unit testing of client-specific behaviour.
**Trade-off:** Decorating CatalogClient is minimal-invasive and keeps callers unchanged, but relies on Spring wiring order and a single bean override (requires care in configuration). If other callers bypass the injected CatalogClient (e.g., instantiate CatalogClientImpl directly), they will not use the cache — accepted for this story.

### D2 — Cache implementation choice (library & concurrency)
**Serves:** AC-1, AC-2, AC-3, AC-6
**Chosen:** use an in-memory, async-capable cache implementation (recommended: Caffeine AsyncLoadingCache) configured with expireAfterWrite(10 minutes) and maximumSize(1000) with LRU eviction semantics. The cache key is the exact bookId string (per story decision). The cache's async loading will use a mapping function that invokes catalogClient.fetchBook(bookId).toFuture()/toCompletableFuture(), integrating Reactor-to-Java-Future bridging so the cache avoids duplicate in-flight downstream calls.
**Why:** Caffeine provides battle-tested LRU + TTL behaviour, efficient concurrent access, and an AsyncLoadingCache that cleanly maps to Reactor by using CompletableFuture without blocking. Using an async cache prevents thundering-herd duplicate downstream calls for the same key on concurrent misses while keeping everything non-blocking (AC-6).
**Alternatives considered:**
- Guava Cache or custom LinkedHashMap-based LRU — rejected: reimplements eviction/TTL concurrency semantics and error-prone under concurrency; Guava lacks the same robust async-loading support as Caffeine.
- Manual ConcurrentHashMap + explicit locking/Promise management — rejected: high maintenance and test burden; easy to get races or memory leaks wrong.
- External/distributed caches (Redis, Memcached) — rejected: out of scope (explicitly by story) and introduces network I/O (and additional operational complexity).
**Trade-off:** adding a small dependency (Caffeine) increases binary size but greatly reduces implementation complexity, improves correctness under concurrency, and fits the in-process requirement. Tests must mock bridging to/from CompletableFuture or use StepVerifier-friendly adapters.

### D3 — Key shape and normalization
**Serves:** AC-1, AC-2
**Chosen:** cache key is the exact String passed as bookId, no normalization or canonicalization.
**Why:** this matches the explicit behaviour decisions in the story and guarantees predictable cache hits.
**Alternatives considered:**
- Normalizing bookId (trim/case) — rejected: story explicitly forbids normalization.
**Trade-off:** exact-keying reduces accidental hits due to variations; callers must use consistent bookId values.

### D4 — Expiry and capacity
**Serves:** AC-3 (expiry), AC-1 (hit window), story constraints
**Chosen:** expire after write: 10 minutes; maximum size: 1000 entries; eviction policy: LRU (Caffeine's default behaviour with maxSize). Do not implement write-based invalidation (story specifies no invalidation on write).
**Why:** matches explicit behavior decisions in the story and avoids complexity of listening for catalog changes.
**Alternatives considered:**
- No eviction (unbounded) — rejected: risks OOM under load.
- Size-based only or expiry-only — rejected: both constraints together provide predictable memory bounds and bounded staleness.
**Trade-off:** entries may be evicted under memory pressure even if still fresh; consumers may see additional catalog calls when eviction occurs.

### D5 — Empty results (no-such-book) handling
**Serves:** AC-4
**Chosen:** do NOT cache empty (not-found) results. If catalogClient.fetchBook(bookId) completes empty (Mono.empty()), propagate empty and do not populate the cache.
**Why:** story explicitly requires that a book added later must be visible immediately without waiting for expiry.
**Alternatives considered:**
- Cache negative lookups for short TTL — rejected: violates AC-4.
**Trade-off:** repeated lookups for permanently-missing ids will continue to hit the catalog service until the caller stops requesting them.

### D6 — Downstream errors
**Serves:** AC-5
**Chosen:** do NOT cache failures/exceptions from the catalog; propagate the error unchanged to callers.
**Why:** story requires error propagation unchanged and that errors do not poison the cache.
**Alternatives considered:**
- Cache transient failures for a short backoff window (circuit-breaker-like) — rejected: out of scope and violates AC-5's explicit requirement.
**Trade-off:** clients may receive repeated errors for a downstream outage; resilience strategies (retries/circuit-breakers) are out of scope.

### D7 — Reactive non-blocking integration
**Serves:** AC-6
**Chosen:** the decorator returns Mono<CatalogBookDto> unchanged. Cache lookups that are in-memory complete synchronously and are returned as Mono.just(value) or Mono.empty(); cache miss uses the async-loading cache and the mapping function bridges the catalogClient.fetchBook(bookId) Mono to a CompletableFuture and back to Mono without blocking. Implementation avoids .block() or collectList().
**Why:** ensures the reactive contract remains intact and satisfies the explicit non-blocking constraint.
**Alternatives considered:**
- Use computeIfAbsent with a supplier that calls catalogClient.fetchBook(...).block() — rejected: introduces blocking and violates AC-6.
- Use Reactor-only caching operators — rejected: they are stream-scoped and do not provide a shared, cross-call cache with TTL and size eviction semantics required here.
**Trade-off:** bridging Reactor to CompletableFuture is straightforward but requires careful testing to ensure completion paths and error paths map correctly.

### D8 — Bean wiring and constructor injection
**Serves:** AC-7 (controllers unchanged), general constraints
**Chosen:** implement the caching decorator as a Spring bean constructed with constructor injection of the existing CatalogClientImpl (or the underlying WebClient bean). Register the decorator as the primary CatalogClient (e.g., using @Primary or explicit @Bean wiring) so BookServiceImpl continues to inject CatalogClient and obtains the caching behavior without changing controllers. All new components use constructor injection.
**Why:** meets the repository conventions (constructor injection) and keeps BookController, BookServiceImpl signatures unchanged.
**Alternatives considered:**
- Change BookServiceImpl to accept a CatalogCacheService — rejected: more invasive.
**Trade-off:** requires attention to bean ordering in tests and production configuration; tests should explicitly instantiate the decorator and mock the wrapped client.

### D9 — Testing strategy
**Serves:** AC-8 (unit tests, coverage), AC-1..AC-6 (behavioural validation)
**Chosen:** unit tests for the caching component only (mocking the wrapped CatalogClient) using JUnit 5 + Reactor StepVerifier. Tests should cover:
- Hit: two successive calls for same bookId within TTL return cached result and wrapped client is called once (AC-1).
- Miss: cache miss calls wrapped client and stores result (AC-2).
- Expiry: entry older than 10 minutes triggers downstream call (AC-3) — simulate by advancing virtual clock (where possible) or by exposing a small TTL override in test configuration.
- Empty result: wrapped client returns Mono.empty(), nothing cached, subsequent call calls wrapped client again (AC-4).
- Error: wrapped client returns error, nothing cached, error propagates (AC-5).

Mocking notes: tests will mock the wrapped CatalogClient to return Mono.just(dto), Mono.empty(), or Mono.error(...). Because the implementation uses Reactor<->CompletableFuture> bridging for async loading, tests must adapt to that bridging; or tests can bypass AsyncLoadingCache by using explicit cache put/get helpers where convenient.

## 4. Contract changes

No public API or generated OpenAPI contract changes.

Service-visible signature changes: none — CatalogClient interface remains unchanged:

    Mono<CatalogBookDto> fetchBook(String bookId);

The caching decorator implements the same interface; BookServiceImpl constructor signature remains the same (no controller changes), satisfying AC-7.

If wiring requires it, a @Configuration class may register the caching decorator as the CatalogClient bean.

## 5. Data & state

No persistent state changes. The cache is in-memory only; no database or schema changes.

Not applicable — no new entities or persistence layers are introduced.

## 6. Impact

**Expected to change (new/modified files):**
- sample-book-service-application/src/main/java/com/example/book/webclient/CachingCatalogClient.java (new) — the CatalogClient decorator that implements caching logic and delegates to the wrapped CatalogClient.
- (optional) sample-book-service-application/src/main/java/com/example/book/config/CacheConfig.java (new) — cache configuration (Caffeine cache settings) and wiring to expose the caching CatalogClient as the primary bean.
- build tooling (pom.xml or build.gradle) — add Caffeine dependency.
- unit tests under src/test/java/com/example/book for the caching component (new test class) — Exercise hit/miss/expiry/empty/error paths with StepVerifier.

**Explicitly unchanged:**
- sample-book-service-application/src/main/java/com/example/book/controller/BookController.java (controller behaviour and signature remain unchanged) — AC-7.
- Generated OpenAPI code modules — do not hand-edit any generated code (story constraint).
- CatalogClient interface signatures.

## 7. Risks

- Dependency & CVE surface: adding Caffeine increases dependency surface; choose maintained version and review licenses.
- Memory usage: in-process cache consumes heap; maximumSize(1000) mitigates but under heavy object size the service may still risk OOM — monitor in a follow-up story (metrics out-of-scope here).
- Eviction churn: under heavy unique-id traffic, LRU evictions may cause frequent misses and downstream load spike.
- Semantics of expiry vs catalog updates: 10-minute expiry may serve slightly stale catalog data; acceptable per story. If stronger freshness is required later, an invalidation-outbox or distributed cache will be needed.
- Wiring surprises: using @Primary or bean replacement must be done carefully so tests that create CatalogClient mocks still behave correctly.
- Bridging Reactor <-> CompletableFuture: mapping must preserve empty vs value vs error semantics exactly; tests must validate all paths.

## 8. Open questions

None — the story explicitly resolved the key behavioural questions (what to cache, key shape, expiry, capacity, empty/error handling, scope). All are implemented by the chosen approach.


---

Design written to .harness/design.md — 9 decisions recorded (D1..D9) and mapped to acceptance criteria as noted above.

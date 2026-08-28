# Story BOOK-2: Cache catalog book lookups

## Goal
Reduce load on the catalog service by caching book lookups in the book service.
Catalog data changes rarely, and the same books are requested repeatedly, so most
calls to `CatalogClient` return a value the service has already seen.

The service has no caching today — no cache abstraction, no cache dependency, and
no eviction policy anywhere in the codebase.

## Expected behaviour
- Lookups through `CatalogClient` are served from a cache when a fresh entry exists.
- A cache miss falls through to the catalog service and populates the cache.
- Entries expire so stale catalog data is not served indefinitely.
- Everything stays reactive — no blocking call is introduced by the cache.

## Explicit behaviour decisions (resolving prior clarifications)
1. **What is cached:** the result of `CatalogClient.fetchBook(String bookId)`.
   Author lookups are out of scope for this story.
2. **Key:** the `bookId` exactly as passed to the client. No normalisation.
3. **Expiry:** entries expire 10 minutes after they are written. Time-based only —
   no invalidation on write, because this service does not write to the catalog.
4. **Capacity:** the cache holds at most 1000 entries. When full, evict least
   recently used.
5. **Cache miss behaviour:** call the catalog service exactly as today and store
   the result. A miss must never fail the request.
6. **Empty results:** when the catalog returns empty (no such book), do NOT cache
   the empty result. A book added later must be visible without waiting for expiry.
7. **Downstream errors:** when the catalog call fails, do NOT cache anything and
   let the error propagate as it does today. No fallback to a stale entry.
8. **Scope of the change:** the cache sits between `BookService` and
   `CatalogClient`. Controllers are unchanged and must not know a cache exists.
9. **Metrics/monitoring:** out of scope for this story.
10. **Distributed caching:** out of scope. A single-instance in-memory cache is
    sufficient; a shared cache across instances is a separate story.
11. **Ownership:** N/A — demo story, no named owner or reviewer required.

## Acceptance criteria
- AC1: A second lookup for the same `bookId` within the expiry window does not
  call the catalog service.
- AC2: A lookup for a `bookId` not in the cache calls the catalog service and
  stores the result.
- AC3: A lookup for a `bookId` whose entry is older than 10 minutes calls the
  catalog service again.
- AC4: When the catalog returns empty, nothing is cached, and a later lookup for
  the same `bookId` calls the catalog service again.
- AC5: When the catalog call fails, nothing is cached and the error propagates
  unchanged.
- AC6: No blocking call is introduced — no `.block()`, no `.collectList()`.
- AC7: The controller layer is unchanged by this story.
- AC8: Unit tests with `StepVerifier` cover hit, miss, expiry, empty and error
  paths; changed classes reach at least 90% line coverage.

## Constraints
- Reactive stack: Spring WebFlux / Reactor. The cache must not block.
- Constructor injection for any new component.
- Unit tests: JUnit 5 + Reactor `StepVerifier`; mock `CatalogClient`; no external calls.
- Do not hand-edit the generated `*-openapi-code` module or any OpenAPI spec.
- Tests are produced in the unit_testing phase, not during coding.

## Out of scope
- Caching author lookups.
- Distributed or shared caching across service instances.
- Cache metrics, monitoring or admin endpoints.
- Any change to the catalog service itself.
- Editing the OpenAPI spec or the generated module.
- Resilience policies (timeouts, retries, circuit breakers).

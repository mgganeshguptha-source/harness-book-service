## What Are We Trying to Achieve
Add a reactive WebFlux endpoint that returns a single book for a given author name. This enables callers to look up a book by exact, case-insensitive author match using the existing reactive patterns in the service; the endpoint must be non-blocking and delegate to the catalog client for lookup.

## Current Behaviour
A reactive `getBookById` endpoint exists in the service. There is no endpoint that looks up a book by author.

## Expected Behaviour
- New endpoint: GET /books/by-author/{author}
- Accepts a non-blank author path segment (URL-decoded by the framework).
- Performs an exact, case-insensitive match on the author name.
- Delegates lookup through the existing catalog client abstraction; the controller delegates to the service layer, which calls the catalog client method `fetchBookByAuthor(String author)` returning a `Mono` of the catalog DTO.
- If the catalog client returns empty → respond 404 Not Found.
- If the author path parameter is empty or blank → respond 400 Bad Request.
- Entire flow is reactive (returns `Mono<ResponseEntity<BookResponse>>`) with no blocking calls such as `.block()` or `.collectList()`.

## Acceptance Criteria
- AC1: `GET /books/by-author/{author}` exists and returns `Mono<ResponseEntity<BookResponse>>`.
- AC2: Controller delegates to a BookService; no blocking operations present.
- AC3: Lookup goes through `CatalogClient.fetchBookByAuthor(...)`; no direct downstream HTTP calls in controller/service code.
- AC4: Reactive patterns are followed (Reactor `Mono` composition); complies with reactive WebFlux idioms.
- AC5: Empty catalog result produces 404; blank author produces 400.
- AC6: Unit test with `StepVerifier` covers the happy path and asserts the response body; modified class has ≥ 90% line coverage.
- AC7: OpenAPI spec updated to include the new operation; code generation remains the source of truth (no hand-edit outside the spec).

## Edge Cases
- Path contains spaces or UTF-8 characters (framework-decoded) — treat as-is for exact, case-insensitive matching.
- Author contains apostrophes or diacritics (e.g., O'Brien, García) — exact, case-insensitive match semantics apply.
- Catalog client returns multiple matches despite expectation — pick the first result returned; do not return 409.
- Very long author string — validate and return 400 if blank; if further length limits are needed, flag for follow-up.
- Concurrent requests for same author — reactive non-blocking handling; no additional concurrency control required in this story.

## Constraints
- Reactive stack: Spring WebFlux / Reactor only. No blocking calls (`.block()`, `.collectList()` are forbidden).
- Constructor injection for new components.
- Unit tests: JUnit 5 + Reactor `StepVerifier`; mock `CatalogClient`; do not perform external calls in tests.
- No timeouts/retries/circuit-breaker/rate-limiting to be added in this story — infra owns resilience.
- Follow existing reactive-controller and reactive-webclient repository conventions.
- OpenAPI must be updated for the operation; generated code is authoritative.

## Out of Scope
- Database schema changes or direct DB access.
- Pagination or listing multiple books by author.
- Authentication/authorization changes; endpoint is public for this sample.
- Performance/load testing or resilience policies (timeouts/retries/circuit-breakers).

## Clarifications Needed
_None — story includes explicit decisions for match rule, multiple-match handling, path encoding, catalog client method name, authentication, empty/blank handling, and resilience scope._

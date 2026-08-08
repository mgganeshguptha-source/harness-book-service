## What Are We Trying to Achieve
Add a reactive-only endpoint to return a single book by exact (case-insensitive) author name, following the existing reactive getBookById pattern and delegating lookup to the catalog common-layer client (CatalogClient). No blocking or collection-to-list; controller returns Mono<ResponseEntity<BookResponse>> and composes Reactor types end-to-end.

## Current Behaviour
No endpoint exists to fetch a book by author. A reactive GET by-id endpoint (getBookById) exists and serves as the pattern to follow. CatalogClient currently exposes fetchBook(String bookId) but not a fetch-by-author method.

## Expected Behaviour
- New API operation: GET /books/by-author/{author}
- Controller implements the generated BooksApi.getBookByAuthor(...) method (no controller-level mapping annotation changes).
- Controller returns Mono<ResponseEntity<BookResponse>> and delegates to BookService.
- BookService delegates to CatalogClient.fetchBookByAuthor(String author) which returns Mono<CatalogBookDto>.
- CatalogClientImpl calls downstream catalog endpoint /catalog/books/by-author/{author}.
- Reactive only: no .block(), no .collectList(), no blocking calls anywhere.
- Matching: exact, case-insensitive author match. If CatalogClient emits multiple results, use the first. If empty → 404.
- Validation: blank or >256 characters → 400 Bad Request.
- Public endpoint: no auth/roles required for this sample.

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller delegates to BookService; no blocking operations (.block/.collectList) used.
- AC3: Lookup uses CatalogClient.fetchBookByAuthor(...) — no direct downstream HTTP calls from controller/service.
- AC4: Reactive patterns used end-to-end (Mono composition via Reactor). Matches reactive-controller and reactive-webclient instructions.
- AC5: Empty catalog result → 404; blank author or author length > 256 → 400.
- AC6: Unit tests (in unit_testing phase) will use StepVerifier to cover the happy path and assert response body; changed class must reach ≥ 90% line coverage (enforced by gate later).
- AC7: Controller implements the existing BooksApi.getBookByAuthor interface method; no edits to generated openapi modules or specs.

## Edge Cases
- Author path segment may include spaces and UTF-8 characters (framework-decoded) — accept as-is.
- Very long author strings (>256) → 400.
- Blank or purely whitespace author → 400.
- Catalog returns multiple matches despite contract — take the first result only.
- Catalog returns non-matching-casing value — matching is case-insensitive on the author parameter; implementation assumes catalog endpoint honors this or service performs case-insensitive comparison without blocking.
- Downstream catalog empty → 404.
- Special characters (quotes, diacritics, emojis) allowed — no additional sanitisation beyond length check.

## Constraints
- Backend (defaults used — .github/copilot-instructions.md not provided / using defaults): Constructor injection | Standard error response format | Jakarta Validation on inputs | JUnit 5 + Mockito for tests | Pagination for list endpoints (not applicable here).
- Reactive-specific: Spring WebFlux / Reactor stack — return Mono types, do not block; follow existing reactive-controller and reactive-webclient conventions in the repo.
- Coding-phase scope: only production source under src/main/** (controller, service, client); do NOT modify generated openapi code or OpenAPI spec.
- Unit tests (unit_testing phase): JUnit 5 + Reactor StepVerifier; mock CatalogClient; no external calls.
- No resilience code (timeouts, retries, circuit-breakers, rate-limiting) to be added — infra owns them.
- Max author length: 256 characters; blank or >256 → 400.
- Controller must implement BooksApi.getBookByAuthor (no @GetMapping added to controller class).

## Out of Scope
- Editing OpenAPI spec or generated *-openapi-code module.
- Database access or schema changes.
- Pagination or returning multiple books by author.
- Authentication/authorization changes.
- Performance/load testing and resilience policies.

<!-- Section 8 — Clarifications Needed omitted: story provides explicit decisions; no outstanding technical ambiguities for CI mode -->

## ⚠️ Missing .github/copilot-instructions.md

I did not find `.github/copilot-instructions.md` in the repository. Default Spring Boot / Angular constraints will be used in the Constraints section below.

---

## What Are We Trying to Achieve
Add a reactive endpoint that returns a single book for a given author name. The endpoint must be implemented with Spring WebFlux / Reactor, follow the existing getBookById reactive pattern, delegate to the BookService which uses CatalogClient, and avoid any blocking calls.

## Current Behaviour
No `GET /books/by-author/{author}` endpoint exists. A reactive `getBookById` pattern is present and CatalogClient provides `fetchBook(String bookId)`; no fetch-by-author method exists yet.

## Expected Behaviour
- Expose the operation declared on the generated `BooksApi` interface: `getBookByAuthor(String author)` returning `Mono<ResponseEntity<BookResponse>>`.
- Controller implements the existing `BooksApi.getBookByAuthor` method (do not add controller-level mapping annotations or edit generated OpenAPI modules).
- Controller delegates to `BookService`, which calls `CatalogClient.fetchBookByAuthor(author)` and returns the composed reactive result to the controller.
- The CatalogClient gets a new method `fetchBookByAuthor(String author)` returning `Mono<CatalogBookDto>`; implement in `CatalogClientImpl` to call `/catalog/books/by-author/{author}`.
- Matching rule: exact, case-insensitive match on author name. If multiple catalog results are returned, use the first element.
- Validation: blank author or author length > 256 → 400 Bad Request. Valid inputs (including spaces and UTF-8) accepted.
- No authentication required for this sample endpoint (public).
- Empty catalog result → 404 Not Found.
- Reactive only: no `.block()` or `.collectList()` anywhere; return Reactor types and compose them.

## Acceptance Criteria
- AC1: `GET /books/by-author/{author}` exists and the controller method returns `Mono<ResponseEntity<BookResponse>>` as declared on `BooksApi`.
- AC2: Controller delegates to `BookService`; no blocking operations are used in controller or service.
- AC3: Lookup goes through `CatalogClient.fetchBookByAuthor(...)`; no direct downstream HTTP calls from controller or service.
- AC4: Reactive patterns followed (returns `Mono`, composes Reactor types); complies with reactive-controller and reactive-webclient instructions.
- AC5: Empty catalog result → 404; blank author or author > 256 chars → 400.
- AC6: Unit test (in unit_testing phase) uses StepVerifier to cover the happy path and assert the response body; changed class must reach ≥ 90% line coverage (tested in unit_testing phase).
- AC7: Controller implements the existing `BooksApi.getBookByAuthor` interface method; do NOT modify generated `*-openapi-code` or OpenAPI specs.

## Edge Cases
- Author is an empty string or only whitespace → return 400 Bad Request.
- Author longer than 256 characters → return 400 Bad Request.
- Author contains spaces, UTF-8 characters, punctuation (e.g., "Gabriel García Márquez") — accept as-is (framework-decoded).
- Catalog returns multiple books for exact author match — take the first result and ignore the rest (no 409 aggregation logic).
- Catalog returns EXTERNAL errors or timeouts — story explicitly omits resilience; these surface as upstream errors (handled by existing global error handling). _No timeouts/retries added here._
- Catalog returns empty Mono → controller/service map to 404.

## Constraints
Using repository defaults because `.github/copilot-instructions.md` was not found.

Backend (defaults):
- Spring Boot (WebFlux / Reactor) — reactive stack required
- Constructor injection for new components
- Standard error response format (application/problem+json) and centralized `@ControllerAdvice`
- Jakarta Validation for inputs (use `@Validated` / `@NotBlank` / custom length check)
- JUnit 5 + Mockito for unit tests; Reactor StepVerifier for reactive assertions
- Do not modify generated OpenAPI modules or specs
- No blocking calls in reactive code (no `.block()`, no `.collectList()` on request-handling path)

Reactive specifics (story-enforced):
- Return `Mono<ResponseEntity<BookResponse>>` from controller
- Service and client return and compose Reactor types only
- CatalogClient method `fetchBookByAuthor(String)` returns `Mono<CatalogBookDto>` and implemented in `CatalogClientImpl` to call `/catalog/books/by-author/{author}` using the repository's reactive WebClient pattern

## Out of Scope
- Editing the OpenAPI spec or generated `*-openapi-code` module (the interface method already exists)
- Database access or schema changes
- Pagination or listing multiple books by author
- Authentication/authorization changes
- Adding resilience (timeouts, retries, circuit-breakers, rate limiting)
- Performance/load testing

## Clarifications Needed
_None — the story supplied explicit decisions for match rule, multiple matches, path decoding, CatalogClient method name and path, validation, auth, and resilience policy._

---

Owner / Team: N/A (not required for implementation)
Repository Path: /home/runner/work/harness-book-service/harness-book-service/service
Last Analysed: 260808

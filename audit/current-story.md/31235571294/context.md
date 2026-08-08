## What Are We Trying to Achieve
Add a reactive endpoint that returns a single book for a given author name. The endpoint must be non-blocking (Spring WebFlux / Reactor), delegate through BookService to the CatalogClient, and validate the author path parameter (non-blank, max 256 chars). Returns 404 when no book found.


## Expected Behaviour
- New controller method implements the already-declared BooksApi.getBookByAuthor and returns Mono<ResponseEntity<BookResponse>>.
- Route: GET /books/by-author/{author} (operation declared in generated BooksApi; do not add controller-level mapping).
- Controller delegates to BookService which calls CatalogClient.fetchBookByAuthor(author) returning Mono<CatalogBookDto>.
- Reactive composition only: no .block(), no .collectList(), no blocking calls anywhere.
- Validation: blank author or author length > 256 → 400 Bad Request.
- Matching: exact, case-insensitive match on author name. If CatalogClient returns multiple results, use the first. If CatalogClient returns empty → 404 Not Found.
- Public endpoint: no auth roles/scopes required for this sample.


## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and the controller method returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller delegates to BookService; no blocking operations in controller or service.
- AC3: Book lookup is performed by CatalogClient.fetchBookByAuthor(String author); no direct downstream HTTP calls from controller/service.
- AC4: Reactive patterns followed throughout (Mono composition). Compliance with reactive-controller and reactive-webclient instructions.
- AC5: Validation: blank author or >256 chars → 400; empty catalog result → 404.
- AC6: Unit tests (unit_testing phase) include StepVerifier happy-path asserting response body; changed class achieves >=90% line coverage (enforced by harness).
- AC7: Controller implements the generated BooksApi.getBookByAuthor interface method (do not modify generated module or OpenAPI spec).


## Edge Cases
- Author containing spaces and UTF-8 characters: accept as-is (framework-decoded path variable).
- Very long author (>256): return 400.
- Blank or all-whitespace author: return 400.
- CatalogClient unexpectedly returns multiple books for the exact match: use the first result and return 200 with that book.
- CatalogClient returns empty Mono: return 404.
- CatalogClient emits error: propagate as a 5xx per existing global error handler (do not change error handling here).


## Constraints
- .github/copilot-instructions.md not found in repository — using generic defaults.

Backend (defaults): Constructor injection | Standard error response format | Jakarta Validation on inputs | JUnit 5 + Mockito for tests | Pagination for list endpoints (not relevant here)

Story-specific constraints:
- Reactive stack: Spring WebFlux / Reactor. No blocking calls (.block(), .collectList()).
- Follow existing reactive-controller and reactive-webclient guidance in the repo.
- Constructor injection for new components; new CatalogClient.fetchBookByAuthor method mirrors existing CatalogClient.fetchBook(String).
- No timeouts/retries/circuit-breaker/rate-limiting to be added in this story.
- Do not edit generated *-openapi-code module or OpenAPI spec.
- Unit tests must mock CatalogClient (no external calls).


## Out of Scope
- Editing OpenAPI spec or generated *-openapi-code module (getBookByAuthor already declared on BooksApi).
- Database access or schema changes — lookup is only via CatalogClient.
- Pagination or listing multiple books by author.
- Authentication/authorization changes.
- Resilience policies (timeouts, retries, circuit breakers, rate limiting).
- Performance/load testing or caching strategy.


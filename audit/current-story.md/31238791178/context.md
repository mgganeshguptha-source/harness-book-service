## What Are We Trying to Achieve
Add a reactive GET endpoint that returns a single book for a given author name. The endpoint must follow the existing reactive patterns (WebFlux/Reactor), delegate to the service layer and a CatalogClient, avoid any blocking calls, and validate the author path parameter (non-blank, max 256 chars). The controller implementation must use the already-generated BooksApi interface method.

## Current Behaviour
No `GET /books/by-author/{author}` endpoint exists. Existing reactive patterns for fetching a book by ID are present in the codebase (not modified by this story). Lookups are performed via a CatalogClient abstraction for catalog queries.

## Expected Behaviour
- Exposed endpoint: GET /books/by-author/{author}
- Controller method signature: Mono<ResponseEntity<BookResponse>> and it implements the existing BooksApi.getBookByAuthor(...) interface method (no controller-level @GetMapping added by hand).
- Controller delegates to BookService; BookService delegates to CatalogClient.fetchBookByAuthor(String author) which returns Mono<CatalogBookDto>.
- Reactive only: no .block(), no .collectList(), compose Reactor types throughout.
- Author matching: exact, case-insensitive match on author name.
- If CatalogClient returns empty: respond 404 Not Found.
- If CatalogClient returns multiple results: use the first result (no 409/aggregation).
- If author path variable is blank or longer than 256 characters: respond 400 Bad Request.
- Path parameter is accepted as already URL-decoded by the framework (spaces and UTF-8 allowed).
- Public endpoint: no authentication/roles enforced for this sample.

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller delegates to BookService; no blocking operations are introduced.
- AC3: Lookup is performed exclusively via CatalogClient.fetchBookByAuthor(...); no direct downstream HTTP is called from controller/service layers.
- AC4: Reactive composition is used (Mono returned, Reactor operators composed); follows project reactive-controller and reactive-webclient rules.
- AC5: Empty catalog result returns 404; blank author → 400; author > 256 chars → 400.
- AC6: Unit tests (in the unit_testing phase) use StepVerifier to cover the happy path and assert response body; changed class should reach ≥90% line coverage (enforced by later gate).
- AC7: Controller implements BooksApi.getBookByAuthor interface method; do NOT modify generated OpenAPI or generated modules.

## Edge Cases
- Blank or empty author path (e.g., "" or whitespace-only) → 400 Bad Request.
- Author length > 256 characters → 400 Bad Request.
- Author containing UTF-8 characters (accents, emoji) and spaces — accepted as-is.
- Author value arrives URL-decoded by framework; percent-encoded input handled by framework before controller.
- CatalogClient returns no result → 404.
- CatalogClient returns more than one result for an exact match → choose first result and return it.
- CatalogClient returns an error (5xx) — service will propagate an appropriate 5xx per error-handling patterns (tests should mock error scenarios where relevant in unit_testing phase).
- Downstream timeouts/retries/rate-limiting are OUT OF SCOPE for this story (infrastructure responsibility).

## Constraints
- Reactive stack: Spring WebFlux / Reactor. No blocking calls (.block(), .collectList()) anywhere in the new code.
- Follow repository reactive-controller and reactive-webclient guidance.
- Constructor injection for new components.
- Do not modify generated OpenAPI modules; implement the existing BooksApi interface method in the controller.
- CatalogClient must expose fetchBookByAuthor(String author): Mono<CatalogBookDto> and CatalogClientImpl calls /catalog/books/by-author/{author}.
- Validation: author parameter max length 256 characters; blank or >256 → 400.
- Unit tests: JUnit 5 + Reactor StepVerifier; mock CatalogClient; no external HTTP calls in tests (unit_testing phase).

NOTE: .github/copilot-instructions.md not found in the repository — default constraints (constructor injection, reactive stack, standard error format, JUnit5/Mockito for tests) are applied.

## Out of Scope
- Editing the OpenAPI spec or generated *-openapi-code module (the BooksApi.getBookByAuthor operation is already declared and must not be changed).
- Direct database access or schema changes — lookup must use CatalogClient only.
- Pagination or listing multiple books by author.
- Authentication/authorization changes.
- Performance/load testing and resilience policies (timeouts, retries, circuit-breakers, rate limiting).


## What Are We Trying to Achieve
Add a reactive, non-blocking endpoint that returns a single book by exact (case-insensitive) author name. The endpoint must follow the existing reactive getBookById pattern: controller implements the generated BooksApi interface method, delegates to BookService, which delegates to the CatalogClient. No blocking calls or direct downstream HTTP calls in controllers/services.

## Expected Behaviour
- Endpoint: GET /books/by-author/{author}
- Returns: Mono<ResponseEntity<BookResponse>>
- Controller: implements BooksApi.getBookByAuthor(...) (do NOT add @GetMapping on controller class). Controller delegates to BookService.
- Service: BookService calls CatalogClient.fetchBookByAuthor(String author) which returns Mono<CatalogBookDto>.
- Catalog client: CatalogClient.fetchBookByAuthor mirrors existing fetchBook(bookId) method and CatalogClientImpl calls the catalog endpoint /catalog/books/by-author/{author}.
- Matching: exact, case-insensitive match on author name. If CatalogClient returns multiple items, use the first result.
- Authentication: public endpoint (no roles/scopes required).
- Error cases: blank or >256-char author → 400 Bad Request; CatalogClient empty result → 404 Not Found.
- Encoding: accept the framework-decoded path variable as-is (spaces and UTF-8 allowed). Do not add custom decoding.

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller implements BooksApi.getBookByAuthor and delegates to BookService; no blocking (.block(), .collectList()) anywhere.
- AC3: Book lookup goes through CatalogClient.fetchBookByAuthor(...); no direct downstream HTTP calls from controller/service.
- AC4: Reactive patterns followed (Mono composition); complies with repo reactive-controller and reactive-webclient rules.
- AC5: Blank author or author >256 chars → 400; no match → 404.
- AC6: Unit tests (unit_testing phase) will use StepVerifier to cover the happy path and assert response body; changed class targeted for ≥90% line coverage (tests added in unit_testing phase only).
- AC7: Do not modify generated *-openapi-code module or OpenAPI spec; implement the already-declared BooksApi.getBookByAuthor method.

## Edge Cases
- Author names containing spaces, non-ASCII (UTF-8) characters: accepted as path variable (framework-decoded).
- Authors with special characters (apostrophes, diacritics): accepted; matching is exact, case-insensitive.
- Multiple matches returned by catalog: pick the first; do not return 409 or aggregate.
- Very long author values (>256): validation should return 400.
- Empty or whitespace-only author: validation should return 400.
- Path characters normally reserved (e.g., '/'): framework decoding behavior assumed—story states value arrives decoded; do not add custom decoding.

## Constraints
- Backend: Spring WebFlux (Reactive) / Reactor. No blocking calls (.block(), .collectList()) anywhere in controller/service/client.
- Follow repository reactive-controller and reactive-webclient instructions and patterns.
- Constructor injection for new components.
- CatalogClient: add Mono<CatalogBookDto> fetchBookByAuthor(String author) to the client interface and implement in CatalogClientImpl to call /catalog/books/by-author/{author}.
- Unit tests (unit_testing phase): JUnit 5 + Reactor StepVerifier; CatalogClient mocked; no external calls.
- No DB access; lookup via CatalogClient only.
- Do not modify generated *-openapi-code module or OpenAPI spec.

Note: .github/copilot-instructions.md not found in this repository; defaults applied for Constraints (constructor injection, standard error format, Jakarta Validation where applicable, JUnit 5 + Mockito for general tests). If the team has repo-specific copilot-instructions, add it to the repo so future contexts use those constraints.

## Out of Scope
- Editing the OpenAPI spec or generated *-openapi-code module (the BooksApi declaration already exists).
- Pagination or returning multiple books for an author.
- Authentication/authorization changes.
- Resilience policies (timeouts, retries, circuit-breakers, rate limiting).
- Database schema changes or direct DB access.
- Performance/load testing.

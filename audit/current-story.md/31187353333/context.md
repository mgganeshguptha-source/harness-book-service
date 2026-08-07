## What Are We Trying to Achieve
Add a reactive endpoint that returns a single book for a given author name. The endpoint must follow the existing reactive getBookById pattern, delegate lookups to CatalogClient, use Reactor types (Mono<ResponseEntity<BookResponse>>), and avoid any blocking operations.

## Current Behaviour
No endpoint exists at GET /books/by-author/{author}. The service currently exposes a reactive getBookById endpoint that will be used as the pattern to follow.

## Expected Behaviour
- Expose a new controller operation: GET /books/by-author/{author}
- Return type: Mono<ResponseEntity<BookResponse>>
- Controller delegates to BookService; BookService calls CatalogClient.fetchBookByAuthor(author) which returns Mono<CatalogBookDto>
- No blocking: do not use .block(), .blockOptional(), or .collectList() anywhere in the request path
- Match rule: exact, case-insensitive match on the author name
- If CatalogClient returns empty → respond 404 Not Found
- If author path variable is blank or >256 characters → respond 400 Bad Request
- If CatalogClient returns multiple results unexpectedly → use the first result (do not return 409)
- Endpoint is public (no authentication/roles required for this sample)
- CatalogClientImpl will call the catalog endpoint: GET /catalog/books/by-author/{author}

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>
- AC2: Controller delegates to BookService; no blocking operations in controller or service
- AC3: Lookup performed via CatalogClient.fetchBookByAuthor(...) — no direct downstream HTTP calls from controller/service
- AC4: Reactive patterns followed (Mono composition) and WebFlux/Reactor conventions observed
- AC5: Empty catalog result → 404; blank author or author > 256 chars → 400
- AC6: Unit test using JUnit 5 + Reactor StepVerifier covers the happy path and asserts response body; changed class has ≥ 90% line coverage
- AC7: OpenAPI spec is updated to include the operation; generated code not hand-edited outside the spec

## Edge Cases
- Blank author path variable ("" or only whitespace) → 400 Bad Request
- Author length > 256 characters → 400 Bad Request
- Author contains spaces, UTF-8 characters, punctuation (e.g., O'Connor) — accepted as-is; framework-provided URL decoding is assumed
- CatalogClient unexpectedly returns multiple books for an exact author match → use the first result
- Very long author values (e.g., >1024) should be rejected by validation above
- Network failure in CatalogClient → _Not found in codebase — confirm with team_ for error mapping (infra-owned resilience; story specifies none)

## Constraints
- .github/copilot-instructions.md not found in repository; using defaults below.

Backend (defaults): Constructor injection | Standard RFC-7807 error format | Jakarta Validation on inputs | JUnit 5 + Mockito for unit tests | Pagination not relevant for this endpoint

Reactive specifics: Spring WebFlux / Reactor stack only — no blocking calls (.block(), .collectList()) anywhere; controller returns Mono<ResponseEntity<BookResponse>>; use WebClient in CatalogClientImpl and compose Monos.

Testing & tooling: Unit tests must mock CatalogClient; tests use Reactor StepVerifier for reactive assertions; no external HTTP calls in tests.

Design-time constraints: Follow existing reactive-controller and reactive-webclient instructions in the repo; constructor injection for new components; do not add timeouts, retries, circuit-breakers, or rate-limiting logic in this story.

## Out of Scope
- Direct database access or schema changes
- Pagination or listing multiple books by author
- Authentication/authorization changes
- Performance or load testing
- Resilience policies (timeouts, retries, circuit breakers, rate limiting)

## Clarifications Needed
- None — all required behaviour and validations were specified in the story.

## Files/Classes to Update (for implementation guidance only — do not add paths in this context)
- Add controller operation following the existing reactive getBookById pattern
- Add BookService delegation method (if not present) to call CatalogClient
- Add CatalogClient.fetchBookByAuthor(String author): Mono<CatalogBookDto>
- Implement CatalogClientImpl to call GET /catalog/books/by-author/{author}
- Update OpenAPI spec to add the new operation (do not hand-edit generated code outside the spec)


## What Are We Trying to Achieve
Add a reactive HTTP GET endpoint that returns a single book for a given author name. The endpoint must follow the existing reactive getBookById pattern, delegate to BookService which calls CatalogClient, and remain fully non-blocking (Reactor types only). Validation and error responses are provided as described below.

## Expected Behaviour
- HTTP endpoint: GET /books/by-author/{author}
- Controller method returns: Mono<ResponseEntity<BookResponse>>
- Request handling:
  - If {author} is blank or only whitespace → respond 400 Bad Request
  - If {author} length > 256 characters → respond 400 Bad Request
  - Otherwise, controller delegates to BookService, which calls CatalogClient.fetchBookByAuthor(author)
  - CatalogClient.fetchBookByAuthor(String) returns Mono<CatalogBookDto>
  - If CatalogClient returns empty → controller responds 404 Not Found
  - If CatalogClient returns one or more results, take the first result and map to BookResponse, respond 200 OK with body
- Reactive rules: no .block(), no .collectList(), compose Reactor types (Mono/Flux) end-to-end
- Path parameter handling: framework-provided URL decoding is relied on; accept spaces and UTF-8 as-is
- Authentication: public endpoint (no roles/scopes required)

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>
- AC2: Controller delegates to BookService; no blocking operations are introduced
- AC3: Book lookup is performed exclusively via CatalogClient.fetchBookByAuthor(...); no direct downstream HTTP calls in controller/service
- AC4: Reactive patterns are followed (Mono composition); aligns with repo's reactive-controller and reactive-webclient instructions
- AC5: Empty catalog result → 404; blank author or author > 256 chars → 400
- AC6: Unit test using StepVerifier covers the happy path and asserts response body; changed class has ≥ 90% line coverage
- AC7: OpenAPI spec updated to include the new operation; generated code is not hand-edited outside the spec

## Edge Cases
- Author contains spaces, punctuation, or non-ASCII characters (UTF-8) — accepted as-is and matched case-insensitively
- Author value with leading/trailing whitespace — treated as provided by framework; controller should validate blank-only values
- CatalogClient returns multiple results for exact author — use the first result (per story decision)
- Very long author values (>256) → validation error 400
- CatalogClient returns an error Mono.error(...) — surface as 5xx per global error handler (do not alter handler behavior here)

## Constraints
- Reactive stack: Spring WebFlux / Reactor. No blocking operations (.block(), .collectList()) anywhere in the new code
- Follow repository reactive-controller and reactive-webclient instructions
- Constructor injection for new components; fields should be private final
- CatalogClient must expose fetchBookByAuthor(String author): Mono<CatalogBookDto>
- CatalogClientImpl to call catalog endpoint: /catalog/books/by-author/{author}
- No resilience (timeouts/retries/circuit-breakers) or rate limiting logic to be added in this story
- Validation: author must be non-blank and ≤ 256 characters; blank or too-long → 400
- Unit tests: JUnit 5 + Reactor StepVerifier; mock CatalogClient (no external calls); aim for ≥90% line coverage on modified class
- OpenAPI update required for the operation; do not manually edit generated code outside the spec pipeline
- No database access — lookup only via CatalogClient

## Out of Scope
- Database schema or direct DB access
- Pagination or listing multiple books by author
- Authentication/authorization changes
- Performance/load testing or resilience policies
- Adding retries, timeouts, or circuit-breakers in application code


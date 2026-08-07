## What Are We Trying to Achieve
Add a reactive GET endpoint that returns a single book matching an exact, case-insensitive author name. It must follow the existing reactive getBookById pattern, delegate lookups to CatalogClient, and avoid any blocking operations (no .block() or .collectList()). The endpoint is public and returns 404 when no match is found.

## Current Behaviour
No endpoint exists for fetching a book by author. A reactive getBookById endpoint exists and serves as the pattern to follow.

## Expected Behaviour
- Expose: GET /books/by-author/{author}
- Controller returns: Mono<ResponseEntity<BookResponse>>
- Controller delegates to BookService; BookService calls CatalogClient.fetchBookByAuthor(String author) which returns Mono<CatalogBookDto>
- Matching: exact, case-insensitive on author name
- If CatalogClient returns empty → respond 404 Not Found
- If author path variable is empty or blank → respond 400 Bad Request
- If CatalogClient returns multiple results (unexpected) → take the first result and return it
- No blocking calls anywhere; Reactor types composed end-to-end

## Acceptance Criteria
- AC1: GET /books/by-author/{author} implemented and returns Mono<ResponseEntity<BookResponse>>
- AC2: Controller delegates to BookService; no blocking operations used
- AC3: Lookup performed via CatalogClient.fetchBookByAuthor(...) (no direct downstream HTTP calls in controller/service)
- AC4: Reactive patterns followed (Mono composition); aligns with reactive WebFlux/WebClient guidance
- AC5: Blank/empty author -> 400; no match -> 404
- AC6: Unit test using StepVerifier for the happy path asserting response body; changed class line coverage ≥ 90%
- AC7: OpenAPI spec updated to add the operation; generated code not hand-edited outside the spec

## Edge Cases
- Path variable contains spaces or UTF-8 characters — accept as-is (framework-decoded)
- Author value with mixed case — match case-insensitively (exact match after normalization)
- Multiple results returned by CatalogClient (unexpected) — use the first result rather than erroring
- Very long author strings — validate and return 400 if blank; length limits unspecified in story
- CatalogClient error → propagated as 5xx (infrastructure/global error handling applies)

## Constraints
- Reactive stack: Spring WebFlux / Reactor only; no blocking operations (no .block(), no .collectList())
- Constructor injection for new components
- Unit tests: JUnit 5 + Reactor StepVerifier; mock CatalogClient; no external network calls
- CatalogClient: add fetchBookByAuthor(String author): Mono<CatalogBookDto>, implemented in CatalogClientImpl calling /catalog/books/by-author/{author}
- Controller returns Mono<ResponseEntity<BookResponse>> and follows existing reactive-controller patterns
- No timeouts/retries/circuit-breakers/rate-limiting added by this story
- Follow repo reactive-webclient and reactive-controller instructions

## Out of Scope
- Direct DB access or schema changes
- Pagination or listing multiple books by author
- Authentication/authorization changes (endpoint is public for this sample)
- Performance/load testing and resilience policies
- Adding retries, timeouts, or circuit-breakers in application code

## Clarifications Needed
None — story contains explicit behavioural decisions about matching, multiple matches, path decoding, authentication, and error handling.

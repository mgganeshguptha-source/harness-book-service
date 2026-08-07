## What Are We Trying to Achieve
Add a reactive endpoint that returns a single book for a given author name. The endpoint must follow the existing reactive getBookById pattern, delegate to BookService and CatalogClient, be fully non-blocking (Reactor Mono), and implement input validation and HTTP semantics described in the story.

## Current Behaviour
No endpoint exists to fetch a single book by author. Existing reactive endpoint getBookById returns a Mono and delegates to CatalogClient.fetchBook(bookId).

## Expected Behaviour
- Expose GET /books/by-author/{author}
- Controller method signature: Mono<ResponseEntity<BookResponse>>
- Validates author path variable: not blank, max length 256 characters — otherwise return 400
- Delegates to BookService; BookService calls CatalogClient.fetchBookByAuthor(author) returning Mono<CatalogBookDto>
- If CatalogClient returns empty: return 404 Not Found
- If CatalogClient returns multiple results (unexpected): use the first result (no 409)
- No blocking calls (.block(), .collectList()) anywhere — use Reactor composition
- Public endpoint (no auth roles required)

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>
- AC2: Controller delegates to BookService; no blocking operations in controller/service
- AC3: Lookup performed via CatalogClient.fetchBookByAuthor(...) — no direct downstream HTTP calls from controller/service
- AC4: Reactive patterns followed (Mono composition); complies with reactive-controller and reactive-webclient constraints
- AC5: Empty catalog result → 404; blank author or author > 256 chars → 400
- AC6: Unit test with StepVerifier covers happy path and asserts the response body; changed class has ≥ 90% line coverage
- AC7: OpenAPI spec updated to add the operation; generated code is not hand-edited outside the spec

## Edge Cases
- author path variable is blank or only whitespace → 400 Bad Request
- author length > 256 → 400 Bad Request
- author contains spaces / UTF-8 characters — accepted as-is (controller receives URL-decoded value)
- CatalogClient returns multiple matches — use first result
- CatalogClient returns empty Mono → respond 404
- Unexpected CatalogClient error → mapped by global error handler per repo conventions (_Not found in codebase — confirm with team_ if different behaviour desired)

## Constraints
- Reactive stack: Spring WebFlux / Reactor (no blocking operations)
- Constructor injection for new components
- Follow repository's reactive-controller and reactive-webclient instructions
- Unit tests: JUnit 5 + Reactor StepVerifier; mock CatalogClient; no external calls
- Validation: Jakarta Validation (or equivalent) for path parameter; max length 256
- Do not add timeouts, retries, circuit-breakers, or rate-limiting in this story

> ## ⚠️ Missing copilot-instructions.md
>
> I don't see **.github/copilot-instructions.md** in this repo.
>
> **What this means:** the Constraints section used generic Spring Boot / WebFlux defaults. The team's specific coding standards in copilot-instructions.md were not applied.
>
> **Recommendation:** add copilot-instructions.md if the team requires custom constraints. This context used safe defaults and the story-specified reactive constraints.

## Out of Scope
- Database schema or direct DB access
- Pagination or listing multiple books by author
- Authentication/authorization changes
- Performance or load testing
- Resilience policies (timeouts, retries, circuit breakers, rate limiting)

## Clarifications Needed
- None — the story explicitly defines match rule (exact, case-insensitive), validation (blank and max length 256), behaviour for multiple matches (use first), public auth, and CatalogClient method name and endpoint.  

<!-- End of file -->
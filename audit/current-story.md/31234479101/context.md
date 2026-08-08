## What Are We Trying to Achieve
Add a reactive endpoint that returns a single book for a given author name. The endpoint should follow the existing reactive getBookById pattern, delegate to BookService, and use the catalog common-layer client (CatalogClient) for lookup. Must be WebFlux/Reactor-native (no blocking calls).

## Current Behaviour
N/A — this is new functionality (no existing endpoint for lookup by author in this service).

## Expected Behaviour
- Expose GET /books/by-author/{author}
- Controller implements the generated BooksApi.getBookByAuthor(String author) method and returns Mono<ResponseEntity<BookResponse>>
- Controller delegates to BookService; BookService delegates to CatalogClient.fetchBookByAuthor(author)
- No blocking operations (no .block(), no .collectList()) anywhere in the call chain
- Exact, case-insensitive match on author; if CatalogClient returns multiple items, use the first; if empty → 404
- Validation: author path param must be non-blank and ≤ 256 characters; blank or >256 → 400 Bad Request

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>
- AC2: Controller delegates to BookService; no blocking operations used
- AC3: Lookup flows through CatalogClient.fetchBookByAuthor(...) (no direct downstream HTTP calls in controllers)
- AC4: Reactive patterns are followed (Mono composition, Reactor operators); compliant with reactive-controller and reactive-webclient instructions
- AC5: Empty catalog result → 404; blank author or author > 256 chars → 400
- AC6: Unit tests (written in unit_testing phase) use StepVerifier to cover happy path and assert response body; changed class should reach ≥ 90% line coverage (enforced by pipeline)
- AC7: Controller implements existing BooksApi.getBookByAuthor interface method (no @GetMapping at controller level; do not edit generated openapi module)

## Edge Cases
- author path param is an empty string or only whitespace → 400
- author length > 256 characters → 400
- author contains UTF-8 characters and spaces (framework provides URL-decoded value) — accept as-is
- CatalogClient returns more than one item for an exact author match — use the first element
- CatalogClient returns an error (5xx) — mapped to upstream error handling (story: do not implement retries/timeouts here)

## Constraints
> ## ⚠️ Missing copilot-instructions.md
>
> I don't see **.github/copilot-instructions.md** in this repo.
>
> **What this means:** the context.md will use generic Spring Boot
> and Angular defaults for the Constraints section. The team's actual
> coding standards are not being applied.
>
> **Recommendation:** create copilot-instructions.md before running
> the analysis prompt. Otherwise Copilot may produce code that doesn't
> match your conventions.
>
> **Continue with defaults, or pause to set up copilot-instructions.md first?**

Backend (defaults used because copilot-instructions.md is missing):
- Reactive Spring WebFlux / Reactor
- Constructor injection for new components
- Standard error response format (application/problem+json via @ControllerAdvice) — _Not found in codebase — confirm with team_
- WebClient configured via WebClientConfig; use injected WebClient bean (catalogWebClient)
- JUnit 5 + Reactor StepVerifier for unit tests
- No DB access; CatalogClient is the single downstream dependency for this story

## Out of Scope
- Editing the OpenAPI spec or the generated *-openapi-code module (getBookByAuthor is already declared on BooksApi)
- Database schema changes or direct DB access
- Pagination or listing multiple books by author
- Authentication / authorization changes (endpoint is public for this sample)
- Resilience policies (timeouts, retries, circuit breakers, rate limiting) — infra-owned
- Performance/load testing

## Clarifications Needed
None — the story includes explicit behaviour decisions covering match rule, multiple-matches policy, path decoding, validation, and implementation ownership for CatalogClient and controller mapping.

## Implementation Notes (useful for coding phase only)
- Controller: implement BooksApi.getBookByAuthor(String author) in the existing controller class pattern (do NOT add @GetMapping on the controller)
- BookService: add a method that delegates to CatalogClient.fetchBookByAuthor(author) and maps CatalogBookDto -> BookResponse
- CatalogClient: add fetchBookByAuthor(String author) returning Mono<CatalogBookDto>; implement in CatalogClientImpl using the existing catalogWebClient bean and uri /catalog/books/by-author/{author}
- WebClient base URL is provided via WebClientConfig bean: configuration key catalog.base-url (default http://localhost:8081)
- Validation: enforce non-blank and max length 256 on the path param at controller/service layer and return 400 on violation
- Tests (unit_testing phase): mock CatalogClient to return a CatalogBookDto; StepVerifier should assert ResponseEntity.ok(body) for happy path and 404 for empty result


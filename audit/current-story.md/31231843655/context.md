## What Are We Trying to Achieve
Add a reactive endpoint to return a single book by exact (case-insensitive) author name, following the existing reactive getBookById pattern and delegating to the CatalogClient. The endpoint must be non-blocking (WebFlux/Reactor) and return a Mono<ResponseEntity<BookResponse>>.

## Current Behaviour
No GET /books/by-author/{author} endpoint exists. Existing reactive endpoint getBookById delegates to BookService and CatalogClient; this story extends that pattern to author lookup.

## Expected Behaviour
- New controller operation wired by the generated BooksApi interface: BooksApi.getBookByAuthor(String author) implemented in the controller (no controller-level @GetMapping changes).
- Endpoint semantics: GET /books/by-author/{author} (path param provided by framework, already URL-decoded).
- Controller returns Mono<ResponseEntity<BookResponse>> and delegates to BookService.
- BookService calls CatalogClient.fetchBookByAuthor(String author) which returns Mono<CatalogBookDto>.
- CatalogClientImpl adds an implementation that calls upstream catalog endpoint path /catalog/books/by-author/{author} and returns Mono<CatalogBookDto>.
- Reactive composition only: no .block(), no .collectList(), no blocking operators anywhere.
- Validation: blank author or author length > 256 → 400 Bad Request.
- Match rule: exact, case-insensitive match on author name. If catalog returns multiple results, use the first result.
- On empty result from CatalogClient → respond 404 Not Found.
- Public endpoint: no authentication/roles required for this sample.

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller delegates to BookService; implementation contains no blocking calls.
- AC3: Lookup is performed via CatalogClient.fetchBookByAuthor(...); no direct downstream HTTP calls in the controller/service.
- AC4: Reactive patterns followed (Mono composition); complies with reactive-controller and reactive-webclient instructions.
- AC5: Blank author or author > 256 chars → 400; catalog empty → 404; path param accepted as URL-decoded by framework.
- AC6: Unit tests (produced in unit_testing phase) use StepVerifier to cover happy path and assert response body; changed class achieves ≥ 90% line coverage (enforced by harness after tests).
- AC7: Controller implements generated BooksApi.getBookByAuthor method (do not edit generated openapi module or specs).

## Edge Cases
- Author path param contains spaces, UTF-8 characters, punctuation — accepted as-is (framework-decoded).
- Very long author string (>256) → 400 validation error.
- Empty or blank ("" or whitespace only) → 400.
- Catalog returns multiple records for an exact match → controller/service selects the first record only.
- Catalog client returns empty Mono → 404.
- Author value containing path-significant characters (slashes) is expected to be handled by framework URL-decoding/escaping — no custom decoding performed.
- Upstream errors from CatalogClient should surface as upstream failures per existing error-handling patterns (no additional retry/timeout added here).

## Constraints
- Reactive stack: Spring WebFlux / Reactor. No blocking operators (.block(), .collectList(), etc.).
- Follow repository reactive-controller and reactive-webclient instructions.
- Constructor injection for new components.
- Do not edit generated *-openapi-code module or the OpenAPI spec — BooksApi.getBookByAuthor already declared and must be implemented.
- No resilience code (timeouts/retries/circuit-breakers/rate-limiting) added in this story — infra owns resilience.
- Unit tests: JUnit 5 + Reactor StepVerifier; CatalogClient must be mocked in tests (no external calls).

⚠️ Missing .github/copilot-instructions.md

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

(Default constraints applied above.)

## Out of Scope
- Editing the OpenAPI spec or generated *-openapi-code module (BooksApi is already present).
- Database schema changes or direct DB access — lookup is via CatalogClient only.
- Pagination or listing multiple books by author.
- Authentication/authorization changes.
- Performance/load testing or resilience policies.

## Clarifications Needed
- [NEEDS CLARIFICATION]: Owner / Team responsible for this change (who should review and own the PR)?


# BOOK-1: Add "get book by author" endpoint

## Section 1 — What Are We Trying to Achieve
Add a reactive, non-blocking endpoint that returns a single Book for a given author name. The endpoint follows the repository's existing reactive pattern (e.g., getBookById) and delegates lookup to the CatalogClient via BookService. The endpoint is reactive (Spring WebFlux / Reactor) and must not perform any blocking operations.

## Section 2 — Current Behaviour
N/A (new endpoint; existing getBookById reactive pattern available as reference).

## Section 3 — Expected Behaviour
- Endpoint: GET /books/by-author/{author}
- Controller method signature: Mono<ResponseEntity<BookResponse>>
- Controller implements the generated BooksApi.getBookByAuthor interface method (no controller-level @GetMapping added or OpenAPI edits).
- Controller delegates to BookService; BookService calls CatalogClient.fetchBookByAuthor(author) which returns Mono<CatalogBookDto>.
- CatalogClient.fetchBookByAuthor(String author) implemented in CatalogClientImpl calling /catalog/books/by-author/{author}.
- Matching: exact, case-insensitive on author name. Framework-provided URL decoding is used; accept value as-is.
- If CatalogClient returns empty → controller returns 404 Not Found.
- Blank author or author length > 256 → return 400 Bad Request.
- If CatalogClient returns multiple items (unexpected) → use the first result (no 409/aggregation logic).
- Public endpoint: no auth/roles required for this sample.

## Section 4 — Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller delegates to BookService; no blocking calls (.block(), .collectList(), etc.).
- AC3: Lookup uses CatalogClient.fetchBookByAuthor(...) — no direct downstream HTTP in controllers.
- AC4: Reactive patterns preserved (Mono composition, Reactor types) and follows reactive-controller & reactive-webclient conventions.
- AC5: Empty catalog result → 404; blank author → 400; author > 256 chars → 400.
- AC6: Unit tests (in unit_testing phase) use StepVerifier for happy path; changed class should reach ≥ 90% line coverage (test phase responsibility).
- AC7: Controller implements existing BooksApi.getBookByAuthor interface method (do not edit generated OpenAPI code).

## Section 5 — Edge Cases
- Author value blank ("" or only whitespace) → 400 Bad Request.
- Author > 256 characters → 400 Bad Request.
- Author with special UTF-8 characters (accents, emojis, apostrophes) — accepted as-is; framework-decoded.
- Spaces in author name — allowed (framework URL-decoding applied).
- Multiple matches returned by catalog (unexpected) — take first result.
- Very long but under 256 characters — accepted.
- Catalog client network failures / downstream errors — out of scope; infrastructure handles resilience. The controller should propagate a 503/500 mapping per global error handling (not changed here).

## Section 6 — Constraints
_Note: .github/copilot-instructions.md not found in repo; defaults applied below._

Backend (auto defaults / applied):
- Constructor injection for new components.
- Standard error response format (application/problem+json) and centralized @ControllerAdvice.
- Jakarta Validation on inputs (use annotations for author param validation where applicable) — enforce max length 256 and not blank.
- JUnit 5 + Mockito for unit tests (unit_testing phase).
- Pagination not applicable (single-item endpoint).
- Reactive stack enforced: Spring WebFlux / Reactor. No .block(), no .collectList() in controller/service/client.
- Follow reactive-controller and reactive-webclient instructions in the repo.
- Do NOT modify OpenAPI spec or generated *-openapi-code module; implement the interface method in controller only.

Performance / non-functional constraints (story-level):
- No timeouts/retries/circuit-breakers/rate-limiting added by this change — infrastructure owns resilience.

## Section 7 — Out of Scope
- Editing OpenAPI spec or generated *-openapi-code module.
- Direct DB access or schema changes.
- Pagination or listing multiple books by author.
- Authentication/authorization changes (endpoint is public for this sample).
- Adding resilience (timeouts/retries/circuit-breakers) or rate-limiting logic.
- Performance/load testing and infra-level configuration.

## Section 8 — Clarifications Needed
N/A (the story provided explicit rules for matching, multiple matches, path encoding, validation, and CatalogClient method name and path).

---

Generated from STORY: BOOK-1 — Add "get book by author" endpoint
Timestamp: 2026-08-08T06:15:40Z

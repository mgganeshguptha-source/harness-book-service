# Context for story: Add "get book by author" endpoint

## Section 1 — What Are We Trying to Achieve
Add a reactive, public endpoint that returns a single book for a given author name. The endpoint must follow the existing reactive getBookById pattern, return Mono<ResponseEntity<BookResponse>>, delegate to BookService, and rely on the catalog client (CatalogClient.fetchBookByAuthor) for lookup. No blocking calls or collectList() allowed; WebFlux/Reactor only.

## Section 2 — Current Behaviour
No GET /books/by-author/{author} endpoint exists. A reactive GET by id endpoint (getBookById) exists and serves as the implementation pattern to follow. CatalogClient currently exposes fetchBook(String bookId) — no fetch by author method exists yet.

## Section 3 — Expected Behaviour
- New endpoint: GET /books/by-author/{author}
- Controller returns Mono<ResponseEntity<BookResponse>> and delegates to BookService.
- BookService calls CatalogClient.fetchBookByAuthor(String author) which returns Mono<CatalogBookDto>.
- CatalogClientImpl implements fetchBookByAuthor by calling the catalog service at /catalog/books/by-author/{author}.
- Exact, case-insensitive match on author name. If the catalog returns multiple items, the first is used.
- If CatalogClient returns empty → controller returns 404 Not Found.
- Blank or empty author path variable → 400 Bad Request.
- Public endpoint: no roles/scopes required.
- Reactive only: no .block(), no .collectList(), no blocking HTTP clients.

## Section 4 — Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller calls BookService; no blocking operations present in controller or service.
- AC3: BookService delegates to CatalogClient.fetchBookByAuthor(...); no direct downstream HTTP calls in controller/service (CatalogClient handles HTTP).
- AC4: Reactive patterns used throughout (Mono composition); complies with reactive-controller and reactive-webclient instructions in repo.
- AC5: Blank/empty author → 400; Catalog returns empty → 404.
- AC6: Unit test with StepVerifier covers happy path and asserts response body; changed class has ≥ 90% line coverage.
- AC7: OpenAPI spec updated to include the operation; generated code not hand-edited outside the spec.

## Section 5 — Edge Cases
- Path variable arrives URL-decoded by the framework; spaces and UTF-8 characters are accepted as-is.
- Multiple catalog matches for the same exact author: use the first result returned by the catalog client (no 409).
- Author names with punctuation or diacritics (e.g., O'Brien, García) are supported and treated exactly (case-insensitive comparison). Ensure encoding preserved.
- Very long author strings: validation rules (max length) not specified in story — see Clarifications.
- Author containing only whitespace: treated as blank and must return 400.

## Section 6 — Constraints
- Reactive stack: Spring WebFlux / Reactor. No blocking calls (.block()/collectList()) anywhere in new code.
- Follow existing reactive-controller and reactive-webclient instructions in repository.
- Constructor injection for new components.
- Unit tests: JUnit 5 + Reactor StepVerifier; mock CatalogClient; no external calls.
- Coverage target: changed class must have ≥ 90% line coverage.
- No timeouts/retries/circuit-breaker or rate-limiting code to be added — infrastructure owns resilience and policies.

## ⚠️ Missing copilot-instructions.md

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

(Defaults were used above because copilot-instructions.md was not found in the repository.)

## Section 7 — Out of Scope
- Database schema changes or direct DB access — lookup is via CatalogClient only.
- Pagination or listing multiple books by author.
- Authentication/authorization changes — endpoint is public for this sample.
- Performance/load testing and resilience policies (timeouts/retries/circuit-breakers/rate-limiting).

## Section 8 — Clarifications Needed
- [NEEDS CLARIFICATION]: Maximum allowed length for the `{author}` path variable and any server-side validation rules (max chars, disallowed characters). Define a concrete max length (e.g., 256) or confirm unlimited subject to framework limits.


---
Context generated in NON-INTERACTIVE / CI mode from the provided story: BOOK-1: Add "get book by author" endpoint.

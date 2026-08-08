## What Are We Trying to Achieve
Add a reactive endpoint that returns a single book for a given author name. The endpoint must follow the existing reactive getBookById pattern, delegate to BookService, and use CatalogClient.fetchBookByAuthor(...) without any blocking calls. It enables consumers to fetch a single book resource by author using a case-insensitive exact match.

## Current Behaviour
No endpoint exists to retrieve a book by author. The service exposes a reactive GET /books/{id} (getBookById) pattern and uses CatalogClient.fetchBook(String bookId) for lookups.

## Expected Behaviour
- New operation exposed via the existing generated BooksApi interface: getBookByAuthor(String author) implemented in the controller.
- Method signature: Mono<ResponseEntity<BookResponse>> returned directly from controller method implementation.
- Controller delegates to BookService; BookService delegates to CatalogClient.fetchBookByAuthor(author) which returns Mono<CatalogBookDto>.
- Reactive-only composition (Mono) throughout; no .block() or .collectList().
- Exact case-insensitive author match; if CatalogClient returns multiple, first result used. Empty result → 404. Blank or >256 chars → 400.

## Acceptance Criteria
- AC1: Controller implements BooksApi.getBookByAuthor and returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller delegates to BookService; no blocking operations anywhere in the controller/service layer for this flow.
- AC3: BookService uses CatalogClient.fetchBookByAuthor(String) — no direct downstream HTTP calls in service/controller.
- AC4: Reactive types composed correctly (Mono) and follow WebFlux/Reactor patterns per project guidance.
- AC5: Behavior: empty catalog result → 404; blank author or author length > 256 → 400; author allowed to contain spaces/UTF-8.
- AC6: Unit tests (in unit_testing phase) using StepVerifier cover happy path; changed class line coverage >= 90% (enforced by harness after tests).
- AC7: No changes to generated OpenAPI module or API spec; controller implements generated interface method (no @GetMapping on controller).

## Edge Cases
- Blank author (empty string or only whitespace): return 400 Bad Request.
- Author > 256 characters: return 400 Bad Request.
- Author contains UTF-8 characters or spaces: accepted as-is (framework-decoded path variable).
- CatalogClient returns multiple entries for exact match: use the first element returned.
- CatalogClient returns empty Mono: return 404 Not Found.
- CatalogClient error: propagate appropriate 5xx via global error handler (no special handling in this story).

## Constraints
- Defaults used because .github/copilot-instructions.md is missing in this repo — see note below.

Backend defaults (applied):
- Constructor injection throughout
- Standard RFC7807 problem+json error format
- Jakarta Validation on inputs
- JUnit 5 + Mockito for unit tests
- Pagination not applicable for this endpoint

Story-specific constraints:
- Reactive stack: Spring WebFlux / Reactor only. No blocking calls (.block(), .collectList()).
- Do not edit generated *-openapi-code module or OpenAPI spec. Implement existing BooksApi.getBookByAuthor method in controller.
- CatalogClient: add fetchBookByAuthor(String author) returning Mono<CatalogBookDto> and implement in CatalogClientImpl to call /catalog/books/by-author/{author}.
- No timeouts/retries/circuit-breakers/rate-limiting to be added by this story.
- Max author length: 256 characters; blank → 400.

Note: .github/copilot-instructions.md was not found in the repository; defaults above were applied. Create copilot-instructions.md if project-specific constraints must override these defaults.

## Out of Scope
- Editing OpenAPI spec or generated *-openapi-code module.
- Database access or schema changes.
- Pagination or returning multiple books by author.
- Authentication/authorization changes — endpoint is public for this sample.
- Adding resilience (timeouts/retries/circuit-breakers) or rate limiting.
- Performance/load testing.

## Clarifications Needed
- N/A — the story includes explicit decisions for match rule, multiple matches, path encoding, catalog client method, authentication, empty/blank handling, validation, and no resilience. No additional clarifications required for implementation in CI mode.

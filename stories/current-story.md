# Story BOOK-1: Add "get book by author" endpoint

## Goal
Add a reactive endpoint that returns a single book for a given author name,
following the existing reactive `getBookById` pattern and delegating to the
catalog common-layer client (`CatalogClient`). Reactive only (WebFlux/Reactor);
no blocking calls.

## Expected behaviour
- New endpoint: `GET /books/by-author/{author}`
- Returns: `Mono<ResponseEntity<BookResponse>>`
- The controller delegates to `BookService`, which calls `CatalogClient` for the
  lookup. No `.block()` and no `.collectList()` anywhere.

## Explicit behaviour decisions (resolving prior clarifications)
1. **Match rule:** exact, case-insensitive match on the author name. No partial/contains matching.
2. **Multiple matches:** the catalog client is expected to return at most one book
   for an exact author match in this sample. If more than one is returned, use the
   **first** result. Do not add 409/aggregation logic.
3. **Path encoding:** the `{author}` value arrives already URL-decoded by the framework.
   Accept it as-is (spaces, UTF-8 allowed). No custom decoding.
4. **CatalogClient method:** add a new method to `CatalogClient` named
   `fetchBookByAuthor(String author)` returning `Mono<CatalogBookDto>`, mirroring the
   existing `fetchBook(String bookId)` method. Implement it in `CatalogClientImpl`
   calling the catalog endpoint `/catalog/books/by-author/{author}`.
5. **Authentication:** public endpoint for this sample — no roles/scopes required.
6. **No match:** return **404 Not Found** when the catalog client returns empty.
7. **Empty/blank author:** return **400 Bad Request**.
8. **Coverage:** target ≥ 90% line coverage on the changed class (matches the harness gate).
9. **Timeouts / retries / rate limiting:** none in this story — infrastructure owns
   resilience. Do not add timeout, retry, circuit-breaker, or rate-limit code.

## Acceptance criteria
- AC1: `GET /books/by-author/{author}` exists and returns `Mono<ResponseEntity<BookResponse>>`.
- AC2: Controller delegates to `BookService`; no blocking operations.
- AC3: Lookup goes through `CatalogClient.fetchBookByAuthor(...)`; no direct downstream HTTP calls.
- AC4: Reactive patterns followed (returns `Mono`, composes Reactor types); complies with
  the reactive-controller and reactive-webclient instructions.
- AC5: Empty catalog result → 404; blank author → 400.
- AC6: Unit test with `StepVerifier` covers the happy path and asserts the response body;
  changed class has ≥ 90% line coverage.
- AC7: OpenAPI spec updated to add the operation; generated code is not hand-edited
  outside the spec.

## Constraints
- Reactive stack: Spring WebFlux / Reactor. No `.block()`, no `.collectList()`.
- Follow the repo's reactive-controller and reactive-webclient instructions.
- Constructor injection for any new components.
- Unit tests: JUnit 5 + Reactor `StepVerifier`; mock `CatalogClient`; no external calls.
- No DB access — lookup is via `CatalogClient` only.

## Out of scope
- Database schema or direct DB access.
- Pagination / listing multiple books by author.
- Auth/authorization changes.
- Performance or load testing.
- Resilience policies (timeouts, retries, circuit breakers, rate limiting).

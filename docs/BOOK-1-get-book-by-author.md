# STORY: BOOK-1 — Add "get book by author" endpoint

## Summary
Add a reactive endpoint that returns a single book for a given author name. Follows the existing reactive getBookById pattern and delegates to the catalog common-layer client (`CatalogClient`). No blocking calls; Reactor/Mono-based composition only.

## Goal
- New endpoint: `GET /books/by-author/{author}`
- Return type: `Mono<ResponseEntity<BookResponse>>`
- Controller implements generated `BooksApi.getBookByAuthor(String author)` method (do NOT add controller-level mapping annotation)
- Controller delegates to `BookService`
- `BookService` delegates to `CatalogClient.fetchBookByAuthor(String author)`
- `CatalogClient.fetchBookByAuthor(...)` returns `Mono<CatalogBookDto>`

## Files to change (coding phase)
- src/main/java/.../controller/BooksController.java
  - Implement `BooksApi.getBookByAuthor(String author)`
  - Validate `author` (blank / length) and return appropriate ResponseEntity on error
  - Delegate to `BookService#getBookByAuthor(String author)`
  - Return `Mono<ResponseEntity<BookResponse>>`

- src/main/java/.../service/BookService.java
  - Add `Mono<BookResponse> getBookByAuthor(String author)` or similar method
  - Call `CatalogClient.fetchBookByAuthor(author)` and map `CatalogBookDto` → `BookResponse`
  - Do not block—compose Monos

- src/main/java/.../client/CatalogClient.java
  - Add method: `Mono<CatalogBookDto> fetchBookByAuthor(String author);`

- src/main/java/.../client/impl/CatalogClientImpl.java
  - Implement `fetchBookByAuthor(String author)` by calling catalog endpoint: `/catalog/books/by-author/{author}` using reactive WebClient
  - Mirror existing `fetchBook(String bookId)` implementation style

## Behaviour and validation
- Matching: exact, case-insensitive match on author name (the catalog is expected to do this; client calls provided endpoint). The service/controller should not perform partial/contains matching.
- Multiple matches: catalog is expected to return at most one. If multiple are returned, select the first result (do not return 409).
- Path encoding: accept `{author}` as already URL-decoded by the framework.
- Empty/blank author: return `400 Bad Request`.
- Max length: 256 characters. If author length > 256 → `400 Bad Request`.
- No match: return `404 Not Found` when `CatalogClient` returns empty.

## Reactive contract
- No `.block()` or `.collectList()` anywhere in these changes.
- Controller method must be `Mono<ResponseEntity<BookResponse>>` and composed from service Mono results.
- Use Reactor operators (map, flatMap, switchIfEmpty) to translate empty → 404.

## Example controller flow (conceptual)
- Controller.getBookByAuthor(author):
  - validate author; if invalid: return Mono.just(ResponseEntity.badRequest().build())
  - return bookService.getBookByAuthor(author)
      .map(book -> ResponseEntity.ok(book))
      .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

## CatalogClient HTTP details
- New client endpoint: `GET /catalog/books/by-author/{author}`
- Implement in `CatalogClientImpl` using reactive WebClient, matching existing client patterns (headers, error mapping) used in this repo.

## Testing notes (unit_testing phase — do not add tests in coding phase)
- Add unit test for the controller using JUnit 5 + Reactor `StepVerifier`.
- Mock `BookService` or `CatalogClient` (depending on test target) to return a `Mono<CatalogBookDto>` → map to `BookResponse` and assert HTTP 200 + response body.
- Tests to cover:
  - Happy path (StepVerifier: expect next ResponseEntity with body matching expected BookResponse)
  - Blank author → 400
  - Author > 256 chars → 400
  - Catalog returns empty → 404
- Use `StepVerifier` for reactive assertions. Do not start servers or perform external HTTP calls.
- Aim for ≥ 90% line coverage on the changed controller class (coverage gate enforced after unit tests run).

## Implementation constraints / important notes
- Do NOT edit generated `*-openapi-code` module or OpenAPI spec. The operation `getBookByAuthor` is already declared on `BooksApi` interface — implement that interface method.
- Constructor injection for new/changed components.
- No timeouts/retries/circuit-breakers/rate-limiting; infra manages resilience.
- Authentication: public endpoint — no roles required for this sample.
- When mapping DTO → API response, follow existing `BookResponse` mapping patterns used for `getBookById`.

## Example responses
- 200 OK
  - Body: BookResponse JSON (as returned by existing mapping)
- 400 Bad Request
  - For blank or >256 length author
- 404 Not Found
  - When catalog returns no book

## TODOs for coder
- Implement controller method body to call service and compose Reactor types.
- Add `fetchBookByAuthor` to `CatalogClient` and implement in `CatalogClientImpl` calling `/catalog/books/by-author/{author}`.
- Add mapping from `CatalogBookDto` → `BookResponse` in service or a shared mapper consistent with repository conventions.

## Rationale / Acceptance Criteria mapping
- AC1: Controller method returns `Mono<ResponseEntity<BookResponse>>` and maps to route declared in `BooksApi`.
- AC2: Controller delegates to `BookService`; no blocking.
- AC3: Lookup uses `CatalogClient.fetchBookByAuthor(...)`.
- AC4: Implementation uses Reactor types only.
- AC5: Validation rules for blank and >256 enforced in controller returning 400; empty catalog result → 404.
- AC6: Unit tests (next phase) will verify happy path using StepVerifier and reach coverage target.
- AC7: Controller implements `BooksApi.getBookByAuthor` (no OpenAPI edits).

---

Document created for coding-phase implementers. Unit tests and coverage verification belong to the unit_testing phase.

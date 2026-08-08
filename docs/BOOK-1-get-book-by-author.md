# Story BOOK-1: Add "get book by author" endpoint

## Goal
Add a reactive endpoint that returns a single book for a given author name, following the existing reactive `getBookById` pattern and delegating to the catalog common-layer client (`CatalogClient`). Reactive only (WebFlux/Reactor); no blocking calls.

## Endpoint
- Method: GET
- Path: /books/by-author/{author}
- Controller method signature: Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author)
- Controller implements generated `BooksApi.getBookByAuthor` (do not add controller-level mapping annotations).

## Behaviour (as required)
1. Exact, case-insensitive match on the author name. No partial/contains matching.
2. CatalogClient is expected to return at most one match. If more than one is returned, the service uses the first result.
3. Path parameter `{author}` arrives URL-decoded — accept as-is. Spaces and UTF-8 allowed.
4. CatalogClient will expose a new method:
   - `Mono<CatalogBookDto> fetchBookByAuthor(String author)`
   - Implemented by `CatalogClientImpl` and calling `/catalog/books/by-author/{author}`.
5. Public endpoint — no auth required for this sample.
6. If catalog client returns empty → return 404 Not Found.
7. Blank or empty author → 400 Bad Request.
8. Author max length: 256 characters. Blank or >256 → 400 Bad Request.
9. No timeouts/retries/circuit-breaker/rate-limiting added here.
10. No `.block()` or `.collectList()` anywhere. Use Reactor composition.

## Validation rules
- Reject when author == null or trimmed length == 0 → 400
- Reject when author.length() > 256 → 400
- No other character restrictions

## Error responses
- 400 Bad Request: invalid/blank/too-long author
- 404 Not Found: catalog returned empty (no matching author)
- 500 Internal Server Error: unexpected runtime failures (preserve existing error handling patterns)

## Reactive flow (recommended)
Controller (implements BooksApi) → BookService.getBookByAuthor(author) → CatalogClient.fetchBookByAuthor(author) (returns Mono<CatalogBookDto>) → map/convert CatalogBookDto to BookResponse → map to ResponseEntity.ok(body) or ResponseEntity.notFound().build()

Example reactive composition (pseudocode):
```
return bookService.findByAuthor(author)
  .map(dto -> ResponseEntity.ok(mapToResponse(dto)))
  .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
```

## Files to modify (coding phase only)
- src/main/java/.../controller/BooksController.java (implement BooksApi.getBookByAuthor)
- src/main/java/.../service/BookService.java (add method getBookByAuthor returning Mono<BookResponse>/Mono<CatalogBookDto> as per existing project style)
- src/main/java/.../client/CatalogClient.java (add method signature `Mono<CatalogBookDto> fetchBookByAuthor(String author)`)
- src/main/java/.../client/CatalogClientImpl.java (implement call to `/catalog/books/by-author/{author}` using reactive webclient; mirror existing `fetchBook` implementation)

> Note: Do not edit generated OpenAPI modules or the BooksApi interface (operation already declared there). Implement the interface method in the controller.

## Unit testing (unit_testing phase)
- Add tests exercising:
  - happy path: CatalogClient returns a CatalogBookDto → controller returns 200 and correct BookResponse (use StepVerifier on reactive controller result)
  - not found: CatalogClient returns Mono.empty() → controller returns ResponseEntity.notFound()
  - validation: blank author and >256 chars → controller returns 400
- Use JUnit 5 + Reactor StepVerifier; mock CatalogClient (no external calls).
- Target: tests cover the changed class with ≥ 90% line coverage.

## Acceptance criteria mapping
- AC1: Controller implements BooksApi.getBookByAuthor and returns Mono<ResponseEntity<BookResponse>> (see Endpoint)
- AC2: Controller delegates to BookService; no blocking calls
- AC3: BookService delegates to CatalogClient.fetchBookByAuthor; no direct HTTP calls in service
- AC4: Uses Reactor types only; no blocking
- AC5: 404 on empty catalog; 400 on blank or >256
- AC6: Unit tests with StepVerifier; coverage gate enforced in pipeline (unit_testing)
- AC7: No changes to generated modules or OpenAPI spec

## Implementation notes / constraints
- Use constructor injection for new components
- Follow repository reactive-controller and reactive-webclient instructions
- Keep code surgical and limited to the files listed above

## Example success response (BookResponse)
```json
{
  "id": "book-123",
  "title": "Example Title",
  "author": "Jane Doe",
  "isbn": "978-...",
  "publishedDate": "2020-01-01"
}
```


---
Document created to guide coding and unit-testing phases for STORY BOOK-1.

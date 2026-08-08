STORY: # Story BOOK-1: Add "get book by author" endpoint

Summary

Implements a reactive endpoint to lookup a single book by exact (case-insensitive) author name.

What changed (coding phase)

- Adds controller implementation for the existing BooksApi.getBookByAuthor(...) operation. The controller returns Mono<ResponseEntity<BookResponse>> and delegates to BookService. No controller-level @RequestMapping changes were made to the generated OpenAPI module.
- Adds BookService method that invokes CatalogClient.fetchBookByAuthor(String author) and maps the Catalog DTO to BookResponse reactively.
- Extends CatalogClient with a new method: Mono<CatalogBookDto> fetchBookByAuthor(String author).
- Implements CatalogClientImpl.fetchBookByAuthor(...) to call the catalog endpoint /catalog/books/by-author/{author} using the reactive WebClient (no .block(), no .collectList()).
- Validates author path parameter: blank or length > 256 → return 400 Bad Request. If CatalogClient returns empty → 404 Not Found. If multiple items returned by downstream, first is used (per story decision).
- Uses constructor injection for all new components. No authentication/roles added (public endpoint).

Files (high level)

- src/main/java/.../controller/BooksController.java (implements BooksApi.getBookByAuthor)
- src/main/java/.../service/BookService.java
- src/main/java/.../client/CatalogClient.java (new method signature)
- src/main/java/.../client/CatalogClientImpl.java (new implementation)
- DTO mapping classes updated/added as needed to produce BookResponse from CatalogBookDto

Testing and next steps (unit_testing phase)

- Unit tests will be added in the unit_testing phase. Tests will use JUnit 5 + Reactor StepVerifier and a mocked CatalogClient to cover the happy path and error cases (blank author → 400, >256 chars → 400, no match → 404). The happy-path StepVerifier will assert the returned ResponseEntity and body content.
- Coverage target: changed classes must reach ≥ 90% line coverage (enforced by harness gate). This is verified after unit tests run.

Notes and constraints

- No changes to OpenAPI spec or generated *-openapi-code module.
- No blocking calls, no collectList(), no timeouts/retries/circuit-breakers added.
- Path parameter arrives URL-decoded by framework and is accepted as-is (spaces/UTF-8 allowed).
- Matches all explicit behaviour decisions from the story and acceptance criteria.

Signed-off-by: BOOK-1 implementation

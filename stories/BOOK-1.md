# Story BOOK-1: Add "get book by author" endpoint

## Goal
Add a new reactive endpoint that returns a book by its author name, following the
same pattern as the existing getBookById endpoint.

## Acceptance criteria
- New endpoint: GET /books/by-author/{author}
- Returns Mono<ResponseEntity<BookResponse>>
- Controller implements the generated BooksApi interface (add the method there via
  the OpenAPI spec; do NOT hand-edit generated code outside the spec)
- Delegates to BookService; no .block(), no .collectList()
- Calls the catalog common-layer client for the lookup
- Unit test with StepVerifier covering the happy path (>= 90% line coverage on the
  changed class)

## Notes
- Reactive only (WebFlux/Reactor). Follow the reactive-controller and
  reactive-webclient instructions.
- No database access; use the CatalogClient.

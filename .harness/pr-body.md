Story: BOOK-1 — Add "get book by author" endpoint

Summary

This change adds a reactive endpoint to look up a single book by exact author name and returns a Mono<ResponseEntity<BookResponse>>. It follows the existing reactive getBookById pattern and delegates lookup to the CatalogClient. No OpenAPI edits or generated-module changes are included.

What changed (high level)

- New controller implementation of BooksApi.getBookByAuthor(...) that returns Mono<ResponseEntity<BookResponse>> and performs input validation.
- BookService updated to expose a reactive method that calls CatalogClient.fetchBookByAuthor(author) and maps the CatalogBookDto -> BookResponse.
- CatalogClient interface extended with fetchBookByAuthor(String author): Mono<CatalogBookDto>.
- CatalogClientImpl implemented to call catalog endpoint /catalog/books/by-author/{author} using WebClient in a reactive, non-blocking way.

Endpoint

- GET /books/by-author/{author}
- Behavior:
  - Blank or empty author => 400 Bad Request
  - author length > 256 => 400 Bad Request
  - Exact, case-insensitive match rule for lookup
  - If CatalogClient returns empty => 404 Not Found
  - If multiple results (unexpected) => use first result
  - Public endpoint (no auth)

Reactive & non-blocking

- All new/changed code uses Reactor types (Mono). No .block(), no .collectList(), no blocking calls added.
- Constructor injection used for new components.

Files expected to be changed (coding phase)

- src/main/java/.../controller/...Controller.java (implements BooksApi.getBookByAuthor)
- src/main/java/.../service/BookService.java (new or updated reactive method)
- src/main/java/.../client/CatalogClient.java (add fetchBookByAuthor signature)
- src/main/java/.../client/CatalogClientImpl.java (implement new client call)

Testing & coverage (unit_testing phase)

- Unit tests will be added in the unit_testing phase using JUnit 5 + Reactor StepVerifier and a mocked CatalogClient.
- Happy-path StepVerifier test must assert HTTP 200 and correct BookResponse body.
- Tests must assert 400 for blank or >256 author and 404 when CatalogClient returns empty.
- Coverage requirement: modified class must reach >= 90% line coverage (enforced by harness gate).

Acceptance criteria mapping

- AC1: Controller exposes GET /books/by-author/{author} returning Mono<ResponseEntity<BookResponse>> and implements BooksApi.getBookByAuthor.
- AC2: Controller delegates to BookService; no blocking.
- AC3: BookService uses CatalogClient.fetchBookByAuthor(...); no downstream HTTP calls elsewhere.
- AC4: Reactive patterns followed (Mono composition).
- AC5: Blank/empty and >256 author -> 400; empty catalog result -> 404.
- AC6: Unit tests with StepVerifier validate happy path and edge cases (written in unit_testing phase).
- AC7: No changes to generated openapi modules or the OpenAPI spec.

Constraints and explicit decisions

- Match rule: exact, case-insensitive author match.
- Path author arrives URL-decoded; accept as-is (no custom decoding).
- No resilience, timeout, retry, or rate-limiting code added.
- Do not edit generated *-openapi-code modules.

Notes for reviewers

- Ensure reactive usage (no blocking) and that the controller implements the generated BooksApi#getBookByAuthor method rather than adding controller-level @GetMapping.
- Validate the CatalogClientImpl performs a reactive WebClient call to /catalog/books/by-author/{author} and returns Mono.empty() when 404 is returned from the catalog service.

Out of scope

- OpenAPI spec edits, DB access, pagination, auth changes, resilience policies, or performance testing.

Done-by

- Coding-phase changes only: production sources under src/main/**. Unit tests will be produced in the unit_testing phase.

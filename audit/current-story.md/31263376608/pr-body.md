# Story BOOK-1: Add "get book by author" endpoint

Summary
-------
Add a reactive endpoint to fetch a single book by author name. The controller implements the already-generated BooksApi.getBookByAuthor(...) method and delegates to BookService. BookService calls CatalogClient.fetchBookByAuthor(author) (new method) and composes Reactor types end-to-end. No blocking calls are introduced.

What changed (coding phase)
---------------------------
- Controller: implements BooksApi#getBookByAuthor and returns Mono<ResponseEntity<BookResponse>> (delegates to BookService). No controller-level @RequestMapping changes or OpenAPI edits.
- Service: BookService exposes a reactive method to lookup by author and map Catalog DTO -> BookResponse.
- Client: CatalogClient interface: added Mono<CatalogBookDto> fetchBookByAuthor(String author).
- Client Impl: CatalogClientImpl implements fetchBookByAuthor(...) and calls downstream catalog endpoint `/catalog/books/by-author/{author}` (reactive WebClient usage mirroring existing fetchBook).

Behavior details (per story decisions)
-------------------------------------
- Matching: exact, case-insensitive author match (catalog is expected to honour this; controller/service do not perform additional partial matching).
- Multiple matches: if catalog returns more than one result, the first result is used.
- Path decoding: use framework-provided value (assumed URL-decoded). Spaces/UTF-8 accepted.
- Validation: author must be non-blank and <= 256 characters. Blank or >256 → 400 Bad Request.
- No match: return 404 Not Found.
- Authentication: public endpoint (no security roles/scopes added).
- Reactive contract: Mono-based, no .block(), no .collectList(), and no blocking anywhere in the new code.

Testing and coverage (unit_testing phase)
-----------------------------------------
- Unit tests will be added in the unit_testing phase using JUnit 5 + Reactor StepVerifier.
- CatalogClient will be mocked in tests; no external calls.
- Happy path StepVerifier test asserts ResponseEntity status and body.
- Coverage target: changed class must reach ≥ 90% line coverage (enforced by harness gate).

Out-of-scope / Constraints
--------------------------
- Do not modify the generated OpenAPI module or specs.
- No timeouts/retries/circuit-breakers/rate-limiting added.
- No DB access; lookup happens via CatalogClient only.

Acceptance criteria mapping
---------------------------
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>> (controller implements BooksApi.getBookByAuthor).
- AC2: Controller delegates to BookService; no blocking.
- AC3: Lookup goes through CatalogClient.fetchBookByAuthor(...).
- AC4: Reactive patterns followed (Mono composition).
- AC5: Empty catalog result → 404; blank or >256 chars author → 400.
- AC6: Unit test with StepVerifier will cover happy path and body; coverage gate >=90% on changed class.
- AC7: Controller implements generated interface method; no OpenAPI edits.

Notes
-----
- This PR contains only production (coding-phase) source changes. Unit tests will be committed in the next phase per harness rules.
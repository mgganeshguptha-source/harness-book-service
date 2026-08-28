## What Are We Trying to Achieve
Add a reactive HTTP endpoint that returns a single book for a given author name. The endpoint must follow the existing reactive getBookById pattern, delegate lookup to the BookService which uses the CatalogClient, and remain fully non-blocking (Spring WebFlux / Reactor). This implements a public lookup by exact, case-insensitive author name.

## Current Behaviour
No `GET /books/by-author/{author}` endpoint exists. A reactive `getBookById` pattern exists in the codebase that this story should mirror. Catalog lookups are performed via a CatalogClient in other flows.

## Expected Behaviour
- Endpoint: `GET /books/by-author/{author}`
- Returns: `Mono<ResponseEntity<BookResponse>>`
- Validation: author path variable must be non-blank and max length 256 characters. Blank or >256 → 400 Bad Request.
- Match rule: exact, case-insensitive match on the author name (no partial matches).
- Lookup flow: Controller delegates to BookService → BookService delegates to `CatalogClient.fetchBookByAuthor(String author)` which returns `Mono<CatalogBookDto>`.
- CatalogClient implementation: add `fetchBookByAuthor(...)` in the client interface and implement in `CatalogClientImpl` to call the catalog endpoint `/catalog/books/by-author/{author}`.
- If the CatalogClient returns empty → controller responds 404 Not Found.
- If CatalogClient returns multiple results (unexpected) → use the first result (no 409/aggregation logic).
- No blocking calls anywhere (`.block()`, `.collectList()`, etc.).
- Public endpoint: no authentication/roles required for this sample.
- Path parameter is treated as already URL-decoded by the framework; accept spaces and UTF-8 as-is.

## Acceptance Criteria
- AC1: `GET /books/by-author/{author}` exists and returns `Mono<ResponseEntity<BookResponse>>`.
- AC2: Controller delegates to BookService; no blocking operations used.
- AC3: Lookup goes through `CatalogClient.fetchBookByAuthor(...)`; no direct downstream HTTP calls from controller.
- AC4: Reactive patterns are followed (Mono composition, Reactor types); complies with reactive-controller and reactive-webclient instructions.
- AC5: Empty catalog result → 404; blank author → 400; author > 256 chars → 400.
- AC6: Unit tests (produced in unit_testing phase) use JUnit 5 + Reactor StepVerifier; mock CatalogClient; happy-path test asserts response body. (Coverage gate: changed class must reach ≥ 90% line coverage — enforced later.)
- AC7: Controller implements the existing API operation declared in the generated OpenAPI module (do not modify the generated module or OpenAPI spec in this story).

## Edge Cases
- Author contains spaces, UTF-8 characters, punctuation (accepted as-is since framework decodes path variable).
- Very long author strings (>256) → 400 Bad Request.
- Empty or blank author path → 400 Bad Request.
- Catalog client returns multiple books for an exact match → pick the first result.
- Catalog client returns empty Mono → 404 Not Found.
- Author with leading/trailing whitespace: framework-provided path value is used as-is; if trimming is required, the unit tests / service should assert expected behaviour (story currently accepts as-is).

## Constraints
- Reactive stack only: Spring WebFlux + Reactor. No blocking operations (`.block()`, `.collectList()`, etc.).
- Constructor injection for new components.
- Do not edit generated OpenAPI code or the OpenAPI spec. The operation is declared already; implement the declared controller method (do not add controller-level mapping annotations that duplicate generated mappings).
- No timeouts, retries, circuit-breakers, or rate-limiting added in this story — infrastructure owns resilience.
- Unit tests: JUnit 5 + Reactor StepVerifier; mock CatalogClient; no external HTTP calls.
- Coding-phase file changes restricted to production code under `src/main/**` only; tests are added in the unit_testing phase.
- Coverage requirement (enforced later): changed class should reach ≥ 90% line coverage.

## Out of Scope
- Editing the OpenAPI spec or the generated `*-openapi-code` module.
- Database schema changes or direct DB access.
- Pagination or listing multiple books by author.
- Authentication/authorization changes.
- Performance or load testing.
- Adding timeouts/retries/circuit-breakers or rate-limiting logic.

<!-- No Clarifications Needed: story provides explicit decisions for match rule, multiple matches, path encoding, validation, and client method naming. -->

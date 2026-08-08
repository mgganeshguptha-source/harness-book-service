## What Are We Trying to Achieve
Add a reactive endpoint that returns a single book for a given author name. The endpoint should follow the repository's reactive patterns (Spring WebFlux / Reactor), delegate lookup to the catalog client, and avoid any blocking or collection-to-blocking operations.

## Current Behaviour
_Skip — new endpoint (no existing GET /books/by-author/{author})_

## Expected Behaviour
- Expose a reactive controller operation: GET /books/by-author/{author}
- Controller method returns Mono<ResponseEntity<BookResponse>> and implements the existing generated API interface method for getBookByAuthor.
- Controller delegates to a BookService; BookService delegates to the catalog client to perform the lookup.
- Catalog client exposes a reactive method fetchBookByAuthor(String author) returning Mono<CatalogBookDto> which calls the catalog endpoint /catalog/books/by-author/{author}.
- Matching rule: exact, case-insensitive match on author name. If the catalog returns multiple items, the first is used.
- Blank or invalid author input (empty/blank or length > 256) → 400 Bad Request.
- If catalog lookup yields no result → 404 Not Found.
- Public endpoint: no authentication/roles required for this sample.
- No blocking calls (no .block(), no .collectList()) anywhere in the controller/service/client implementation.

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and controller method returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller delegates to BookService; no blocking operations used in controller or service.
- AC3: BookService uses CatalogClient.fetchBookByAuthor(author) (no direct downstream HTTP calls from service/controller).
- AC4: Reactive composition (Mono) is used end-to-end and follows reactive-webclient/reactive-controller instructions in the repo.
- AC5: Validation: blank author → 400; author length > 256 → 400. No other character restrictions.
- AC6: Catalog empty result → 404 Not Found.
- AC7: Controller implements the existing generated BooksApi.getBookByAuthor interface method (do not add controller-level @GetMapping; do not edit generated openapi module/spec).
- AC8: Unit tests (produced in unit_testing phase) will use StepVerifier and mock the CatalogClient; target ≥ 90% line coverage on the changed class (enforced by harness gate — produced in unit_testing phase, not coding).

## Edge Cases
- Author name contains spaces and UTF-8 characters (framework provides URL-decoded value) — accepted as-is.
- Very long author names (>256) → 400 Bad Request per story rule.
- Author blank or whitespace-only → 400 Bad Request.
- Catalog returns multiple books for exact match → use the first returned result; do not return 409.
- Catalog client returns an error (5xx) — not specified in story: infrastructure handles resilience; controller returns appropriate 502/503/5xx mapping via global error handler — _Not found in codebase — confirm with team_.

## Constraints
> ⚠️ Missing copilot-instructions.md
>
> I don't see **.github/copilot-instructions.md** in this repo.
>
> What this means: the context.md will use generic Spring Boot and Angular defaults for the Constraints section. The team's actual coding standards are not being applied.
>
> Recommendation: create copilot-instructions.md before running the analysis prompt. Otherwise Copilot may produce code that doesn't match your conventions.
>
> Continue with defaults, or pause to set up copilot-instructions.md first?

Applied defaults (CI-mode):
- Backend: Constructor injection required. Standard RFC-7807 error format. Jakarta Validation on inputs. JUnit 5 + Mockito for tests. Follow reactive-controller and reactive-webclient repo instructions.
- Reactive stack: Spring WebFlux / Reactor. No .block(), no .collectList(). Use Mono/Flux composition.
- Service boundary: Catalog lookup must go through the catalog client (no direct HTTP calls in controller/service).
- Validation: author max length 256, blank → 400. Use server-side Jakarta validation or explicit checks in controller/service.
- Unit tests: StepVerifier for reactive testing; mock CatalogClient; no external calls.

## Out of Scope
- Editing the OpenAPI spec or the generated *-openapi-code module. The getBookByAuthor operation is already declared on the generated BooksApi interface.
- Database schema changes or direct DB access.
- Pagination or returning multiple books by author; this story returns a single book only.
- Authentication/authorization changes — endpoint is public for this sample.
- Adding timeouts / retries / circuit-breakers / rate-limiting — infrastructure owns resilience.
- Performance/load testing and resilience policy implementation.

## Clarifications Needed
_None — the story provides explicit behavior for matching, multiple matches, encoding, validation, and delegation to CatalogClient._

---

Service owner or integrator: please review the missing-in-repo copilot-instructions.md recommendation and confirm whether to proceed with repository defaults or add project-specific constraints before running analysis or implementation phases.
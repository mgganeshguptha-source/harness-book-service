## What Are We Trying to Achieve
Add a reactive endpoint that returns a single book for a given author name. The endpoint must follow the existing reactive controller pattern (WebFlux/Reactor), delegate the lookup to the catalog client, and avoid any blocking calls. This enables clients to retrieve a book by exact author match in a non-blocking, composable way.

## Current Behaviour
- The service exposes a reactive endpoint to fetch books by ID (referenced pattern exists). There is no endpoint to fetch a book by author.
- No controller method currently implements GET /books/by-author/{author}.

## Expected Behaviour
- New endpoint: GET /books/by-author/{author}
- Returns: Mono<ResponseEntity<BookResponse>>
- Behaviour details:
  - Exact, case-insensitive match on the author name (no partial/contains matching).
  - The controller delegates the lookup to the service layer which in turn uses the catalog client to fetch the book. No direct downstream HTTP calls from the controller.
  - If the catalog client yields no result, respond 404 Not Found.
  - If the {author} path variable is blank or longer than 256 characters, respond 400 Bad Request.
  - If the catalog client returns more than one match (unexpected), the implementation should use the first result returned; do not add conflict/aggregation logic.
  - The endpoint is public (no authentication/roles required for this sample).
  - Reactive only: return Mono, compose Reactor types, and avoid .block(), .collectList(), or other blocking operators.

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller delegates to the service layer; no blocking operations present in controller or service.
- AC3: Lookup performed exclusively via CatalogClient.fetchBookByAuthor(author) (no direct downstream HTTP callers added here).
- AC4: Reactive patterns followed (Mono composition, WebFlux controller model) and compliant with reactive-controller/reactive-webclient constraints.
- AC5: Empty catalog result → 404; blank author → 400; author > 256 chars → 400.
- AC6: Unit tests (produced in the unit_testing phase) use StepVerifier to assert the happy path and response body; changed class achieves ≥90% line coverage (coverage gate enforced later).
- AC7: Controller implements the existing BooksApi.getBookByAuthor interface method (do not add controller-level @GetMapping or edit generated OpenAPI modules/specs).

## Edge Cases
- Blank author path variable → 400 Bad Request.
- Author longer than 256 characters → 400 Bad Request.
- Author contains spaces or UTF-8 characters (framework provides URL-decoded value) — accept as-is.
- Catalog client returns empty → 404 Not Found.
- Catalog client unexpectedly returns multiple books → use first result (no 409).
- Very large or malicious input strings beyond length check are rejected by 400; other character sets accepted.
- Network/timeout/resilience behaviours are out of scope and handled by infrastructure.

## Constraints
- .github/copilot-instructions.md not found in repository — using default Spring Boot / WebFlux constraints.
  > ## ⚠️ Missing copilot-instructions.md
  >
  > I don't see **.github/copilot-instructions.md** in this repo.
  >
  > **What this means:** the context.md will use generic Spring Boot
  > and Angular defaults for the Constraints section. The team's actual
  > coding standards are not being applied.
  >
  > **Recommendation:** create copilot-instructions.md before running
  > the analysis prompt. Otherwise Copilot may produce code that doesn't
  > match your conventions.

- Backend (defaults applied): constructor injection for components; standard RFC-7807 error format on failures; Jakarta Validation used for input validation; JUnit 5 + Mockito for unit tests; reactive WebFlux + Reactor (no blocking operators).
- Reactive stack only: do not introduce blocking calls, .block(), or collectList() in controller/service implementation.
- Do not modify generated OpenAPI modules or specs; implement the existing BooksApi.getBookByAuthor method in the controller.
- No resilience code (timeouts, retries, circuit-breakers, rate-limiting) to be added in this story.
- Unit tests: JUnit 5 + Reactor StepVerifier; CatalogClient should be mocked; no external calls during tests.

## Out of Scope
- Editing the OpenAPI spec or the generated *-openapi-code module (the getBookByAuthor operation is already declared on BooksApi).
- Database schema changes or direct DB access — lookup must go through CatalogClient only.
- Pagination or listing multiple books by author.
- Authentication/authorization changes.
- Performance/load testing or resilience policies.

## Clarifications Needed
N/A (not required for implementation)

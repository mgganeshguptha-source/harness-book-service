## What Are We Trying to Achieve
Add a reactive endpoint to expose a book lookup by author name so callers can retrieve a single BookResponse for a given author. This follows the existing reactive getBookById pattern and must delegate to the catalog common-layer client (CatalogClient). Implementation must be reactive (WebFlux / Reactor) and must not block.

## Current Behaviour
No endpoint exists to find a book by author. Existing reactive endpoint: getBookById exists and is used as the pattern to follow.

## Expected Behaviour
- New endpoint: GET /books/by-author/{author}
- Returns: Mono<ResponseEntity<BookResponse>>
- Reactive controller must delegate to BookService (service layer) and must call the catalog common-layer client for the lookup. No blocking calls (.block(), .collectList()) allowed in controller or service.
- Behavior on results:
  - If a single matching book found → 200 OK with BookResponse
  - If no matching book → 404 Not Found (problem+json per service standards)
  - If multiple books match the author → _Not found in codebase — confirm with team_

## Acceptance Criteria
- AC1: Endpoint exists at GET /books/by-author/{author} and returns Mono<ResponseEntity<BookResponse>>
- AC2: Controller delegates to a BookService and does not perform blocking operations
- AC3: Lookup uses the catalog common-layer client (CatalogClient) for the author search — controller/service must not call downstream HTTP clients directly except via the common client
- AC4: Controller follows reactive patterns (returns Mono, composes Reactor types) and complies with reactive-webclient/reactive-controller instructions
- AC5: Unit test using StepVerifier covering the happy path (catalog client returns expected data) with at least one test asserting the response body; target: include unit test such that changed class has >=90% line coverage (story requests this threshold)
- AC6: OpenAPI spec updated to add this operation (the generated API interface will include the new method); do not hand-edit generated code outside of the OpenAPI spec

## Edge Cases
- Author path contains spaces, punctuation, UTF-8 characters (e.g., "Gabriel García Márquez")
- Author param is empty string or missing — should be validated (400)
- Multiple books with same author exist — behavior not specified in story
- Catalog client returns 500 or times out — propagate appropriate problem+json (503 or 500) per global error handling
- Catalog client returns empty or not found — translate to 404 for the endpoint
- Catalog client returns partial/error payloads — resilience handling required

## Constraints
- Reactive stack: Spring WebFlux / Reactor. No blocking calls (`.block()`, `.collectList()` on reactive streams) in controller or service code.
- Follow existing reactive-controller and reactive-webclient instructions in the repo.
- API contract change must be expressed in the OpenAPI spec so generated server interface includes the new method; do NOT hand-edit generated sources outside of spec changes.
- Use constructor injection for new components.
- Error responses must follow application/problem+json (RFC 7807) and include correlationId.
- Unit tests: JUnit 5 + Reactor StepVerifier. Mock the CatalogClient in unit tests; do not call external systems in unit tests.
- No DB access in this story — lookup is via CatalogClient (common-layer client).

## Out of Scope
- Database schema changes or direct DB access
- Adding a new catalog client implementation — assume an existing catalog common-layer client is available
- Pagination or listing multiple books by author (unless clarified)
- Authentication/authorization policy changes across the service (unless clarified)
- Performance/load testing — not part of unit test acceptance

## Clarifications Needed
- [NEEDS CLARIFICATION]: Match rule — should the author lookup be exact match, case-insensitive exact, or partial/contains match? (e.g., "Rowling" vs "J.K. Rowling")
- [NEEDS CLARIFICATION]: Multiple matches — if the catalog client returns multiple books for the author, should the endpoint return the first match, an aggregated response, 409 Conflict, or 200 with a list? The story expects a single BookResponse but code must accept the desired behaviour.
- [NEEDS CLARIFICATION]: Path encoding and allowed characters — confirm how spaces and special/UTF-8 characters in the {author} path param should be encoded and validated (e.g., require clients to URL-encode, or accept plus-sign/space rules).
- [NEEDS CLARIFICATION]: CatalogClient method & contract — which method on the existing catalog common-layer client should be used for an author lookup (method name, request shape)? _Not found in codebase — confirm with team_
- [NEEDS CLARIFICATION]: Authentication/Authorization — does this endpoint require any specific roles or is it public? Specify the expected auth scheme (JWT bearer + roles, or public).
- [NEEDS CLARIFICATION]: Expected HTTP status for "no match" — story implies 404 but confirm if 200 with empty body/array is preferred.
- [NEEDS CLARIFICATION]: Unit test coverage target enforcement — the story requests ">= 90% line coverage on the changed class"; confirm whether repository has an enforced threshold or CI job and whether 90% is strict for this change.
- [NEEDS CLARIFICATION]: Circuit breaker / timeout behaviour for the CatalogClient call — should the service apply timeouts and fallbacks, and if so what policy (timeout value, fallback response)?
- [NEEDS CLARIFICATION]: Rate limiting — is any rate-limiting required for this new endpoint?

## Constraints auto-filled (defaults applied)
- Constructor injection throughout
- Standard error response shape (application/problem+json)
- Jakarta Validation on controller inputs
- JUnit 5 + Mockito for unit tests; Reactor StepVerifier for reactive assertions
- Pagination is not required for this endpoint unless multiple-match behaviour is chosen

---

Context written in CI mode. Resolve any [NEEDS CLARIFICATION] items with the product owner or team before implementing or updating the OpenAPI spec.
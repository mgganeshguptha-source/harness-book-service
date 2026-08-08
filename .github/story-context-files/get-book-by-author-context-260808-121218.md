## What Are We Trying to Achieve
Add a reactive endpoint that returns a single book for a given author name. The endpoint must be non-blocking (Spring WebFlux / Reactor), implement the existing BooksApi.getBookByAuthor interface method, delegate to BookService which calls CatalogClient.fetchBookByAuthor(author), and return a Mono<ResponseEntity<BookResponse>>. Validation, error handling, and delegation rules follow the story decisions.

## Expected Behaviour
- Endpoint: GET /books/by-author/{author} implemented by the controller class that implements BooksApi.getBookByAuthor (no controller-level mapping annotations to be added in this story).
- Returns: Mono<ResponseEntity<BookResponse>>.
- Delegation flow: Controller -> BookService -> CatalogClient.fetchBookByAuthor(String author) -> Catalog service endpoint /catalog/books/by-author/{author}.
- Reactive only: no .block(), no .collectList(), and no blocking calls anywhere in the new code.
- Match rule: exact, case-insensitive match on the author name. If CatalogClient returns multiple results, take the first.
- Blank or >256 char author: respond 400 Bad Request.
- No match (CatalogClient returns empty): respond 404 Not Found.
- Public endpoint: no authentication/roles required for this sample.

## Acceptance Criteria
- AC1: GET /books/by-author/{author} exists and returns Mono<ResponseEntity<BookResponse>>.
- AC2: Controller delegates to BookService; no blocking operations introduced.
- AC3: BookService calls CatalogClient.fetchBookByAuthor(...) — no direct downstream HTTP calls from the controller/service.
- AC4: Reactive patterns followed (Mono composition) and code complies with the repo's reactive-controller and reactive-webclient instructions.
- AC5: Input validation: blank author → 400; author length >256 → 400; normal UTF-8 and spaces accepted as-is.
- AC6: Catalog empty → 404; multiple catalog matches → use first result.
- AC7: Controller implements the generated BooksApi.getBookByAuthor interface method (do not edit generated OpenAPI modules).

## Edge Cases
- Author path variable contains spaces, UTF-8 characters (accepted as-is — framework URL-decoded).
- Very long author (>256) → 400.
- Empty/blank author (zero-length or whitespace-only) → 400.
- CatalogClient returns multiple books for exact author → first returned item is used.
- CatalogClient returns an error (5xx) — not handled specially in this story; infrastructure/infra owners own resilience. The controller/service should propagate a suitable error response per existing global error handling.
- Author containing special characters (apostrophes, punctuation) — accepted; no additional sanitisation beyond length.

## Constraints
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
>
> **Continue with defaults, or pause to set up copilot-instructions.md first?**

Applied defaults (backend):
- Constructor injection for new components
- Standard error response format (application/problem+json via ControllerAdvice)
- Jakarta Validation on inputs (use @Valid / @NotBlank / @Size where applicable)
- JUnit 5 + Mockito for unit tests
- Reactive stack: Spring WebFlux / Reactor — no blocking calls
- API contract must not modify generated OpenAPI modules

Story-specific constraints:
- Add CatalogClient.fetchBookByAuthor(String) -> Mono<CatalogBookDto> in the existing CatalogClient abstraction and implement in CatalogClientImpl calling /catalog/books/by-author/{author}.
- Do not add timeouts, retries, circuit-breakers, or rate-limiting in this story.
- Controller must implement BooksApi.getBookByAuthor; do not add @GetMapping or other mapping annotations to the controller.
- No DB access; lookup exclusively via CatalogClient.

## Out of Scope
- Editing the OpenAPI spec or the generated *-openapi-code module
- Pagination or listing multiple books by author
- Authentication/authorization changes
- Resilience policy (timeouts, retries, circuit-breakers, rate limiting)
- Performance/load testing or infra concerns

## Clarifications Needed
_N/A — all required decisions were provided in the story._

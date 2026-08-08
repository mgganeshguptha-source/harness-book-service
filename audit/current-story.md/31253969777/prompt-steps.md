# Plan for: BOOK-1: Add "get book by author" endpoint

**Source:** newest context file in .github/story-context-files (story: "BOOK-1: Add \"get book by author\" endpoint")
**Stack:** Backend — Spring Boot, WebFlux (Reactive), Reactor
**Total steps:** 9
**Unresolved clarifications:** None

---

## Before you execute any step

1. Keep the context file (the BOOK-1 context in .github/story-context-files) in your Copilot Chat context while executing the plan.
2. Do not edit generated OpenAPI modules or the OpenAPI spec; the BooksApi interface already declares getBookByAuthor and is owned by the generated module.
3. This plan is for the *coding* phase. The coding phase writes ONLY production source under src/main/**. Unit tests are created in the unit_testing phase.

---

## Pre-flight

Assumptions the plan makes:

1. Backend uses Spring Boot + Spring WebFlux with Reactor and Spring WebClient for any downstream calls. The project follows constructor injection and package layout under `src/main/java`.
2. Behaviour preserved from Current Behaviour: existing getBookById reactive flow, error-mapping via ControllerAdvice, and logging/correlation patterns remain unchanged.
3. Non-functional constraints (timeouts/retries/circuit-breakers) are out of scope for this story and will not be implemented here; infrastructure provides them.

If any assumption is wrong, stop and update the context/spec before coding.

---

## Impacted Files (seeded — confirm during Step 1 inventory)

| ID | Path | Role |
|----|------|------|
| F1 | src/main/java/com/harness/book/service/controller/BooksController.java | Controller implementing BooksApi; implement getBookByAuthor(...) here (no @GetMapping) |
| F2 | src/main/java/com/harness/book/service/service/BookService.java | Service interface — add method fetchBookByAuthor(...) returning Mono<BookResponse> |
| F3 | src/main/java/com/harness/book/service/service/impl/BookServiceImpl.java | Service implementation — call CatalogClient and map DTO → BookResponse |
| F4 | src/main/java/com/harness/book/service/client/CatalogClient.java | Catalog client interface — add fetchBookByAuthor(String) : Mono<CatalogBookDto> |
| F5 | src/main/java/com/harness/book/service/client/impl/CatalogClientImpl.java | Catalog client implementation — call `/catalog/books/by-author/{author}` using reactive WebClient and return Mono<CatalogBookDto> |

> Note: These are the expected files to change in the coding phase. Step 1 will confirm the exact paths/names used in the repo and add any missing repository-specific files.

---

## Step 1 — Inventory / confirm impacted files

Goal: Confirm the exact controller, service, client class names and paths to edit. Add any genuinely required non-code files (none expected) and update the Impacted Files block.

Suggested prompt:

> Using the BOOK-1 context file in .github/story-context-files, list the concrete files that must be changed to add the getBookByAuthor endpoint in the coding phase. Start with these candidates and add or remove as needed:
> - src/main/java/com/harness/book/service/controller/BooksController.java
> - src/main/java/com/harness/book/service/service/BookService.java
> - src/main/java/com/harness/book/service/service/impl/BookServiceImpl.java
> - src/main/java/com/harness/book/service/client/CatalogClient.java
> - src/main/java/com/harness/book/service/client/impl/CatalogClientImpl.java
>
> For each returned file, provide a one-line role. Also list any other files (DTOs, mappers, exception classes, or package-private helpers) that must be touched. Do NOT change files — only list them.

Review checkpoint: Confirm the Impacted Files table above exactly matches the project's file names. If other files are required (e.g., an existing Catalog DTO package or a mapper class), add them as new IDs (F6, F7...). Do not proceed until the file list is accurate.

---

## Step 2 — Design decision: mapping & error flow

Goal: Decide how CatalogBookDto maps to BookResponse and how to propagate not-found and validation behaviours through Reactor.

Suggested prompt:

> Propose a short implementation plan for BookService.fetchBookByAuthor that: 1) calls CatalogClient.fetchBookByAuthor(author), 2) maps CatalogBookDto → BookResponse, 3) when the Mono is empty, returns Mono.error(new NotFoundException(...)) so controller advice maps it to 404. Show the suggested mapper logic and where to place validation (author blank / length) so that controller method returns 400 for invalid input. Do not write file changes yet.

Review checkpoint: Pick the mapping approach (manual mapping in service vs central mapper utility). Prefer manual mapping in the service if the codebase has only simple DTO conversions; otherwise add/choose an existing mapper component.

---

## Step 3 — CatalogClient: interface addition (F4)

Goal: Add the new method signature to the CatalogClient interface.

What to change (as text only; do NOT implement now):

- Add to F4 (CatalogClient.java):

```java
// new method in CatalogClient
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

Review checkpoint: Ensure the interface follows existing patterns (returns Reactor Mono, named fetchBookByAuthor) and the DTO CatalogBookDto type is available in the client/dto package.

---

## Step 4 — CatalogClientImpl: implementation outline (F5)

Goal: Describe the implementation of fetchBookByAuthor in CatalogClientImpl using reactive WebClient, mirroring existing fetchBook(bookId) implementation.

Intended change (text snippet):

```java
// in CatalogClientImpl
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}")
            .build(author))
        .retrieve()
        .onStatus(HttpStatus::is4xxClientError, resp -> /* map 404 -> Mono.empty() or propagate */)
        .bodyToMono(CatalogBookDto.class);
}
```

Notes: follow existing error-mapping convention in CatalogClientImpl (use the same onStatus handlers). Do NOT add retries/timeouts here.

Review checkpoint: Implementation must be fully reactive and not block. Ensure any 404 from catalog becomes Mono.empty() (or preserve semantics expected by BookService as decided in Step 2).

---

## Step 5 — Service interface & impl (F2, F3)

Goal: Add service method and implement reactive composition calling CatalogClient.

Planned change (text snippet):

```java
// in BookService.java (interface)
Mono<BookResponse> fetchBookByAuthor(String author);

// in BookServiceImpl.java (implementation)
@Override
public Mono<BookResponse> fetchBookByAuthor(String author) {
    // Validation handled at controller layer; service assumes non-blank, <=256
    return catalogClient.fetchBookByAuthor(author)
        .map(dto -> /* map fields to BookResponse */)
        // If the catalog returns empty, propagate empty so controller maps to 404
        ;
}
```

Mapping: show explicit field mapping (id, title, author, publishedDate, etc.) consistent with existing getBookById mapping.

Review checkpoint: Implementation composes Reactor types only; no blocking calls. If multiple results are possible, the CatalogClient is expected to return at most one; if multiple occur, BookService should take the first element (the catalog client implementation should ensure this behavior or the service can map using flux.next()). The chosen consistent approach should be documented in a comment.

---

## Step 6 — Controller method implementation (F1)

Goal: Implement the BooksApi.getBookByAuthor method in the controller so it delegates to BookService.fetchBookByAuthor and returns Mono<ResponseEntity<BookResponse>>. Do NOT add controller-level mappings; implement the interface method only.

Planned controller method (text snippet):

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    // Validate input
    if (author == null || author.isBlank()) {
        return Mono.just(ResponseEntity.badRequest().build());
    }
    if (author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.fetchBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book))
        .switchIfEmpty(Mono.defer(() -> Mono.just(ResponseEntity.notFound().build())));
}
```

Notes: Using switchIfEmpty to return 404. Alternatively throw NotFoundException and let ControllerAdvice map — either is acceptable; pick the pattern consistent with existing getBookById. Make the chosen approach consistent across controller methods.

Review checkpoint: Controller must NOT call .block() or .collectList(). Method must return Mono<ResponseEntity<BookResponse>> and must delegate to BookService; do not call CatalogClient directly from controller.

---

## Step 7 — Logging, correlationId, and error mapping

Goal: Ensure logs and correlation propagation follow project conventions.

What to check (textual instructions):

- Add an INFO or DEBUG log line when request is received only if it matches project pattern, using the existing logger acquisition and MDC for correlationId.
- Do NOT log request bodies or PHI. Do not include author value in logs if it's considered PHI by team policy — prefer logging correlationId only.
- Ensure any NotFound / Validation errors are surfaced via existing ControllerAdvice (use NotFoundException if that pattern is used).

Review checkpoint: No PHI in logs; correlationId present in logs per logging guidance; controller does not emit stack traces to clients.

---

## Step 8 — Coding-phase validation checklist (before committing changes)

Before creating a PR for the coding phase, verify:

- Only files in the Impacted Files block were changed in src/main/**.
- No tests were added or modified under src/test/** (unit tests are created in unit_testing phase).
- No changes to generated OpenAPI code or spec.
- All reactive code composes Mono/Flux only; no .block(), .toFuture().get(), or .collectList() used.
- Controller method signature exactly implements BooksApi.getBookByAuthor and returns Mono<ResponseEntity<BookResponse>>.
- CatalogClientImpl uses WebClient/reactive client; onStatus handling mirrors existing client patterns.
- Input validation for blank and >256 length returns 400 as described.

Review checkpoint: If any of these fail, fix before PR.

---

## Step 9 — Unit testing & coverage (unit_testing phase — description only)

Goal: Unit tests will be written in the next phase. Describe required tests so the unit_testing phase can implement them.

Tests to create (text-only specs):

- File: src/test/java/.../controller/BooksControllerTest.java
  - Happy path: mock BookService.fetchBookByAuthor to return Mono.just(BookResponse). Use StepVerifier to assert status 200 and body matches.
  - Not found: mock service to return Mono.empty(); assert 404.
  - Validation: call controller method with blank author and >256 author and assert 400 responses.

- File: src/test/java/.../service/BookServiceTest.java
  - Happy path: mock CatalogClient.fetchBookByAuthor to return Mono.just(CatalogBookDto), assert mapping to BookResponse.
  - Empty catalog: mock to return Mono.empty(), assert Mono.empty() or NotFoundException behavior as per chosen pattern.

Coverage goal: ensure changed controller class has >= 90% line coverage; tests should focus on controller branching and service mapping.

Review checkpoint: Tests use JUnit5 + Reactor StepVerifier; CatalogClient is mocked; no external calls.

---

## Done criteria (map back to Acceptance Criteria)

- AC1: Controller exposes getBookByAuthor via BooksApi and returns Mono<ResponseEntity<BookResponse>> — Confirmed in controller method snippet.
- AC2: Controller delegates to BookService; no blocking — Confirmed in Step 6 snippet and reactive checks in Step 8.
- AC3: Service uses CatalogClient.fetchBookByAuthor — Confirmed in Step 5 and Step 3/4 descriptions.
- AC4: Reactive patterns only — ensured by checklist in Step 8.
- AC5: Validation and empty-result behaviour (400 / 404) — handled in Step 6 and Step 5 design.
- AC6: Unit test specs supplied for unit_testing phase; coverage target documented.
- AC7: Controller implements BooksApi method; no OpenAPI edits — noted in pre-flight and Step 6.

---

## Convention drift check (final coding-phase step)

Before submitting the coding-phase PR, run a quick manual review against .github/copilot-instructions.md and the project's logging & exception handling conventions. Document any drift and resolve or flag to reviewers.

---

## Notes / Rationale

- The plan avoids any addition of timeouts/retries/circuit breakers per story constraints.
- Validation is intentionally minimal (blank + length) as requested; character set is unrestricted.
- If the project already centralises validation via annotations on the generated OpenAPI DTOs, prefer keeping validation there and adapting the controller/service accordingly — do not edit generated code; instead, validate in controller using simple checks.

---

"Plan written for CI-mode by build-prompt-steps skill."

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T11:03:17
- phase: coding
- approved impacted files: ['src/main/java/com/harness/book/service/client/CatalogClient.java', 'src/main/java/com/harness/book/service/client/impl/CatalogClientImpl.java', 'src/main/java/com/harness/book/service/controller/BooksController.java', 'src/main/java/com/harness/book/service/service/BookService.java', 'src/main/java/com/harness/book/service/service/impl/BookServiceImpl.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T11:03:17

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T11:08:06
- phase: coding
- approved impacted files: ['src/main/java/com/harness/book/service/client/CatalogClient.java', 'src/main/java/com/harness/book/service/client/impl/CatalogClientImpl.java', 'src/main/java/com/harness/book/service/controller/BooksController.java', 'src/main/java/com/harness/book/service/service/BookService.java', 'src/main/java/com/harness/book/service/service/impl/BookServiceImpl.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/controller/BookController.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/controller/BookController.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T11:08:06

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T11:13:32
- phase: coding
- approved impacted files: ['src/main/java/com/harness/book/service/client/CatalogClient.java', 'src/main/java/com/harness/book/service/client/impl/CatalogClientImpl.java', 'src/main/java/com/harness/book/service/controller/BooksController.java', 'src/main/java/com/harness/book/service/service/BookService.java', 'src/main/java/com/harness/book/service/service/impl/BookServiceImpl.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T11:13:32

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T11:15:59
- phase: coding
- approved impacted files: ['src/main/java/com/harness/book/service/client/CatalogClient.java', 'src/main/java/com/harness/book/service/client/impl/CatalogClientImpl.java', 'src/main/java/com/harness/book/service/controller/BooksController.java', 'src/main/java/com/harness/book/service/service/BookService.java', 'src/main/java/com/harness/book/service/service/impl/BookServiceImpl.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T11:15:59

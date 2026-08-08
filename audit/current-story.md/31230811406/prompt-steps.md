# Plan for: BOOK-1 add get book by author

**Source:** newest file in .github/story-context-files/ (CI mode)
**Stack:** Backend — Spring Boot (Spring WebFlux / Reactor)
**Total steps:** 9
**Unresolved clarifications:** None (story clarified all behaviours)

---

## Before you execute any step

1. Keep the newest file in .github/story-context-files/ in scope; in CI mode that file is authoritative.  
2. This plan is CI-mode only: do NOT modify generated OpenAPI modules or specs. Coding phase must only change production source under src/main/**. Unit tests are produced in the unit_testing phase.  
3. Follow repository conventions: constructor injection, reactive return types (Mono/Flux), no blocking (.block(), .collectList()), and existing BooksApi interface method implementation.  

---

## Pre-flight

The plan assumes:

1. Backend uses Spring WebFlux / Reactor and the project already exposes a generated `BooksApi` interface with `getBookByAuthor` declared.  
2. Behaviour-preservation: existing `getBookById` reactive pattern is the model to follow (no blocking).  
3. Non-functional: no resilience (timeouts/retries/circuit-breaker) or rate-limiting will be added in this story; infra handles those concerns.

If any assumption is wrong, stop and revise before coding.

---

## Impacted Files (seed list — Step 1 will confirm exact paths)

| ID | Path (seed) | Role |
|----|-------------|------|
| F1 | src/main/java/**/controller/*BooksController*.java | Controller implementing BooksApi (implement getBookByAuthor) |
| F2 | src/main/java/**/service/*BookService*.java | Service layer; add method to delegate to CatalogClient |
| F3 | src/main/java/**/client/*CatalogClient*.java | Catalog client interface; add fetchBookByAuthor(String) method |
| F4 | src/main/java/**/client/*CatalogClientImpl*.java | Catalog client implementation; implement HTTP call to /catalog/books/by-author/{author} |

> Note: Step 1 (inventory) must confirm the exact paths and class names. If different names exist (e.g. BooksControllerImpl, BooksApiController, CatalogClientGateway), Step 1 will record the canonical IDs and update later steps to reference IDs.

---

## Step 1 — Inventory: confirm the real file set and discover any missing non-code artifacts

Goal: Confirm the exact controller, service, and client classes to modify; detect any additional required files (e.g. DTOs, mappers) and record them as new IDs.

Suggested prompt / CI action (automated):

- List the concrete files that implement the BooksApi interface and the existing BookService and CatalogClient classes. Also check for Catalog DTO classes (CatalogBookDto) and BookResponse DTOs. If any of the seed paths above are incorrect, return the actual paths. Also confirm whether `CatalogClient` is an interface with an existing `fetchBook(String)` method to mirror.

Review checkpoint: Confirm the Impacted Files block is populated with exact paths and update IDs if any additional files are required (e.g., DTOs or mappers). Do NOT change code yet.

---

## Step 2 — Design: reactive flow and error handling

Goal: Decide exact reactive composition and validation placement.

Design decisions to validate (automated/manual):
1. Validation of path parameter `author` (blank or >256 → return 400) performed at controller entry using `@Valid`-style or explicit checks before delegation. For simplicity and clarity, controller should validate and return ResponseEntity.badRequest() for invalid inputs.
2. Controller method will return `Mono<ResponseEntity<BookResponse>>` and delegate to `BookService.findByAuthor(author)` which returns `Mono<BookResponse>` or `Mono.empty()` for not found. The controller maps empty -> 404.
3. `CatalogClient.fetchBookByAuthor(String)` returns `Mono<CatalogBookDto>` and BookService maps CatalogBookDto -> BookResponse.

Suggested prompt / CI action:

- Propose the exact method signatures for controller, service, and client (see Step 3). Confirm mapping responsibilities and that no blocking calls are used. If there is an existing mapper utility in the codebase, prefer reusing it; otherwise implement a small mapping method in BookService.

Review checkpoint: Pick this design or provide an explicit alternate with pros/cons. Confirm mapping location (BookService vs a dedicated mapper).

---

## Step 3 — Controller changes (coding phase — production code edits described here)

Goal: Implement `BooksApi.getBookByAuthor` method in the controller implementation class (do not add @GetMapping — implements existing interface method).

Planned edits (text only — do NOT modify files in this planning phase):

- Modify the controller class that implements `BooksApi` (F1) to add/implement the `getBookByAuthor(String author)` method. Use constructor injection for BookService.

Intended method signature:

```java
// in F1 (controller implementing BooksApi)
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    // Validation: blank or length>256 -> 400
    if (author == null || author.trim().isEmpty() || author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.findByAuthor(author)
        .map(bookResponse -> ResponseEntity.ok(bookResponse))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
}
```

Notes:
- No `.block()` or `.collectList()` allowed. Use Reactor mapping and switchIfEmpty.
- Keep controller thin; no business logic beyond validation and mapping of service response -> ResponseEntity.

Review checkpoint: Confirm the controller implements BooksApi's method signature exactly and uses reactive composition as shown. Confirm no framework-level mapping annotation is added at controller level (the interface-driven mapping is already present).

---

## Step 4 — Service layer: add reactive delegation

Goal: Add `findByAuthor(String author)` to BookService (F2) that delegates to CatalogClient and maps the DTO.

Planned method signature and body (text-only):

```java
// in F2 (BookService)
public Mono<BookResponse> findByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
        .map(this::toBookResponse);
}

private BookResponse toBookResponse(CatalogBookDto dto) {
    // map fields from CatalogBookDto -> BookResponse
}
```

Notes:
- The service must not block. It returns Mono<BookResponse> and lets the controller convert to ResponseEntity.
- If catalogClient returns Mono.empty(), service returns Mono.empty() as well.
- Use constructor injection for CatalogClient.

Review checkpoint: Confirm method signature and mapping location. Ensure the mapping handles nulls safely and does not attempt blocking calls.

---

## Step 5 — CatalogClient interface and implementation (F3, F4)

Goal: Add `fetchBookByAuthor(String author): Mono<CatalogBookDto>` to CatalogClient and implement in CatalogClientImpl to call `/catalog/books/by-author/{author}`.

Planned interface change (text-only):

```java
// in F3 (CatalogClient)
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

Planned implementation (text-only) in F4 (CatalogClientImpl):

```java
// pseudocode in F4
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient
        .get()
        .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}")
            .build(author))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatus::is4xxClientError, resp -> /* map 404 -> Mono.empty or propagate a NotFoundException mapped later */ )
        .bodyToMono(CatalogBookDto.class);
}
```

Notes and expectations:
- Use the existing WebClient wiring (F4 likely already has webClient configured). Reuse existing error handling patterns in the client. Do NOT implement timeouts/retries here.
- For a 404 from catalog, the client should return Mono.empty() (or the client method can map 404 to Mono.empty()); choose whichever pattern matches existing `fetchBook(String)` implementation — Step 1 must confirm the existing pattern and align behavior.

Review checkpoint: Confirm the client method signature and that the implementation uses non-blocking WebClient and returns Mono<CatalogBookDto>. Confirm how 404 is handled in existing client methods and match that behaviour.

---

## Step 6 — DTOs and mapping

Goal: Confirm DTO classes and field mappings exist; if not, add minimal mapping in BookService.

Planned action (text-only):
- If `CatalogBookDto` and `BookResponse` already exist, document the field mapping in the plan and reuse existing mappers. Example mapping snippet:

```java
// example mapping inside BookService
private BookResponse toBookResponse(CatalogBookDto dto) {
    return BookResponse.builder()
        .id(dto.getId())
        .title(dto.getTitle())
        .author(dto.getAuthor())
        .publishedDate(dto.getPublishedDate())
        // map other fields as required
        .build();
}
```

Review checkpoint: Confirm DTO names and fields. If DTOs differ in package/name, Step 1 must have added the correct file IDs.

---

## Step 7 — Unit testing plan (unit_testing phase)

Goal: Provide the tests to be implemented in the unit_testing phase (do NOT create tests now).

Test cases to add (in unit_testing phase):
1. Happy path: CatalogClient returns a CatalogBookDto -> BookService maps -> Controller returns 200 with BookResponse body. Use Mockito to mock CatalogClient and StepVerifier to subscribe to controller method Mono and assert status and body.
2. Not found: CatalogClient returns Mono.empty() -> Controller returns 404.
3. Bad request: blank author and author >256 -> Controller returns 400 (unit tests should call controller method directly).  

Test skeleton (for unit_testing phase only):

```java
// Example JUnit5 + StepVerifier assertion (to be created in src/test/...)
StepVerifier.create(controller.getBookByAuthor("J. K. Rowling"))
    .expectNextMatches(responseEntity -> responseEntity.getStatusCode().is2xxSuccessful()
        && responseEntity.getBody() != null
        && responseEntity.getBody().getAuthor().equals("J. K. Rowling"))
    .verifyComplete();
```

Coverage note: Ensure the changed controller class gets >90% line coverage as required by the harness; write tests accordingly in unit_testing phase.

Review checkpoint: Unit tests are added in unit_testing phase; confirm file locations and mocking approach then.

---

## Step 8 — Manual validation / acceptance criteria checks (second-to-last)

Goal: Manually validate each AC against a running instance (post-deploy or local run using existing run profile).

Validation checklist:
- AC1: Invoke GET /books/by-author/{author} and confirm return type and 200/404/400 behaviours.
- AC2: Inspect controller to confirm it delegates to BookService and contains no blocking calls.
- AC3: Inspect BookService to confirm delegation to CatalogClient.fetchBookByAuthor(...).
- AC4: Inspect code for Reactor usage and absence of blocking calls (.block(), .collectList()).
- AC5: Test empty catalog -> 404; blank/too-long author -> 400.
- AC6: Run unit tests (unit_testing phase) and verify coverage gate passes.

If any AC fails, loop back to the specific implementation step and correct.

---

## Step 9 — Convention drift check and PR guidance

Goal: Review changed files for adherence to repository conventions (.github/copilot-instructions.md) before opening PR.

Checklist:
- Use constructor injection and mark dependencies private final.
- Use SLF4J logging (if logging needed) with parameterised messages and no PHI.
- Ensure no new blocking calls or synchronous WebClient usage.
- Ensure CatalogClient interface and impl reuse existing WebClient configuration and error mapping.
- Add a TODO audit comment if any new code touches PHI (not expected here).

PR body guidance (include in PR description):
- Reference STORY: BOOK-1 and list ACs satisfied.  
- Describe the exact classes changed (use Impacted Files IDs after Step 1 confirmation).  
- Note that unit tests are added in the unit_testing phase and coverage gate must pass.

---

## Done criteria

Before opening a PR for the coding phase, confirm:
- All production source edits are limited to the confirmed Impacted Files (F1..FN) and nowhere else.  
- Controller implements `BooksApi.getBookByAuthor` exactly, returns `Mono<ResponseEntity<BookResponse>>`, and validates author (blank/length).  
- BookService delegates to `CatalogClient.fetchBookByAuthor(...)` and maps CatalogBookDto -> BookResponse.  
- CatalogClient interface includes `Mono<CatalogBookDto> fetchBookByAuthor(String author)` and its implementation uses WebClient non-blocking calls to `/catalog/books/by-author/{author}`.  
- No `.block()` or `.collectList()` introduced.  
- Step 1 confirmed the exact file paths and the Impacted Files block in this plan has been updated to the real paths.

---

End of plan.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T00:48:35
- phase: coding
- approved impacted files: ['src/main/java/**/client/*CatalogClient*.java', 'src/main/java/**/client/*CatalogClientImpl*.java', 'src/main/java/**/controller/*BooksController*.java', 'src/main/java/**/service/*BookService*.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T00:48:35

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T00:49:56
- phase: coding
- approved impacted files: ['src/main/java/**/client/*CatalogClient*.java', 'src/main/java/**/client/*CatalogClientImpl*.java', 'src/main/java/**/controller/*BooksController*.java', 'src/main/java/**/service/*BookService*.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T00:49:56

# Plan for: Add "get book by author" endpoint

**Source:** get-book-by-author-context-260808-150104.md + .github/copilot-instructions.md
**Stack:** Backend — Spring WebFlux (Reactive) with Reactor; sample generated API module present
**Total steps:** 9
**Unresolved clarifications:** None (context resolved all explicit decisions)

---

## Before you execute any step

1. Keep get-book-by-author-context-260808-150104.md in your Copilot Chat context throughout the plan.
2. .github/copilot-instructions.md is auto-loaded by Copilot when present. Verify it is present if you expect team conventions applied.
3. Execute steps in one Copilot Chat session when possible. If you restart, paste the full plan back into the session alongside the context file.
4. If a step asks Copilot to modify a file, confirm Copilot read the file's current contents (the response should cite real method names and imports) before accepting edits.
5. Do not add tests in this coding phase — unit tests are created during the unit_testing phase per pipeline rules.

---

## Pre-flight

The plan assumes:

1. Backend uses Spring WebFlux with Reactor and the project already returns Mono<ResponseEntity<...>> in controllers (see existing BookController#getBookById). This story preserves that reactive pattern.
2. Existing behaviour for getBookById is preserved. New method follows same conventions (constructor injection, logging, mapping DTOs to BookResponse).
3. Non-functional constraints (timeouts/retries/circuit-breakers) are out of scope and handled by infra; do not introduce them here.

If any assumption is wrong, stop and update the context file before proceeding.

---

## Impacted Files

| ID | Path | Role |
|----|------|------|
| F1 | sample-book-service-application/src/main/java/com/example/book/controller/BookController.java | Reactive controller implementing BooksApi; add implementation for getBookByAuthor |
| F2 | sample-book-service-application/src/main/java/com/example/book/service/BookService.java | Service interface — add signature getBookByAuthor |
| F3 | sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java | Service implementation — implement getBookByAuthor delegating to CatalogClient |
| F4 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java | Common-layer client interface — add fetchBookByAuthor signature |
| F5 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java | Common-layer client impl — add HTTP call to /catalog/books/by-author/{author} |

> Notes: The generated API interface declaring getBookByAuthor already exists in the generated module (sample-book-service-openapi-code/src/...) and must NOT be edited. The controller implements that interface and therefore must implement the method; do not add controller-level @GetMapping annotations.

---

## Step 1 — Inventory and confirm impacted files (seeded)

**Goal:** Confirm the concrete file set that this story will change (coding phase). Ensure non-code artifacts (none required here) are listed.

**Suggested prompt:**

> Planning from: get-book-by-author-context-260808-150104.md. Start with these candidate files: sample-book-service-application/src/main/java/com/example/book/controller/BookController.java, sample-book-service-application/src/main/java/com/example/book/service/BookService.java, sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java, sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java, sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java. Confirm these are the only production files that need modification for the coding phase. Add any genuinely required files (for example DTOs or configuration beans) and remove any not impacted. Do NOT touch generated modules or OpenAPI specs.

**Review checkpoint:** Confirm the Impacted Files table above matches the inventory output. If other production files are required (e.g., a new DTO or WebClient bean qualifier), add them to the Impacted Files block before proceeding.

---

## Step 2 — Design validation & error mapping approach

**Goal:** Decide how the controller will return 400 for blank or >256 author and 404 for no match, while conforming to project conventions.

**Suggested prompt:**

> The controller must return 400 when author is blank or >256 characters, and 404 when the CatalogClient returns empty. Propose two implementation options consistent with the project's error handling: (A) explicit reactive response construction in the controller (validate and return Mono.just(ResponseEntity.badRequest().build()) / .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))), or (B) throw a ValidationException/NotFoundException that the global @ControllerAdvice maps to RFC7807 responses. For each option list pros/cons. Recommend an option that aligns with the existing codebase (see BookController#getBookById and existing project conventions). Do not write code yet.

**Review checkpoint:** Choose the option to implement. If the repo already has a global exception type for validation, prefer (B); otherwise implement (A) to avoid introducing new exception classes during this small story.

---

## Step 3 — Update service interface (F2)

**Goal:** Add a reactive service API for author lookup.

**Suggested edits (text snippet to apply):**

Before (excerpt from F2):

```java
public interface BookService {
    Mono<BookResponse> getBook(String bookId);
}
```

After:

```java
public interface BookService {
    Mono<BookResponse> getBook(String bookId);

    // New: find a single book by exact, case-insensitive author name
    Mono<BookResponse> getBookByAuthor(String author);
}
```

**Review checkpoint:** Confirm the new signature compiles (no other code calls missing). No implementation yet.

---

## Step 4 — Add CatalogClient API (F4)

**Goal:** Extend the common-layer client interface with the new reactive fetch method.

**Suggested edits (text snippet to apply):**

Before (excerpt from F4):

```java
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId);
}
```

After:

```java
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId);

    // New: fetch by exact, case-insensitive author name
    Mono<CatalogBookDto> fetchBookByAuthor(String author);
}
```

**Review checkpoint:** Ensure the interface addition is minimal and descriptive. No impl yet.

---

## Step 5 — Implement CatalogClient HTTP call (F5)

**Goal:** Add a reactive WebClient call in CatalogClientImpl that calls /catalog/books/by-author/{author} and returns Mono<CatalogBookDto>.

**Suggested edits (text snippet to apply):**

Before (excerpt from F5):

```java
@Override
public Mono<CatalogBookDto> fetchBook(String bookId) {
    log.info("Calling catalog common-layer for bookId:{}", bookId);
    return webClient.get()
            .uri("/catalog/books/{id}", bookId)
            .retrieve()
            .bodyToMono(CatalogBookDto.class);
}
```

After (add new method):

```java
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    log.info("Calling catalog common-layer for author:{}", author);
    return webClient.get()
            .uri("/catalog/books/by-author/{author}", author)
            .retrieve()
            .bodyToMono(CatalogBookDto.class);
}
```

**Review checkpoint:** Confirm no blocking calls, method uses existing webClient bean (catalogWebClient qualifier), and logging matches project style. Ensure path variable name matches downstream endpoint.

---

## Step 6 — Implement service method (F3)

**Goal:** Implement BookServiceImpl#getBookByAuthor delegating to CatalogClient.fetchBookByAuthor and mapping CatalogBookDto → BookResponse.

**Suggested edits (text snippet to apply):**

Before (excerpt from F3):

```java
@Override
public Mono<BookResponse> getBook(String bookId) {
    return catalogClient.fetchBook(bookId)
            .map(dto -> new BookResponse(dto.getBookId(), dto.getTitle(), dto.getAuthor()));
}
```

After (add new method):

```java
@Override
public Mono<BookResponse> getBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
            .map(dto -> new BookResponse(dto.getBookId(), dto.getTitle(), dto.getAuthor()));
}
```

**Review checkpoint:** Confirm reactive composition only (no .block()/collectList()). If CatalogClient returns empty, the Mono completes empty and controller will map to 404 in the next step.

---

## Step 7 — Implement controller method (F1)

**Goal:** Implement BooksApi#getBookByAuthor in BookController. Validate `author` path variable (blank or >256 → 400). Delegate to bookService.getBookByAuthor(author); on result present return 200+body, on empty return 404.

**Suggested edits (text snippet to apply):**

Add method to BookController:

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    log.info("Request received getBookByAuthor author:{}", author);

    // Validation: blank or too long
    if (author == null || author.isBlank() || author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.getBookByAuthor(author)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

**Implementation notes:**
- Do not add @GetMapping or other request mapping annotations — the controller implements the generated BooksApi which already declares mappings.
- Keep code reactive; use defaultIfEmpty to map empty Mono → 404.

**Review checkpoint:** Confirm method compiles and that no blocking or new dependencies introduced. Confirm logging consistent with existing controller (use same logger).

---

## Step 8 — Unit test plan (unit_testing phase)

**Goal:** Describe tests to be implemented in the unit_testing phase to satisfy AC6.

**Suggested tests (to be created in unit_testing phase under src/test):**

1. Happy path: mock CatalogClient.fetchBookByAuthor(author) → Mono.just(CatalogBookDto); call BooksApi.getBookByAuthor(author) using BookController (or WebTestClient) and verify StepVerifier expects ResponseEntity.ok with BookResponse body fields matching mapped values.
2. Not found: mock CatalogClient.fetchBookByAuthor(author) → Mono.empty(); verify controller returns ResponseEntity.notFound().
3. Validation: blank author and author length >256 produce ResponseEntity.badRequest().

**Test details:**
- Use JUnit 5 + Reactor StepVerifier.
- Mock CatalogClient with Mockito (or a project-preferred mocking library).
- Do not perform any external HTTP calls.

**Review checkpoint (unit_testing phase):** Tests added and passing locally; changed class has ≥90% line coverage.

---

## Step 9 — Done criteria & manual validation

Before opening a PR, confirm:

- AC1: BooksApi.getBookByAuthor is implemented and returns Mono<ResponseEntity<BookResponse>> (verify method signature in F1).
- AC2: Controller delegates to BookService; no blocking operations in controller or service.
- AC3: BookService delegates to CatalogClient.fetchBookByAuthor (no direct HTTP calls from service or controller).
- AC4: Reactive patterns followed across controller → service → client (Mono composition only).
- AC5: Blank or >256 → 400; Catalog empty → 404 (manual checks and unit tests in next phase).
- AC6: Unit tests (in unit_testing phase) cover happy path and edge cases; coverage gate ≥90% for changed class.
- AC7: No edits to generated openapi code or OpenAPI spec; controller implements existing BooksApi method — confirm by checking the generated interface file.

Manual validation steps (to run locally / CI after unit_testing):

- Start the app (or run controller slice tests). For a sample happy path, mock the CatalogClient to return a known CatalogBookDto and call GET /books/by-author/{author} using WebTestClient or StepVerifier against the controller method.
- Verify response codes and bodies for happy, not-found, and validation cases.

---

## Implementation notes & caveats

- Keep changes minimal and focused to the five files listed. Do not modify the generated module or OpenAPI spec.
- Follow existing project logging style (BookController uses log.info with placeholders).
- Do not add retries, timeouts, or circuit-breakers — infra owns resilience.
- If the project later centralises validation into a ControllerAdvice, refactor the validation checks to throw ValidationException and let the advice map it to RFC7807 responses.

---

Plan written to .harness/prompt-steps.md. Execute the steps in order; coding changes occur in the coding phase only and tests in the unit_testing phase.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T15:07:56
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-08-08T15:07:56

# Plan for: BOOK-1: Add "get book by author" endpoint

**Source:** newest context file in .github/story-context-files (BOOK-1: Add "get book by author" endpoint)
**Stack:** Backend — Spring WebFlux (Reactor), Java Spring Boot
**Total steps:** 10
**Unresolved clarifications:** None (all explicit decisions provided in the context)

---

## Before you execute any step

1. Keep the newest context file from .github/story-context-files in your Copilot Chat context while executing steps. In CI mode the harness already provided the context used to produce this plan.
2. .github/copilot-instructions.md (if present) is auto-loaded by Copilot; confirm it exists if your team conventions are expected to be applied.
3. Execute steps in order. Step 1 (inventory) confirms the exact file set — do not edit files until Step 2+ explicitly asks to.
4. This plan is for the coding phase only. Do NOT create or edit any test files in src/test/** in this phase — unit tests are produced in the unit_testing phase.

---

## Pre-flight

The plan assumes:

1. Backend uses Spring WebFlux with Reactor and the project follows constructor injection and the layered package layout under sample-book-service-application (controller, service, webclient).
2. Existing behaviour for getBookById is preserved; the new endpoint implements exactly the BooksApi.getBookByAuthor contract declared in the generated openapi module. No controller-level @RequestMapping additions are required (controller implements BooksApi).
3. Non-functional constraints (timeouts/retries/circuit-breakers) are out of scope for this story and are not added here.

If any assumption is wrong, stop and revise the context file before proceeding.

---

## Impacted Files (seed list — Step 1 will confirm exact files)

| ID | Path | Role |
|----|------|------|
| F1 | sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java | Generated API interface (already declares getBookByAuthor) — DO NOT EDIT |
| F2 | sample-book-service-application/src/main/java/com/example/book/controller/BookController.java | Reactive controller implementing BooksApi — add method implementation for getBookByAuthor |
| F3 | sample-book-service-application/src/main/java/com/example/book/service/BookService.java | Service interface — add new method signature getBookByAuthor(String) |
| F4 | sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java | Service implementation — implement getBookByAuthor delegating to CatalogClient |
| F5 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java | Common-layer client interface — add fetchBookByAuthor(String) |
| F6 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java | Common-layer client impl — implement fetchBookByAuthor calling /catalog/books/by-author/{author} |

> Notes: The generated openapi module (F1) already declares the operation. Do NOT hand-edit any file under the -openapi-code module.

---

## Step 1 — Inventory: confirm the exact file set

**Goal:** Verify the candidate files above are the correct files to change. Add any genuinely required files (config, DTOs) to the Impacted Files block and remove any that are not needed.

**Suggested prompt:**

> Planning from: newest context file in .github/story-context-files (BOOK-1: Add "get book by author" endpoint). Start with these candidate files: 
> - sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java
> - sample-book-service-application/src/main/java/com/example/book/controller/BookController.java
> - sample-book-service-application/src/main/java/com/example/book/service/BookService.java
> - sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java
> - sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java
> - sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java
>
> Confirm which of these files must be edited as part of the coding phase to implement GET /books/by-author/{author}. Add any missing required files (for example DTOs or config) and remove non-impacted files. Return a one-line role per confirmed file and produce the final Impacted Files table.

**Review checkpoint:** Confirm the Impacted Files table above exactly matches the codebase. If a file path differs, update the table to the exact path. Do not proceed to edits until the table is correct.

---

## Step 2 — Design the method signatures (small design; propose exact signatures)

**Goal:** Decide and record the exact Java method signatures to add to interfaces and classes.

**Suggested prompt:**

> Using the verified Impacted Files table (from Step 1) and the project conventions in .github/copilot-instructions.md, propose the exact method signatures to add or implement. Use reactive types and constructor injection. Do not write method bodies yet. The signatures must follow these rules from the story: blank author -> 400; author max length 256; service method returns Mono<BookResponse>; CatalogClient method returns Mono<CatalogBookDto>. Provide the method signatures and brief rationale.

**Expected outputs (to record in the plan):**

- In F3 (BookService.java) add:

```java
Mono<BookResponse> getBookByAuthor(String author);
```

- In F5 (CatalogClient.java) add:

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

- In F2 (BookController.java) implement the interface method already declared in F1 (BooksApi):

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author)
```

- In F4 (BookServiceImpl.java) implement:

```java
@Override
public Mono<BookResponse> getBookByAuthor(String author)
```

- In F6 (CatalogClientImpl.java) implement:

```java
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author)
```

**Review checkpoint:** Confirm these signatures match team conventions and existing DTO names (BookResponse, CatalogBookDto). If DTO names differ, update signatures accordingly.

---

## Step 3 — Implement CatalogClient interface change (F5)

**Goal:** Add the fetchBookByAuthor signature to the CatalogClient interface.

**Suggested prompt:**

> Edit F5 (CatalogClient.java). Add the new method signature `Mono<CatalogBookDto> fetchBookByAuthor(String author);`. Do not change existing methods. Return the updated file contents.

**Review checkpoint:** Confirm only the single method signature was added to the interface and imports are correct.

---

## Step 4 — Implement CatalogClientImpl (F6)

**Goal:** Implement the new method in CatalogClientImpl to call the common-layer endpoint `/catalog/books/by-author/{author}` reactively.

**Suggested prompt:**

> Edit F6 (CatalogClientImpl.java). Add the method implementation:
>
> ```java
> @Override
> public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
>     log.info("Calling catalog common-layer for author:{}", author);
>     return webClient.get()
>             .uri("/catalog/books/by-author/{author}", author)
>             .retrieve()
>             .bodyToMono(CatalogBookDto.class);
> }
> ```
>
> Keep the implementation fully reactive and consistent with the existing fetchBook method. Do not add retry/timeout logic.

**Review checkpoint:** Confirm the method uses webClient, returns Mono<CatalogBookDto>, and mirrors the style of fetchBook (no blocking). Ensure imports are present.

---

## Step 5 — Update BookService interface (F3)

**Goal:** Add the getBookByAuthor declaration to the service interface.

**Suggested prompt:**

> Edit F3 (BookService.java). Add `Mono<BookResponse> getBookByAuthor(String author);` to the interface, keeping existing methods unchanged.

**Review checkpoint:** Confirm only the new method signature was added to the interface.

---

## Step 6 — Implement BookServiceImpl.getBookByAuthor (F4)

**Goal:** Implement the service logic to call CatalogClient.fetchBookByAuthor and map the CatalogBookDto to BookResponse reactively.

**Suggested prompt:**

> Edit F4 (BookServiceImpl.java). Implement:
>
> ```java
> @Override
> public Mono<BookResponse> getBookByAuthor(String author) {
>     return catalogClient.fetchBookByAuthor(author)
>             .map(dto -> new BookResponse(dto.getBookId(), dto.getTitle(), dto.getAuthor()));
> }
> ```
>
> Keep it fully reactive, do not block, and follow existing mapping conventions used in getBook(String bookId).

**Review checkpoint:** Confirm no blocking calls were introduced and mapping is consistent with existing code.

---

## Step 7 — Implement BookController.getBookByAuthor (F2)

**Goal:** Implement the controller method declared by BooksApi to validate input and delegate to BookService.

**Suggested prompt:**

> Edit F2 (BookController.java). Add/modify the override for getBookByAuthor(String author) with these behaviours:
>
> - If author == null or author.trim().isEmpty() → return Mono.just(ResponseEntity.badRequest().build())
> - If author.length() > 256 → return Mono.just(ResponseEntity.badRequest().build())
> - Otherwise delegate to bookService.getBookByAuthor(author)
>     - On value: return ResponseEntity.ok(body)
>     - If empty: return ResponseEntity.notFound().build()
>
> Example implementation:
>
> ```java
> @Override
> public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
>     log.info("Request received getBookByAuthor author:{}", author);
>     if (author == null || author.trim().isEmpty() || author.length() > 256) {
>         return Mono.just(ResponseEntity.badRequest().build());
>     }
>     return bookService.getBookByAuthor(author)
>             .map(ResponseEntity::ok)
>             .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
> }
> ```
>
> Do not add @GetMapping — the generated BooksApi interface already declares the mapping.

**Review checkpoint:** Confirm the controller performs validation synchronously (no blocking), delegates to service, and returns the correct 400/404 semantics.

---

## Step 8 — Small code review & convention check

**Goal:** Scan the changed files for style, constructor injection, logging placeholders, and avoid accidental blocking.

**Suggested prompt:**

> Review the diffs for F2, F3, F4, F5, F6 against .github/copilot-instructions.md and Java conventions. List any convention drift (constructor injection, logging usage, import ordering). Do not apply fixes automatically — list issues for the developer to confirm.

**Review checkpoint:** Confirm: constructor injection used, private final fields preserved, no System.out, no .block(), and logging uses placeholders.

---

## Step 9 — Unit test plan (unit_testing phase)

**Goal:** Describe the unit tests to be implemented in the next phase (unit_testing) to meet AC6 (StepVerifier, mock CatalogClient, coverage ≥ 90%).

**Suggested prompt for unit_testing phase (do NOT run now):**

> For BookController (or BookService) write JUnit 5 tests using Reactor StepVerifier and Mockito to cover:
> - Happy path: CatalogClient returns CatalogBookDto → StepVerifier asserts ResponseEntity.ok and body fields
> - Not found: CatalogClient returns Mono.empty() → expect 404
> - Bad request: blank author and author > 256 → expect 400
>
> Mock CatalogClient.fetchBookByAuthor(...) and verify no external HTTP calls. Target the controller class with the BooksApi method implemented. Ensure line coverage target is met by focusing tests on controller branches and service mapping.

**Review checkpoint:** Confirm tests will mock CatalogClient and use StepVerifier to assert response Mono values. (Implementation of tests occurs in unit_testing phase.)

---

## Step 10 — Validation (manual) and Done criteria

**Goal:** Manual verification against Acceptance Criteria after coding and unit_testing phases are complete.

**Manual validation checklist:**

- AC1: Call GET /api/v1/books/by-author/{author} (spring base path if present) and confirm it returns Mono<ResponseEntity<BookResponse>> shape (200 + body) for happy path.
- AC2: Confirm controller delegates to BookService (inspect diffs) and no .block() present.
- AC3: Confirm BookService delegates to CatalogClient.fetchBookByAuthor(...) (inspect diffs). No direct WebClient calls in service layer.
- AC4: Confirm all code uses Reactor types; no blocking or collectList usage.
- AC5: Test behaviours:
  - blank author → 400
  - author length > 256 → 400
  - catalog returns empty → 404
- AC6: Run unit tests (unit_testing phase) that use StepVerifier to assert happy path and branch coverage. Achieve ≥90% line coverage on changed class as required by pipeline gate.
- AC7: Confirm controller implements BooksApi.getBookByAuthor (no controller-level @RequestMapping changes; generated module not edited).

**Convention drift check (final):** Run the repo's code-style/linting locally and confirm no drift from .github/copilot-instructions.md. Fix any flagged issues before opening a PR.

---

## Done criteria (before opening a PR)

- All file edits limited to the Impacted Files above under src/main/java/** (no src/test changes in coding phase).
- The controller implements the existing BooksApi.getBookByAuthor method.
- The BookService interface and implementation expose getBookByAuthor and delegate to CatalogClient.
- CatalogClient exposes fetchBookByAuthor and CatalogClientImpl calls `/catalog/books/by-author/{author}` reactively.
- Input validation for blank and >256 chars present and returns 400; empty downstream → 404.
- No blocking operations introduced; code compiles locally; unit tests executed in the unit_testing phase cover the behaviours via StepVerifier.

---

Co-ordinate with the unit_testing phase to add the tests described in Step 9. If any of the file paths above differ in your checkout, update the Impacted Files table (Step 1) before making edits.



## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T10:19:45
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-08-08T10:19:45

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T10:25:10
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-08-08T10:25:10

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T10:26:32
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-08-08T10:26:32

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T10:29:47
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-08-08T10:29:47

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T10:32:16
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-08-08T10:32:16

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T10:33:23
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-08-08T10:33:23

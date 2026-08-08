# Plan for: BOOK-1 - Add "get book by author" endpoint

**Source:** newest context file in .github/story-context-files (Story: BOOK-1: Add "get book by author" endpoint)
**Stack:** Backend — Spring WebFlux / Reactor (reactive-controller, reactive-webclient patterns)
**Total steps:** 10
**Unresolved clarifications:** None (all clarifications resolved in the provided context)

---

## Before you execute any step

1. This plan was generated in CI mode from the newest context file in .github/story-context-files. Keep that file available when executing steps locally.
2. Do not modify the generated OpenAPI module or any OpenAPI spec. The controller must implement the existing BooksApi.getBookByAuthor method (no controller-level @GetMapping edits).
3. Coding phase must only change production source under src/main/**. Tests are added in the unit_testing phase. This plan describes code changes as text + before/after snippets; do NOT apply edits in this planning phase.

---

## Pre-flight

Assumptions the plan makes:

1. Backend uses Spring Boot with WebFlux and Reactor. WebClient is the project-standard reactive HTTP client and there is an existing CatalogClient and CatalogClientImpl pattern to mirror.
2. Existing behaviour preserved: the getBookById reactive pattern and BooksApi interface are canonical; this story will follow the same layered delegation (Controller → BookService → CatalogClient).
3. Non-functional constraints (timeouts/retries/rate-limiting) are out of scope and handled by infra — this change does not add resilience wrappers.

If any assumption is wrong, stop and update the context file before proceeding.

---

## Impacted Files (seed list — Step 1 will confirm and add any missing files)

| ID | Path | Role |
|----|------|------|
| F1 | src/main/java/com/harness/book/service/controller/BooksController.java | Controller implementing BooksApi (reactive) |
| F2 | src/main/java/com/harness/book/service/service/BookService.java | Service layer delegating to CatalogClient |
| F3 | src/main/java/com/harness/book/service/client/CatalogClient.java | Catalog client interface (add fetchBookByAuthor) |
| F4 | src/main/java/com/harness/book/service/client/CatalogClientImpl.java | Catalog client implementation (WebClient) |
| F5 | src/main/java/com/harness/book/service/model/CatalogBookDto.java | DTO returned by CatalogClient |
| F6 | src/main/java/com/harness/book/service/model/BookResponse.java | API response DTO (existing — mapping may be reused) |

> Later steps reference files by ID (F1..F6). Step 1 (inventory) should confirm these paths and add any missing files (e.g., mapper/util classes) but do NOT rename IDs once assigned.

---

## Step 1 — Inventory and confirm impacted files

**Goal:** Confirm the exact file locations and whether helper classes (mappers, exceptions) already exist and must be used or created.

**Suggested prompt (CI / local):**

> Planning from the newest context file in .github/story-context-files (BOOK-1: Add "get book by author" endpoint). Start with these candidate files: F1-F6 (listed above). Read each file's current contents and report whether it exists and whether it already contains relevant helper methods (e.g., mapping methods, validation utilities, existing CatalogClient methods). Also list any non-code artifacts required (e.g., config properties for catalog base URL). Do not edit any file — only list and confirm. If additional files are required (mapper, dto, exception), add them to the impacted files list with new IDs.

**Review checkpoint:** Confirm F1..F6 paths exist or note which are absent. Add any newly required files (e.g., F7: src/main/java/.../mapper/BookMapper.java). Record final Impacted Files table.

---

## Step 2 — Design reactive flow and validation

**Goal:** Decide exact reactive composition and validation approach for blank/length checks and empty-result → 404 handling.

**Suggested prompt:**

> Given the project uses WebFlux and Reactor, propose a concise reactive implementation for the flow: BooksApi.getBookByAuthor -> BooksController (implements interface) -> BookService.getBookByAuthor(String author) -> CatalogClient.fetchBookByAuthor(String author). Include validation (blank or >256 → return Mono<ResponseEntity.badRequest()> with RFC-7807 problem body?) and empty result mapping (Mono.empty() → Mono.error(new NotFoundException(...)) or Mono<ResponseEntity.notFound()). Propose exact approach consistent with existing project patterns (e.g., if controllers return Mono<ResponseEntity<...>>, show how to return 400/404 without blocking). Provide pros/cons and pick the recommended option.

**Review checkpoint:** Confirm chosen approach matches existing project patterns (use same ProblemDetails/error mapping mechanism) and that it avoids .block() / .collectList().

---

## Step 3 — API surface: CatalogClient interface change (text-only)

**Goal:** Add method signature to CatalogClient interface.

**Intended code change (text):**

Target: F3 (src/main/java/com/harness/book/service/client/CatalogClient.java)

Add this method signature to the interface:

```java
// new in CatalogClient
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

**Review checkpoint:** Ensure the interface addition mirrors existing fetchBook(String bookId) method style and reactive return type.

---

## Step 4 — CatalogClientImpl: implement reactive call to catalog

**Goal:** Implement client call using WebClient to GET /catalog/books/by-author/{author} and return Mono<CatalogBookDto>.

**Intended code change (text):**

Target: F4 (src/main/java/com/harness/book/service/client/CatalogClientImpl.java)

Add implementation analogous to existing fetchBook(bookId) method. Example snippet:

```java
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient
        .get()
        .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}").build(author))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatus::is4xxClientError, resp -> Mono.empty()) // adapt to project error handling
        .bodyToMono(CatalogBookDto.class);
}
```

Notes: follow existing error mapping (do not swallow exceptions if project maps them centrally). Do NOT add any blocking, do not collect lists.

**Review checkpoint:** Implementation uses existing WebClient bean, timeouts/resilience left to infra, and preserves reactive composition.

---

## Step 5 — Service layer: BookService.getBookByAuthor

**Goal:** Add a service method to validate the author input and delegate to CatalogClient.fetchBookByAuthor.

**Intended code change (text):**

Target: F2 (src/main/java/com/harness/book/service/service/BookService.java)

Add method signature and implementation (reactive):

```java
public Mono<BookResponse> getBookByAuthor(String author) {
    if (author == null || author.trim().isEmpty() || author.length() > 256) {
        return Mono.error(new ValidationException("author must be non-blank and ≤256 chars"));
    }
    return catalogClient.fetchBookByAuthor(author)
        .map(this::mapCatalogDtoToResponse);
        // if catalogClient returns empty Mono, ensure controller maps to 404 (or service can map to Mono.error(NotFoundException))
}
```

If project convention maps empty Mono to 404 at controller layer, implement service to return Mono.empty() when no book found; otherwise convert to Mono.error(new NotFoundException(...)). Choose the option consistent with existing patterns (see Step 2 decision).

**Review checkpoint:** Validation enforced without blocking; mapping function exists or will be added (see F5/F6). No direct WebClient calls in service.

---

## Step 6 — Controller: implement BooksApi.getBookByAuthor

**Goal:** Implement the interface method in the controller to call service, map empty → 404, and return Mono<ResponseEntity<BookResponse>>.

**Intended code change (text):**

Target: F1 (src/main/java/com/harness/book/service/controller/BooksController.java)

Implement the existing interface method (do NOT add @GetMapping here). Example snippet:

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    return bookService.getBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
        .onErrorResume(ValidationException.class, ex -> Mono.just(ResponseEntity.badRequest().build()));
}
```

Notes: Prefer to return problem+json bodies per project's error handling via ControllerAdvice; if centralised exception mapping is used, prefer throwing ValidationException/NotFoundException and let advice convert them to proper ProblemDetails responses. Follow the repository's existing pattern for controller error handling.

**Review checkpoint:** Controller implements BooksApi.getBookByAuthor, returns Mono<ResponseEntity<BookResponse>>, delegates to BookService, and contains no blocking calls.

---

## Step 7 — DTO mapping and models

**Goal:** Ensure CatalogBookDto (F5) and BookResponse (F6) are present and mapping exists.

**Intended code change (text):**

Target: F5, F6

If CatalogBookDto missing, add with fields returned by catalog service. Add or reuse BookMapper.map(CatalogBookDto -> BookResponse) in either BookService or a dedicated mapper.

Example mapping snippet:

```java
private BookResponse mapCatalogDtoToResponse(CatalogBookDto dto) {
    return BookResponse.builder()
        .id(dto.getId())
        .title(dto.getTitle())
        .author(dto.getAuthor())
        .publishedDate(dto.getPublishedDate())
        .build();
}
```

**Review checkpoint:** Mapping covers required fields; no blocking; DTOs use camelCase and ISO date strings.

---

## Step 8 — Unit testing guidance (unit_testing phase)

**Goal:** Provide the unit test plan (do not create tests in coding phase).

**Test plan (to implement in unit_testing phase):**

- Class under test: BooksController (or BookService) — prefer controller-level unit test that mocks BookService or CatalogClient as appropriate.
- Use JUnit5 + Reactor StepVerifier.
- Happy path: mock CatalogClient.fetchBookByAuthor(author) to return Mono.just(CatalogBookDto(...)); assert controller returns ResponseEntity.ok with expected BookResponse body.
- Not found: mock to return Mono.empty(); assert controller returns 404.
- Validation: blank author and >256 char author produce 400.

Example StepVerifier snippet (controller through service mocking):

```java
StepVerifier.create(controller.getBookByAuthor("Jane Doe"))
    .assertNext(response -> {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Jane Doe", response.getBody().getAuthor());
    })
    .verifyComplete();
```

Coverage note: aim ≥90% line coverage for the changed class per harness gate.

**Review checkpoint:** Tests mock CatalogClient; no external calls; use Reactor test utilities.

---

## Step 9 — Manual validation against acceptance criteria

**Goal:** Provide a checklist for the developer to validate the running app after coding and tests are in place.

**Validation checklist:**

- AC1: Invoke GET /books/by-author/{author} via REST client; confirm response type Mono<ResponseEntity<BookResponse>> (i.e., HTTP response OK + JSON body) for an existing author.
- AC2: Inspect controller/service code to confirm delegation and absence of blocking calls.
- AC3: Confirm service uses catalogClient.fetchBookByAuthor (no direct WebClient calls in controller/service beyond CatalogClientImpl).
- AC5: Verify blank author → 400; author >256 → 400; author not found → 404.
- AC6: Run unit tests and verify StepVerifier tests pass; check coverage report for ≥90% lines on changed class.

If any AC fails, revert to the related step (implementation or tests) and fix there.

---

## Step 10 — Convention drift review

**Goal:** After code changes, review diffs for adherence to repository conventions (.github/copilot-instructions.md). List items to check (constructor injection, SLF4J usage, no blocking, reactive patterns, exception hierarchy usage, ProblemDetails error format).

**Suggested checklist:**

- Constructor injection used for new beans.
- No System.out.* or printStackTrace usage.
- No .block(), .collectList(), or other blocking patterns in controller/service.
- Exceptions used are part of application exception hierarchy (ValidationException / NotFoundException) and mapping handled by @ControllerAdvice.
- WebClient usage follows existing project patterns and shares the configured bean.

**Review checkpoint:** List any drift items and decide whether to fix before opening PR.

---

## Done criteria

Before creating a PR for the coding phase, confirm:

- All code changes are limited to src/main/java/** and match the file/method signatures described in this plan.
- Controller implements BooksApi.getBookByAuthor and returns Mono<ResponseEntity<BookResponse>>.
- No blocking APIs used anywhere in the change set.
- CatalogClient interface and implementation include fetchBookByAuthor returning Mono<CatalogBookDto> and call /catalog/books/by-author/{author} reactively.
- Validation (blank / >256) enforced and results in 400; not-found yields 404.
- Impacted Files block is accurate and any extra required files were added to it in Step 1.

---

## Notes for implementer

- THIS IS A PLANNING PHASE ONLY: do NOT create/edit any .java or test files in this step. The coding phase will implement the exact changes described above. Placeholders and code snippets in this plan are illustrative — adapt them to the project's packages, common utilities, and existing exception types.
- If ambiguity remains about where CatalogClient/WebClient beans are configured, resolve during Step 1 inventory.

---

Plan generated for CI-mode consumption.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T12:30:31
- phase: coding
- approved impacted files: ['src/main/java/com/harness/book/service/client/CatalogClient.java', 'src/main/java/com/harness/book/service/client/CatalogClientImpl.java', 'src/main/java/com/harness/book/service/controller/BooksController.java', 'src/main/java/com/harness/book/service/model/BookResponse.java', 'src/main/java/com/harness/book/service/model/CatalogBookDto.java', 'src/main/java/com/harness/book/service/service/BookService.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T12:30:31

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T12:33:08
- phase: coding
- approved impacted files: ['src/main/java/com/harness/book/service/client/CatalogClient.java', 'src/main/java/com/harness/book/service/client/CatalogClientImpl.java', 'src/main/java/com/harness/book/service/controller/BooksController.java', 'src/main/java/com/harness/book/service/model/BookResponse.java', 'src/main/java/com/harness/book/service/model/CatalogBookDto.java', 'src/main/java/com/harness/book/service/service/BookService.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T12:33:08

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T12:39:36
- phase: coding
- approved impacted files: ['src/main/java/com/harness/book/service/client/CatalogClient.java', 'src/main/java/com/harness/book/service/client/CatalogClientImpl.java', 'src/main/java/com/harness/book/service/controller/BooksController.java', 'src/main/java/com/harness/book/service/model/BookResponse.java', 'src/main/java/com/harness/book/service/model/CatalogBookDto.java', 'src/main/java/com/harness/book/service/service/BookService.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T12:39:36

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T12:43:45
- phase: coding
- approved impacted files: ['src/main/java/com/harness/book/service/client/CatalogClient.java', 'src/main/java/com/harness/book/service/client/CatalogClientImpl.java', 'src/main/java/com/harness/book/service/controller/BooksController.java', 'src/main/java/com/harness/book/service/model/BookResponse.java', 'src/main/java/com/harness/book/service/model/CatalogBookDto.java', 'src/main/java/com/harness/book/service/service/BookService.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T12:43:45

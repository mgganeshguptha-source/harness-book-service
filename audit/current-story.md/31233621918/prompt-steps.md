# Plan for: BOOK-1: Add "get book by author" endpoint

**Source:** newest file in .github/story-context-files/ (story: "BOOK-1: Add \"get book by author\" endpoint")
**Stack:** Backend — Spring Boot WebFlux (Reactive) using Reactor, WebClient, and the existing generated BooksApi interface
**Total steps:** 10
**Unresolved clarifications:** None (all explicit behaviour decisions provided in the context file)

---

## Before you execute any step

1. Keep the context file from .github/story-context-files/ in CI context. The plan was generated from the newest context file on disk.
2. Do not edit any OpenAPI-generated module or the OpenAPI spec. Implement only production source under src/main/java in this coding phase.
3. This is the coding-phase plan only. Do NOT create or modify test files in this phase. Unit tests (StepVerifier-based) are created in the unit_testing phase.
4. Follow repository conventions: constructor injection, private final fields, SLF4J logging, reactive composition (no .block(), no .collectList()).

---

## Pre-flight

Assumptions the plan makes:

1. Stack assumption: Backend uses Spring Boot with WebFlux, Reactor, and a WebClient-based CatalogClient implementation pattern is already present in the codebase. The generated BooksApi interface (declaring getBookByAuthor) exists and the controller class implements it.
2. Behaviour preservation: Existing getBookById semantics and routes are preserved. No change to other Books endpoints. New endpoint implements exact, case-insensitive author match, returns 400 for blank or >256 chars, 404 for no match.
3. Non-functional handling: No resilience or timeout changes in this story (the infra team manages timeouts/retries). Performance targets are out of scope; avoid blocking calls to prevent thread starvation.

---

## Impacted Files

| ID | Path | Role |
|----|------|------|
| F1 | src/main/java/com/harness/bookservice/controller/BooksController.java | Controller implementing BooksApi.getBookByAuthor (returns Mono<ResponseEntity<BookResponse>>) |
| F2 | src/main/java/com/harness/bookservice/service/BookService.java | Service interface: declares reactive business method for lookup |
| F3 | src/main/java/com/harness/bookservice/service/impl/BookServiceImpl.java | Service implementation delegating to CatalogClient and mapping DTOs |
| F4 | src/main/java/com/harness/bookservice/client/CatalogClient.java | Client interface to the catalog service — add fetchBookByAuthor(String): Mono<CatalogBookDto> |
| F5 | src/main/java/com/harness/bookservice/client/impl/CatalogClientImpl.java | CatalogClient implementation using WebClient — implement fetchBookByAuthor call |
| F6 | src/main/java/com/harness/bookservice/model/BookResponse.java | API response DTO (mapping from CatalogBookDto) — may already exist; if not, create it here in coding phase |
| F7 | src/main/java/com/harness/bookservice/dto/CatalogBookDto.java | DTO returned by CatalogClient (likely exists). If missing, create a minimal DTO matching catalog shape.

> Note: If any of the seeded file paths do not exist in the repo, create the ones that are missing under the same logical packages. The controller must implement the generated BooksApi interface method (do not add new request-mapping annotations on the controller). If packages differ, adapt to existing package layout — keep names but preserve package conventions.

---

## Step 1 — Inventory and confirm impacted files (seeded list)

**Goal:** Confirm the exact file paths and any additional non-code artifacts required (none expected) and populate the Impacted Files block definitively.

**Suggested prompt (for local/manual execution / human review):**

> Read the codebase under src/main/java and confirm which of the following files exist: F1..F7 (seeded above). If any seeded file is missing, add it as a new impacted file and note its intended role. Also check for any required non-code artifacts (DB migration, config, OpenAPI edits). Do not modify code in this step — only list files and their current status (exists / missing). Return the finalized Impacted Files table with exact paths.

**Review checkpoint:** Confirm the Impacted Files table lists the real paths used in the repo. If package names/path differ, update later steps to reference the actual IDs/paths — do not proceed until the file mapping is accurate.

---

## Step 2 — Design the reactive flow and error handling

**Goal:** Agree the reactive composition and where validation occurs (controller vs service), and confirm mapping responsibilities (CatalogBookDto -> BookResponse).

**Suggested prompt:**

> Propose the implementation plan for getBookByAuthor, covering: (a) input validation (blank / length >256) done in controller (return 400), (b) service returns Mono<BookResponse> (empty when no match), (c) controller composes result into Mono<ResponseEntity<BookResponse>> — map to 200 when present, 404 when empty. For mapping, recommend a simple mapping function in BookServiceImpl or a Mapper util. Show short pros/cons of putting validation in controller vs service. Do not write code yet.

**Review checkpoint:** Confirm validation stays in controller, service is pure reactive delegation + mapping, and controller implements BooksApi.getBookByAuthor (no new @GetMapping).

---

## Step 3 — Add CatalogClient API method (F4)

**Goal:** Add method signature to CatalogClient interface.

**Suggested code snippet (before/after):**

Before (excerpt):

```java
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId);
    // ... other methods
}
```

After (add):

```java
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId);

    // New: fetch a single book by exact, case-insensitive author
    Mono<CatalogBookDto> fetchBookByAuthor(String author);
}
```

**Review checkpoint:** Confirm only the interface is changed in this step and method signature matches the story: Mono<CatalogBookDto> fetchBookByAuthor(String author).

---

## Step 4 — Implement CatalogClientImpl.fetchBookByAuthor (F5)

**Goal:** Implement the WebClient call to catalog endpoint /catalog/books/by-author/{author} returning Mono<CatalogBookDto>. Use non-blocking WebClient, reusing existing WebClient instance and common error handling.

**Suggested code snippet:**

```java
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient
        .get()
        .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}")
            .build(author))
        .retrieve()
        .onStatus(HttpStatus::is4xxClientError, resp -> Mono.empty()) // allow empty to propagate
        .bodyToMono(CatalogBookDto.class);
}
```

Notes:
- Reuse existing WebClient configured in this class.
- Do not add retries/timeouts here (story forbids). Rely on existing shared configuration if present.

**Review checkpoint:** Confirm method returns Mono<CatalogBookDto> and doesn't block. If the codebase has a different error mapping pattern (e.g., map 404->Mono.empty()), follow that pattern.

---

## Step 5 — Add BookService interface method (F2)

**Goal:** Declare the reactive service API used by the controller.

**Suggested code snippet:**

```java
public interface BookService {
    /**
     * Find a single book by exact, case-insensitive author. Returns empty when not found.
     */
    Mono<BookResponse> getBookByAuthor(String author);
}
```

**Review checkpoint:** Confirm method name and reactive return type align with controller expectations.

---

## Step 6 — Implement BookServiceImpl (F3)

**Goal:** Implement getBookByAuthor to delegate to CatalogClient.fetchBookByAuthor(author), map CatalogBookDto -> BookResponse, and return Mono.empty() for no match. Keep implementation non-blocking and small.

**Suggested code snippet:**

```java
@Override
public Mono<BookResponse> getBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
        .map(this::mapToBookResponse);
}

private BookResponse mapToBookResponse(CatalogBookDto dto) {
    return BookResponse.builder()
        .id(dto.getId())
        .title(dto.getTitle())
        .author(dto.getAuthor())
        // map other fields as needed
        .build();
}
```

Notes:
- Keep mapping logic simple. If a Mapper class exists, use it.
- Do not perform input validation here (controller does it).

**Review checkpoint:** Confirm BookServiceImpl delegates to CatalogClient (F5) and returns a Mono<BookResponse> without blocking.

---

## Step 7 — Implement controller method (F1) implementing BooksApi.getBookByAuthor

**Goal:** Implement the generated interface method, perform input validation (blank / >256 chars), delegate to BookService, and compose the ResponseEntity result (200 with body or 404). Return Mono<ResponseEntity<BookResponse>>.

**Suggested code snippet (method body only):**

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    if (author == null || author.isBlank() || author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.getBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
}
```

Notes:
- Do not add @GetMapping — the method implements the generated BooksApi interface.
- Use constructor injection for BookService.
- Return 400 immediately for invalid input.

**Review checkpoint:** Confirm controller implements BooksApi.getBookByAuthor, performs validation as specified, and returns the expected Reactive ResponseEntity types without blocking.

---

## Step 8 — Small wiring and model / DTO changes (F6, F7)

**Goal:** Ensure BookResponse and CatalogBookDto have the fields used in mapping. If BookResponse already exists, update its builder or constructor; otherwise create a minimal BookResponse DTO used by API.

**Suggested minimal BookResponse example:**

```java
public class BookResponse {
    private String id;
    private String title;
    private String author;
    // getters, setters, builder or Lombok
}
```

**Review checkpoint:** Confirm the mapping compiles and no new public API shapes are introduced other than BookResponse.

---

## Step 9 — Convention drift check

**Goal:** Review changed files for drift against repository conventions (.github/copilot-instructions.md) and the Java instruction files (constructor injection, logging, package layout). Do not auto-fix; list any drift for developer action.

**Suggested prompt/checklist:**

> For F1..F5 (changed files), verify: constructor injection used, private final fields, SLF4J logging, no blocking calls, DTOs use camelCase, dates/money rules followed. List any deviations.

**Review checkpoint:** Address any convention drift before opening PR.

---

## Step 10 — Manual validation & Done criteria

**Goal:** Verify the implementation satisfies the Acceptance Criteria and the story constraints.

**Validation checklist (manual) mapping to ACs:**

- AC1: Call the generated controller method (e.g., via curl or REST client) for GET /books/by-author/{author}; expect 200 + JSON body for a known author. Confirm method signature is Mono<ResponseEntity<BookResponse>>.
- AC2: Inspect code to confirm controller delegates to BookService and no .block() is present.
- AC3: Inspect BookServiceImpl to confirm it delegates to CatalogClient.fetchBookByAuthor and that no direct HTTP calls are made from the service or controller.
- AC4: Confirm reactive use: Mono return types and Reactor composition operators used (map, switchIfEmpty), no collectList(), no blocking.
- AC5: Test invalid inputs: blank author → 400, author >256 chars → 400; unknown author → 404.
- AC6 (unit_testing phase): Write unit tests using StepVerifier mocking CatalogClient to return a CatalogBookDto Mono.just(...) for happy path and Mono.empty() for not found. Assert controller returns expected ResponseEntity and body. Ensure test coverage gate (>=90% on changed class) is met in unit_testing phase.
- AC7: Confirm controller implements BooksApi.getBookByAuthor and that no changes were made to generated modules or the OpenAPI spec.

**What to do if a validation fails:**
- If validation shows blocking calls, revert and rework to Reactor composition.
- If CatalogClient returns multiple results (contrary to assumption), adapt BookServiceImpl to take the first element (but story says catalog returns at most one; if multiple appear, take first and log a warning). Do not change API signatures.

---

## Done criteria (before opening PR for coding phase)

- All modified production Java files (F1..F5 and any created DTOs) compile locally and follow reactive patterns.
- Controller method implements BooksApi.getBookByAuthor, performs validation, delegates to BookService, returns Mono<ResponseEntity<BookResponse>>.
- BookService delegates to CatalogClient.fetchBookByAuthor (no direct HTTP calls from service or controller).
- No .block(), no .collectList() introduced.
- Impacted Files table is accurate and recorded in the PR description.
- Unit tests are NOT included in this coding-phase PR (they will be added in the unit_testing phase).

---

Coaching note for the unit_testing phase (for the next phase):

- Unit tests to create in next phase:
  - Controller happy-path: mock BookService to return Mono.just(BookResponse) and StepVerifier the controller response. Assert ResponseEntity status and body.
  - Controller not-found: mock BookService to return Mono.empty() and assert 404.
  - Controller invalid input: call method with blank and >256 strings and assert 400.
  - Service unit tests: mock CatalogClient to return Mono.just(CatalogBookDto) and verify mapping to BookResponse.

---

Plan generated in CI mode from the newest context file. Execute the steps in order; do not modify OpenAPI-generated modules. Good luck.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T01:55:15
- phase: coding
- approved impacted files: ['src/main/java/com/harness/bookservice/client/CatalogClient.java', 'src/main/java/com/harness/bookservice/client/impl/CatalogClientImpl.java', 'src/main/java/com/harness/bookservice/controller/BooksController.java', 'src/main/java/com/harness/bookservice/dto/CatalogBookDto.java', 'src/main/java/com/harness/bookservice/model/BookResponse.java', 'src/main/java/com/harness/bookservice/service/BookService.java', 'src/main/java/com/harness/bookservice/service/impl/BookServiceImpl.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T01:55:15

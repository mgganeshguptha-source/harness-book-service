# Plan for: BOOK-1 - Add "get book by author" endpoint

**Source:** add-get-book-by-author-context-260808-001358.md + .github/copilot-instructions.md
**Stack:** Backend — Spring WebFlux (reactive Spring Boot), Reactor, WebClient
**Total steps:** 11
**Unresolved clarifications:** None (all explicit in context.md)

---

## Before you execute any step

1. Keep add-get-book-by-author-context-260808-001358.md in your Copilot Chat context throughout the plan.
2. .github/copilot-instructions.md is auto-loaded by Copilot when present in the repo. You don't need to attach it manually.
3. Execute steps in one Copilot Chat session when possible. If you restart, paste the full plan back into the chat alongside the context file.
4. This planning phase is non-invasive: DO NOT create, edit, or write any .java or test files now. Implementation happens later. The only file produced in CI is this plan at .harness/prompt-steps.md.

---

## Pre-flight

Assumptions the plan makes:

1. Backend uses Spring WebFlux + Reactor and existing patterns in the repo (BookController, BookService, CatalogClient) should be followed.
2. Behaviour preservation: existing getBookById endpoint and reactive behaviour remain unchanged. New endpoint mirrors patterns used by getBookById (returns Mono<ResponseEntity<BookResponse>> and delegates to BookService).
3. Non-functional handling: performance/resilience (timeouts/retries/circuit breakers) are out of scope for this story and must not be added. Unit tests will use StepVerifier and mocks only.

If any assumption is wrong, stop and revise the context file before proceeding.

---

## Impacted Files (seed list — Step 1 will confirm)

| ID | Path | Role |
|----|------|------|
| F1 | sample-book-service-application/src/main/java/com/example/book/controller/BookController.java | Reactive controller (contains getBookById) |
| F2 | sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java | API interface for generated controller signatures |
| F3 | sample-book-service-application/src/main/java/com/example/book/service/BookService.java | Service interface |
| F4 | sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java | Service implementation (delegates to CatalogClient) |
| F5 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java | Catalog client interface |
| F6 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java | Catalog client implementation (WebClient) |
| F7 | sample-book-service-openapi-code/src/main/resources/openapi.yaml (or equivalent) | OpenAPI specification file to add operation |
| F8 | sample-book-service-application/src/test/java/com/example/book/BookServiceImplTest.java | Unit tests for BookService (extend coverage) |
| F9 | sample-book-service-openapi-code/src/main/java/com/example/book/model/BookResponse.java | DTO for API responses (if present) |

> Note: Step 1 (inventory) will confirm the above list and add any legitimately required non-code files (OpenAPI source, schema, or generation config). Do not assume file paths beyond this seed until Step 1 confirms.

---

## Step 1 — Inventory (seeded file list)

**Goal:** Confirm the exact files the story will touch and add any missing required files (OpenAPI source, DTOs, new CatalogClient method, tests). Ensure no other files are implicitly required.

**Suggested prompt:**

> Planning from: add-get-book-by-author-context-260808-001358.md
>
> Start with these candidate files: F1..F9 (as listed in the Impacted Files block). Read each file and confirm whether it must be changed for BOOK-1. If other files are required (e.g., OpenAPI source location, generated API interface path, DTOs, test classes), add them to the impacted-files list and assign them new IDs (do not renumber existing IDs). For each confirmed file, return a one-line role description and whether it will be modified or added (modify/add). Also list any non-code artifacts needed (OpenAPI yaml change, generation step). Do not make edits yet.

**Review checkpoint:** Confirm the final Impacted Files table matches the project layout. If a different OpenAPI path is used, update the Impacted Files table accordingly.

---

## Step 2 — Design decision: API & delegations

**Goal:** Decide exact method signatures and error mapping patterns following existing getBookById pattern.

**Suggested prompt:**

> Using the existing getBookById patterns in F1 and F2, propose the exact controller method signature, the BookService method signature, and the CatalogClient method signature for the new "get by author" flow. Ensure signatures are reactive (Mono), use ResponseEntity where controller returns a response, and do not introduce blocking calls. Show the controller path as GET /api/v1/books/by-author/{author} (or match existing base path/mapping style in F1/F2). Also propose the OpenAPI operation entry to add in F7. Do not write code to files yet — return only the signatures and a short rationale.

**Review checkpoint:** Choose the proposed signatures that match existing conventions (constructor injection, DTO names, package structure). If the project uses generated API interfaces (F2), prefer updating the OpenAPI spec (F7) rather than hand-editing generated sources.

---

## Step 3 — Spec change (OpenAPI)

**Goal:** Add an operation for GET /books/by-author/{author} to the OpenAPI source so generated API code includes the endpoint.

**Suggested prompt:**

> Edit the OpenAPI source (F7). Add a new path /books/by-author/{author} with:
> - GET
> - path param "author" (string, required, maxLength=256)
> - responses: 200 (application/json -> BookResponse), 400 (problem+json), 404 (problem+json)
> - operationId: getBookByAuthor
>
> Provide the exact YAML snippet to add. Do not edit generated java sources directly — update the spec so generation produces the API interface and model types.

**Review checkpoint:** Confirm the OpenAPI snippet matches existing style (operationId casing, tag grouping) and that the project build regenerates sources from F7 (document how to regenerate if applicable).

---

## Step 4 — Add CatalogClient API surface (interface)

**Goal:** Add the new client method signature to the CatalogClient interface (F5) and implement it in CatalogClientImpl (F6). The method is reactive and mirrors existing fetchBook(...) method.

**Suggested prompt:**

> Edit F5 and F6: add a method:
>
> ```java
> // in CatalogClient.java
> Mono<CatalogBookDto> fetchBookByAuthor(String author);
> ```
>
> In CatalogClientImpl, implement fetchBookByAuthor(...) to call the catalog endpoint `/catalog/books/by-author/{author}` using the existing WebClient bean (catalogWebClient). Return a Mono<CatalogBookDto>. Follow existing error mapping style used in F6 (do not block). Provide the before/after snippets only.

**Review checkpoint:** Confirm the new method mirrors existing network call patterns (same deserialization, same error handling), and that no blocking operators are introduced.

---

## Step 5 — Service interface and impl change

**Goal:** Add new service method to BookService (F3) and implement in BookServiceImpl (F4) to delegate to CatalogClient.fetchBookByAuthor(...). Enforce validation (blank / >256 → 400) and map empty Mono to a NotFound result.

**Suggested prompt:**

> Edit F3 and F4. Add to BookService interface:
>
> ```java
> Mono<BookResponse> getBookByAuthor(String author);
> ```
>
> Implement in BookServiceImpl to:
> - validate author (blank → throw ValidationException, length>256 → ValidationException)
> - call catalogClient.fetchBookByAuthor(author)
> - map CatalogBookDto -> BookResponse using existing mapper method (reuse existing mapping utilities if present)
> - if catalog returns empty, return Mono.empty() up to the controller (or alternatively map to a NotFoundException depending on existing service-to-controller conventions). Follow the pattern used by existing getBookById in F4.
>
> Provide before/after snippets.

**Review checkpoint:** Confirm no blocking, that validation uses the application's ValidationException type, and mapping reuses existing converters.

---

## Step 6 — Controller endpoint

**Goal:** Add the controller method that exposes GET /books/by-author/{author} and returns Mono<ResponseEntity<BookResponse>> delegating to BookService.getBookByAuthor(...).

**Suggested prompt:**

> Edit F1 (BookController). Add a method with signature similar to existing getBookById but for author:
>
> ```java
> @GetMapping("/books/by-author/{author}")
> public Mono<ResponseEntity<BookResponse>> getBookByAuthor(@PathVariable String author) {
>     return bookService.getBookByAuthor(author)
>         .map(book -> ResponseEntity.ok(book))
>         .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
> }
> ```
>
> Ensure controller-level logging and request/response patterns match existing methods. Do NOT use .block() or .collectList().

**Review checkpoint:** Confirm endpoint returns Mono<ResponseEntity<BookResponse>>, delegates to BookService, and uses Reactor composition only.

---

## Step 7 — Validation and error mapping

**Goal:** Ensure blank/overlength author produces 400. Verify where ValidationException maps to 400 in the project and follow same pattern.

**Suggested prompt:**

> Identify the project's centralised exception handling (ControllerAdvice). Confirm the exception type to throw for validation failures (e.g., ValidationException). Update the service code (F4) to throw that exception for blank or >256 length. Do not add new exception mapping classes; reuse existing ones.

**Review checkpoint:** Confirm thrown exception type maps to 400 and the error payload shape matches RFC7807 problem+json used in the project.

---

## Step 8 — Unit tests (StepVerifier)

**Goal:** Add unit tests to cover the happy path and validation paths, targeting ≥ 90% line coverage for the changed class (BookServiceImpl or BookController depending on test target).

**Suggested prompt:**

> Edit F8 (BookServiceImplTest). Add tests with StepVerifier that:
> 1. Happy path: mock CatalogClient.fetchBookByAuthor(author) to return a CatalogBookDto; assert BookService.getBookByAuthor(author) emits a BookResponse with expected fields and completes.
> 2. Not found: mock CatalogClient to return Mono.empty(); assert service returns Mono.empty() or controller returns 404 depending on where mapping is done.
> 3. Validation: empty author and >256 author produce the expected ValidationException (use StepVerifier.expectError matching the exception class and message).
>
> Tests must be pure unit tests: mock CatalogClient, do not call real HTTP or WebClient.

**Review checkpoint:** Confirm tests use StepVerifier, mock CatalogClient, and that coverage tools report ≥ 90% for the modified class. If controller tests are added instead of service, ensure they also mock BookService.

---

## Step 9 — OpenAPI generation / generated code guidance

**Goal:** Ensure generated API code aligns with hand-coded controller changes and that generated sources are not hand-edited.

**Suggested prompt:**

> After updating F7 (OpenAPI), run the project's codegen task (document the exact gradle/maven command used in repo) to regenerate API interfaces (F2) and model classes (F9). Confirm the generated BooksApi contains `Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author);`. If the repo uses manual tweaks around generated code, document them here. Do not hand-edit generated sources — update the spec and regenerate.

**Review checkpoint:** Confirm the generated API interface matches the chosen signatures. If generation requires additional config, note the command to run.

---

## Step 10 — Convention drift check & coverage gate

**Goal:** Verify changes conform to project conventions (constructor injection, SLF4J logging, no blocking) and that tests satisfy coverage gate.

**Suggested prompt:**

> Review all modified files against .github/copilot-instructions.md and the repo's java instructions. List any deviations (e.g., field injection, missing final, non-reactive operators). Provide a short remediation for each. Also, document the test command to run and how to inspect coverage for the changed class (coverage report path).

**Review checkpoint:** No convention drift. Coverage for the changed class ≥ 90%.

---

## Step 11 — Manual validation against acceptance criteria (validation step)

**Goal:** Manually verify each Acceptance Criterion in the running application.

**Suggested prompt:**

> List the acceptance criteria from add-get-book-by-author-context-260808-001358.md and for each provide a short manual test checklist (HTTP request to make, expected HTTP status and body). Include curl examples for:
> - happy path (200 + JSON body)
> - not found (404)
> - blank author (400)
> - author >256 chars (400)
>
> Also include a sanity check that no .block() or .collectList() appears in changed classes.

**Review checkpoint:** Execute the checklist against a local dev run of the service (or test runner) and mark each AC pass/fail. If any fail, loop back to the relevant implementation step.

---

## Done criteria

Before opening a PR, confirm:

- The OpenAPI spec (F7) includes GET /books/by-author/{author} and codegen yields the expected interface.
- Controller method exists and returns Mono<ResponseEntity<BookResponse>> (AC1, AC4).
- Controller delegates to BookService; no blocking ops used (AC2).
- BookService delegates to CatalogClient.fetchBookByAuthor(...) (AC3).
- Blank or >256 author → 400; empty catalog result → 404 (AC5).
- Unit tests using StepVerifier cover happy path and validation paths; changed class has ≥ 90% line coverage (AC6).
- Do not hand-edit generated API code — update the OpenAPI spec and regenerate (AC7).

---

## Intended code changes (described as TEXT — DO NOT APPLY IN THIS PLANNING PHASE)

Below are the target file paths and the precise method signatures and minimal before/after snippets to apply during implementation. These are textual edits for the implementer to apply in the implementation phase.

1) Catalog client interface — F5

Before (excerpt):
```java
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId);
    // ...
}
```

After (excerpt):
```java
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId);

    // New: fetch by author (exact, case-insensitive)
    Mono<CatalogBookDto> fetchBookByAuthor(String author);
}
```

2) CatalogClientImpl — F6 (implementation uses existing catalogWebClient)

Before (excerpt):
```java
public class CatalogClientImpl implements CatalogClient {
    private final WebClient webClient;

    public CatalogClientImpl(@Qualifier("catalogWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<CatalogBookDto> fetchBook(String bookId) {
        return webClient.get()
            .uri("/catalog/books/{id}", bookId)
            .retrieve()
            .bodyToMono(CatalogBookDto.class);
    }
}
```

After (excerpt):
```java
    @Override
    public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
        return webClient.get()
            .uri("/catalog/books/by-author/{author}", author)
            .retrieve()
            .bodyToMono(CatalogBookDto.class);
    }
```

3) BookService interface — F3

Before (excerpt):
```java
public interface BookService {
    Mono<BookResponse> getBookById(String bookId);
}
```

After (excerpt):
```java
public interface BookService {
    Mono<BookResponse> getBookById(String bookId);

    // New: fetch a single book by exact, case-insensitive author name
    Mono<BookResponse> getBookByAuthor(String author);
}
```

4) BookServiceImpl — F4 (delegate + validation)

Before (excerpt):
```java
public class BookServiceImpl implements BookService {
    private final CatalogClient catalogClient;
    // constructor omitted

    @Override
    public Mono<BookResponse> getBookById(String bookId) {
        // existing implementation
    }
}
```

After (excerpt):
```java
    @Override
    public Mono<BookResponse> getBookByAuthor(String author) {
        if (author == null || author.trim().isEmpty()) {
            return Mono.error(new ValidationException("author must not be blank"));
        }
        if (author.length() > 256) {
            return Mono.error(new ValidationException("author must be <= 256 characters"));
        }

        return catalogClient.fetchBookByAuthor(author)
            .map(dto -> mapToBookResponse(dto)); // reuse existing mapper
            // if fetchBookByAuthor returns empty, controller will turn into 404
    }
```

5) BookController — F1 (add endpoint)

Before (excerpt):
```java
@RestController
public class BookController {
    private final BookService bookService;
    // constructor omitted

    public Mono<ResponseEntity<BookResponse>> getBookById(String bookId) {
        // existing
    }
}
```

After (excerpt):
```java
    @GetMapping("/books/by-author/{author}")
    public Mono<ResponseEntity<BookResponse>> getBookByAuthor(@PathVariable("author") String author) {
        return bookService.getBookByAuthor(author)
            .map(b -> ResponseEntity.ok(b))
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
```

6) OpenAPI (F7) — YAML snippet to add (example):
```yaml
  /books/by-author/{author}:
    get:
      summary: Get a single book by exact author name (case-insensitive)
      operationId: getBookByAuthor
      parameters:
        - name: author
          in: path
          required: true
          schema:
            type: string
            maxLength: 256
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BookResponse'
        '400':
          description: Bad Request
          content:
            application/problem+json: {}
        '404':
          description: Not Found
          content:
            application/problem+json: {}
```

7) Tests (F8) — test method outlines (use StepVerifier):

- testGetBookByAuthor_HappyPath
  - mock catalogClient.fetchBookByAuthor("Alice") -> Mono.just(dto)
  - StepVerifier.create(bookService.getBookByAuthor("Alice")).expectNextMatches(resp -> resp.getTitle().equals("...")).verifyComplete();

- testGetBookByAuthor_NotFound
  - mock to Mono.empty(); assert service returns Mono.empty() or controller returns 404 depending on mapping.

- testGetBookByAuthor_InvalidAuthor
  - author blank -> expect ValidationException
  - author length >256 -> expect ValidationException

---

## Notes & developer reminders

- Follow constructor injection and mark injected fields private final.
- Reuse existing mapping utilities; do not duplicate DTO mapping logic.
- Do not introduce timeouts, retries, or circuit breakers in this story.
- Ensure exception types used for validation map to 400 in the existing ControllerAdvice.
- Do not hand-edit generated sources from the OpenAPI codegen; change the OpenAPI and regenerate.

---

Plan written to .harness/prompt-steps.md

To execute: open this file and follow each step's Suggested prompt in Copilot Chat, keeping add-get-book-by-author-context-260808-001358.md attached.

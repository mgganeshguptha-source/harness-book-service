# Plan for: BOOK-1 - Add "get book by author" endpoint

**Source:** newest context file in .github/story-context-files/ (BOOK-1 context provided to CI)
**Stack:** Backend — Spring WebFlux (Reactive Spring / Reactor)
**Total steps:** 10
**Unresolved clarifications:** None

---

## Before you execute any step

1. Keep the BOOK-1 context file in your Copilot Chat context throughout the plan (CI already supplied it).
2. .github/copilot-instructions.md is auto-loaded by Copilot when present. This plan follows repo conventions: constructor injection, reactive patterns, no blocking.
3. Execute steps in order. This plan is for planning only — do not modify or create any .java or test files now. Implementation happens in a later phase. The only file written in this phase is this plan.

---

## Pre-flight

Assumptions the plan makes:

1. Backend uses Spring WebFlux with Reactor and a reactive CatalogClient (interface + implementation) pattern already present. The project uses constructor injection and JUnit 5 + Reactor StepVerifier for tests.
2. Existing `getBookById` reactive endpoint and `CatalogClient.fetchBook(String bookId): Mono<CatalogBookDto>` exist and serve as patterns to mirror. Behaviour preserved: reactive non-blocking handling, error mapping via ControllerAdvice, and RFC-7807 problem responses as per repo conventions.
3. Non-functional constraint: coverage gate requires changed-class line coverage >= 90%. Tests will be unit tests using mocks for CatalogClient; no external calls.

If any assumption is wrong, stop and update the context.md before proceeding.

---

## Impacted Files

| ID  | Path (relative to repo root) | Role |
|-----|------------------------------|------|
| F1  | src/main/java/.../controller/BookController.java | REST controller (contains reactive getBookById) |
| F2  | src/main/java/.../service/BookService.java | Service layer delegating to CatalogClient |
| F3  | src/main/java/.../client/CatalogClient.java | Catalog client interface (reactive) |
| F4  | src/main/java/.../client/CatalogClientImpl.java | Catalog client implementation (reactive WebClient) |
| F5  | src/main/java/.../model/BookResponse.java | API response DTO (returned by controller) |
| F6  | src/main/java/.../dto/CatalogBookDto.java | DTO used by CatalogClient (if present) |
| F7  | src/test/java/.../controller/BookControllerTest.java | Unit tests for the controller using StepVerifier |
| F8  | src/main/resources/openapi.yaml | OpenAPI specification file to update (or similar OpenAPI generator location) |

> Note: Exact package paths (the "..." above) must be filled using the real package structure in Step 1's implementation-stage inventory. Do not change package conventions.

---

## Step 1 — Inventory (seed list)

Goal: Confirm exact file paths and any non-code files required (OpenAPI spec, DTOs, test infra). Seed the inventory with the files above and ask the repository to confirm exact package paths.

Suggested prompt (for implementation-phase Copilot):

> Planning from: BOOK-1 context file. Start with these candidate files: F1 BookController, F2 BookService, F3 CatalogClient, F4 CatalogClientImpl, F5 BookResponse DTO, F6 CatalogBookDto, F7 BookControllerTest, F8 openapi.yaml. Read each file and return the exact path, package, and a one-line role. Add any legitimately required files (e.g., exception mappers, DTOs, OpenAPI generator descriptor) and remove files not impacted. Do not edit files yet.

Review checkpoint: Confirm the Impacted Files table above is updated with exact paths (replace "...") and add any missed non-code files (e.g., OpenAPI generator config, API module). Record final IDs.

---

## Step 2 — Design the API surface and validation rules

Goal: Decide exact method signatures and controller route behavior following the story decisions.

Suggested prompt:

> Using the confirmed files from Step 1, propose the exact controller method signature and the corresponding service and client method signatures for the new endpoint GET /books/by-author/{author}. Ensure: controller returns Mono<ResponseEntity<BookResponse>>; controller validates path param (non-blank, max 256); blank or >256 → respond 400 (validation); empty client result → 404. Suggest the Java types and annotations to use (e.g., @GetMapping, @PathVariable, @Valid/@Size). Do not implement code yet—just list the signatures and exact annotations.

Expected outcome (the plan's choices — copy these into implementation later):

- Controller method signature (F1):

```java
@GetMapping("/books/by-author/{author}")
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(@PathVariable("author") @NotBlank @Size(max = 256) String author)
```

- Service method signature (F2):

```java
public Mono<BookResponse> findBookByAuthor(String author)
```

- CatalogClient interface addition (F3):

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

- CatalogClientImpl (F4) will call upstream path `/catalog/books/by-author/{author}` and map CatalogBookDto → BookResponse at the service layer. Ensure WebClient usage is reactive and non-blocking.

Review checkpoint: Confirm these signatures align with the codebase patterns (parameter validation annotations, exception mapping for empty Mono → 404 via service/controller composition or ControllerAdvice mapping). If conventions differ, adjust.

---

## Step 3 — Describe the CatalogClient change (interface + impl)

Goal: Specify the exact interface addition and the HTTP call details for CatalogClientImpl (description only; do not implement in this phase).

Planned changes (text + snippet):

- F3 (CatalogClient.java) — add method:

```java
// CatalogClient (reactive client interface)
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

- F4 (CatalogClientImpl.java) — new implementation mirrors existing fetchBook(...) style. It should call GET `/catalog/books/by-author/{author}` using the shared WebClient bean, return Mono<CatalogBookDto>, and propagate empty Mono when 404 from upstream (map 404 to Mono.empty() in client impl if existing pattern does that). No retries or timeouts added.

HTTP call details (for implementer):

- Request: GET /catalog/books/by-author/{author}
- Path variable author is inserted as-is (assumed framework URL-decoded already).
- Response mapping: map HTTP 200 body → CatalogBookDto; HTTP 404 → Mono.empty(); other 4xx/5xx → propagate as a mapped TechnicalException per existing client pattern.

Review checkpoint: Ensure the CatalogClientImpl follows the repo's existing error mapping/pattern used by the other client methods (e.g., fetchBook) so exception translation is consistent.

---

## Step 4 — Describe the Service layer method

Goal: Specify the implementation contract for BookService that composes CatalogClient reactively and enforces empty result → NotFound.

Planned service method (F2):

```java
public Mono<BookResponse> findBookByAuthor(String author) {
    // Pseudocode to implement later (reactive composition only):
    // return catalogClient.fetchBookByAuthor(author)
    //    .map(dto -> mapToBookResponse(dto))
    //    .switchIfEmpty(Mono.error(new NotFoundException("Book not found for author")));
}
```

Notes for implementer:
- Do mapping from CatalogBookDto → BookResponse inside the service (or a mapper util) — do not let controller depend on Catalog DTOs.
- Throw or return a NotFoundException that is mapped by @ControllerAdvice to a 404 with RFC7807 body.
- Do NOT use block() or collectList().

Review checkpoint: Confirm mapping function and NotFoundException usage follow project conventions (exception type, message sanitisation). Add a TODO HIPAA audit comment if BookResponse might include PHI (unlikely for a sample book catalog), but follow repo policy if needed.

---

## Step 5 — Describe the Controller endpoint

Goal: Specify controller-level wiring and input validation, and the exact response wrapping.

Planned controller method (F1) — descriptive snippet:

```java
@GetMapping("/books/by-author/{author}")
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(
    @PathVariable("author") @NotBlank @Size(max = 256) String author
) {
    return bookService.findBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book));
}
```

Notes:
- Validation on path variable: use `@Validated` at controller class level if using javax validation on path variables; otherwise perform an explicit check and return BadRequest in a reactive manner. Ensure the repo's existing pattern for validating path variables is followed.
- Blank or >256 length → return 400 Bad Request (ValidationException) handled by ControllerAdvice.
- If service emits NotFoundException, ControllerAdvice maps to 404.

Review checkpoint: Confirm whether the project uses annotation-based validation on @PathVariable or explicit manual checks; follow existing pattern.

---

## Step 6 — OpenAPI spec update

Goal: Add the new operation to the OpenAPI spec so generated code and docs include it.

Planned OpenAPI snippet to add to F8 (openapi.yaml):

```yaml
  /books/by-author/{author}:
    get:
      summary: Get a single book by exact author name (case-insensitive)
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
          $ref: '#/components/responses/BadRequest'
        '404':
          $ref: '#/components/responses/NotFound'
```

Notes:
- Keep the BookResponse schema referenced to existing component.
- Ensure the operationId follows project naming conventions (e.g., getBookByAuthor).
- Do not hand-edit generated code outside the spec; update the spec so generator (if used) produces the API surface.

Review checkpoint: Confirm whether the repo uses openapi.yaml or annotations-first; follow the repository's authoritative source of truth for OpenAPI. If annotations are used (springdoc), add the @Operation annotation snippet instead in the controller in implementation phase.

---

## Step 7 — Unit tests (controller) specification

Goal: Add unit tests for the controller class (F7) using JUnit 5 and StepVerifier, mocking CatalogClient so no external calls occur. Achieve >=90% line coverage on changed class.

Test cases to implement later (descriptions and snippets):

1. Happy path — CatalogClient returns a CatalogBookDto → service maps to BookResponse → controller returns 200 with expected body.

Test pseudocode:

```java
// Arrange: mock catalogClient.fetchBookByAuthor("Alice") -> Mono.just(catalogDto)
// Act: call controller.getBookByAuthor("Alice")
// Assert: StepVerifier.create(result)
//    .expectNextMatches(responseEntity -> responseEntity.getStatusCode()==HttpStatus.OK
//         && responseEntity.getBody().getTitle().equals("Expected Title"))
//    .verifyComplete();
```

2. Not found — catalogClient returns Mono.empty() -> service emits NotFound -> controller yields 404 mapped by ControllerAdvice.

3. Validation — blank author or >256 chars -> controller validation triggers 400.

Coverage guidance:
- Aim tests to exercise both mapping functions and the controller wiring so the changed class (controller) attains >=90% line coverage. If necessary, add focused unit tests for private mapping methods via indirect behaviour.

Review checkpoint: Confirm test harness and mocking framework (Mockito or MockK) usage conventions and adapt test snippets accordingly.

---

## Step 8 — Convention drift review

Goal: After code changes, review changed files against .github/copilot-instructions.md and repo conventions.

Suggested review checklist for implementation-phase Copilot:

- Constructor injection used everywhere for new classes; no field injection.
- SLF4J logging only; no secrets or PHI logged.
- No blocking calls (.block(), .collectList()).
- All exceptions map through the single @RestControllerAdvice.
- CatalogClientImpl uses shared WebClient bean and sets timeouts only if that is standard in other clients (story says do not add timeouts — follow existing pattern).

Review checkpoint: List any drifts and justify or correct them before opening PR.

---

## Step 9 — Manual validation against Acceptance Criteria (validation step)

Goal: Walk through each AC against the running application (manual checklist). This is executed after implementation.

Suggested manual test checklist:

1. AC1: Call GET /api/v1/books/by-author/{author} with a known author — expect 200 and a BookResponse JSON. Confirm return type in code is Mono<ResponseEntity<BookResponse>>.
2. AC2: Inspect controller code — it delegates to BookService and contains no blocking calls.
3. AC3: Inspect service and ensure it calls CatalogClient.fetchBookByAuthor(...); no direct HTTP calls in service/controller.
4. AC4: Confirm reactive composition (Mono usage) and run unit tests for StepVerifier assertions.
5. AC5: Blank author -> 400; author >256 -> 400; unknown author -> 404.
6. AC6: Run unit tests and coverage report — changed class >= 90% line coverage. Attach coverage output to PR.
7. AC7: Confirm OpenAPI spec updated; regenerate client/docs if project uses generator and verify no hand-editing occurred outside spec.

If any AC fails, identify which implementation step to revisit (service, controller, client, tests) and loop back.

---

## Step 10 — Done criteria (pre-PR checklist)

Before opening a PR, confirm all of the following:

- Code compiles and all unit tests pass locally.
- Controller returns Mono<ResponseEntity<BookResponse>> and performs validation as specified.
- Service delegates to CatalogClient.fetchBookByAuthor(...), and CatalogClientImpl is the only class making the downstream HTTP call.
- No usages of .block(), .collectList(), or other blocking constructs in changed files.
- Unit tests (controller tests) cover happy path, not-found, and validation, using StepVerifier. Changed controller class has >= 90% line coverage.
- OpenAPI spec updated; regenerated artifacts (if applicable) were produced by generator and not hand-edited.
- Convention drift review done and any issues addressed.
- Add PR description referencing BOOK-1 and the acceptance criteria, including instructions to reviewers on how to run the new tests and verify the OpenAPI change.

---

## Implementation notes for later coding (do not implement in this planning phase)

- Target file edits (listed here for developer convenience; do not edit now):
  - Edit F3: add `Mono<CatalogBookDto> fetchBookByAuthor(String author);`
  - Edit F4: implement WebClient GET to `/catalog/books/by-author/{author}` returning Mono<CatalogBookDto>`
  - Edit F2: add `findBookByAuthor(String author)` method that maps DTO to BookResponse and `switchIfEmpty(Mono.error(new NotFoundException(...)))`.
  - Edit F1: add controller endpoint method returning `Mono<ResponseEntity<BookResponse>>` with path `/books/by-author/{author}` and validation annotations.
  - Edit F8: add new OpenAPI path entry for `/books/by-author/{author}` referencing BookResponse schema.
  - Add tests in F7 as described above using StepVerifier and mocking CatalogClient.

Example before/after snippets (only illustrative):

Before (F1 snippet):

```java
// existing controller: getBookById(String id)
@GetMapping("/books/{id}")
public Mono<ResponseEntity<BookResponse>> getBookById(@PathVariable String id) { ... }
```

After (F1 snippet):

```java
@GetMapping("/books/by-author/{author}")
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(
    @PathVariable("author") @NotBlank @Size(max = 256) String author
) {
    return bookService.findBookByAuthor(author)
        .map(ResponseEntity::ok);
}
```

Service mapping example (F2 pseudocode):

```java
public Mono<BookResponse> findBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
        .map(this::toBookResponse)
        .switchIfEmpty(Mono.error(new NotFoundException("Book not found for author")));
}
```

CatalogClient interface addition (F3):

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

---

Plan written to .harness/prompt-steps.md. This file is the authoritative plan for CI — do not modify other source files in this phase. Proceed to implementation phase when ready.

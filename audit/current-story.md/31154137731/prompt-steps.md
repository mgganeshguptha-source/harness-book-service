# Plan for: Add "get book by author" endpoint (BOOK-1)

**Source:** newest context file in .github/story-context-files/ (story: "BOOK-1: Add \"get book by author\" endpoint")
**Stack:** Backend — Spring Boot, Reactive (Spring WebFlux / Reactor)
**Total steps:** 11
**Unresolved clarifications:** None (the context lists explicit behaviour decisions)

---

## Before you execute any step

1. Keep the context file (the newest .md in .github/story-context-files/) in the CI run context. This plan was produced from that file.
2. .github/copilot-instructions.md is auto-loaded by Copilot when present. Follow its conventions (constructor injection, package/layout, logging rules, reactive guidelines).
3. Execute steps in order. Each implementation step is described but must not be applied in this planning phase; this file only *describes* the intended edits as text (method signatures, file paths, example before/after snippets). Implementation happens later.
4. Step 1 is an inventory step and must confirm exact file paths used in this repository before editing.

---

## Pre-flight

The plan assumes:

1. Backend uses Spring WebFlux with Reactor and project uses constructor injection and SLF4J logging.
2. Behaviour preservation: existing `getBookById` reactive pattern (returns Mono<ResponseEntity<...>>) is preserved; no blocking calls will be introduced.
3. Non-functional: no resilience (retry/timeout/circuit-breaker) is added in this story; infra owns that.

If any of these assumptions are wrong, stop and update the context or reopen the story.

---

## Impacted Files (seed list — Step 1 must confirm exact paths)

| ID | Path (seed) | Role |
|----|-------------|------|
| F1 | src/main/java/..../controller/BookController.java | REST controller (add endpoint) |
| F2 | src/main/java/..../service/BookService.java | Service layer (new method) |
| F3 | src/main/java/..../client/CatalogClient.java | Catalog client interface (add method) |
| F4 | src/main/java/..../client/CatalogClientImpl.java | Catalog client impl (calls /catalog/books/by-author/{author}) |
| F5 | src/main/java/..../dto/CatalogBookDto.java | DTO returned by CatalogClient |
| F6 | src/main/java/..../model/BookResponse.java | API response model (used by controller) |
| F7 | src/test/java/..../controller/BookControllerTest.java | Unit tests for controller (StepVerifier + mocked CatalogClient) |
| F8 | src/main/resources/openapi.yaml | OpenAPI spec (add operation) |

> Review checkpoint (Step 1): confirm exact repository file paths and add any missing files (package-root differences, test locations). If repository uses generated OpenAPI files in a different location, note that path here. After confirmation, assign the IDs above to the confirmed paths.

---

## Step 1 — Inventory: confirm actual paths and any non-code files

**Goal:** Confirm the real file paths (replace seed paths above) and detect any non-code files required (OpenAPI source, generated client locations).

**Suggested prompt:**

> Planning from: the newest context file in .github/story-context-files/ (story BOOK-1). Starting with these candidate files: [list the seed paths from the Impacted Files block]. Read each file and reply with the exact path in this repo, one-line role, and note any missing files the change requires (OpenAPI source, DTOs, test classes). Also confirm where OpenAPI is defined (openapi.yaml / generated folder). Do not change files yet.

**Review checkpoint:** Replace each seed path in the Impacted Files block with the confirmed exact path. Add any newly discovered files as F9, F10, … Do not proceed until the file list is exact.

---

## Step 2 — Design: Controller + Service + Client interaction

**Goal:** Decide exact method signatures and exception/empty handling flow (400 for blank author, 404 for empty Mono result).

**Suggested prompt:**

> Using the confirmed file paths (Impact Files block), propose the concrete method signatures, return types, and error-handling flow for the new feature. Include: 1) CatalogClient.fetchBookByAuthor(String author) -> Mono<CatalogBookDto>; 2) BookService.getBookByAuthor(String author) -> Mono<BookResponse>; 3) BookController.getBookByAuthor(@PathVariable String author) -> Mono<ResponseEntity<BookResponse>>. Describe how blank author yields 400 and empty result maps to 404. Do not write code yet, just list signatures and the Reactor composition flow.

**Expected decisions (to approve):**
- CatalogClient.fetchBookByAuthor(String author): Mono<CatalogBookDto>
- BookService.getBookByAuthor(String author): Mono<BookResponse>
- BookController.getBookByAuthor(String author): Mono<ResponseEntity<BookResponse>>
- Blank/blank-only author -> respond with 400 Bad Request immediately (Mono.just(ResponseEntity.badRequest().build()) or Mono.error mapped by ControllerAdvice to RFC7807 with 400). For this sample, choose immediate 400 response from controller.
- If CatalogClient returns empty -> controller returns Mono.just(ResponseEntity.notFound().build()). Service should return Mono.empty() to allow controller to map to 404.

**Review checkpoint:** Approve these decisions. If you prefer exceptions vs explicit response mapping, pick one now (plan assumes explicit response mapping in controller).

---

## Step 3 — Describe exact code edits (controller) — textual patch only

**Goal:** Describe the intended controller changes (file F1).

**Suggested content to include in commit-phase (do not execute now):**

- Add new handler method to BookController:

```java
// in BookController.java (add method)
@GetMapping("/api/v1/books/by-author/{author}")
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(@PathVariable("author") String author) {
    if (!StringUtils.hasText(author)) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.getBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book))
        .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

- Notes: use org.springframework.util.StringUtils for blank check. Ensure `bookService` is constructor-injected `private final BookService bookService;`.

**Review checkpoint:** Confirm the method signature and behaviour mapping match AC1, AC5 and the reactive pattern (no blocking). If the project prefers throwing ValidationException for blank input mapped by ControllerAdvice, adjust accordingly (plan currently uses explicit 400 response).

---

## Step 4 — Describe service-layer change (file F2)

**Goal:** Add a service method that delegates to CatalogClient and maps CatalogBookDto -> BookResponse.

**Suggested service method signature and example implementation to add (text only):**

```java
// in BookService.java
public Mono<BookResponse> getBookByAuthor(String author) {
    // delegate to catalog client and convert DTO
    return catalogClient.fetchBookByAuthor(author)
        .map(dto -> new BookResponse(dto.getId(), dto.getTitle(), dto.getAuthor(), dto.getPublishedDate(), dto.getPrice()));
}
```

- Notes: keep transformation simple and reactive; do not block. If multiple catalog results are possible, the CatalogClient contract returns at most one; use the first if a list were returned (per story decisions) — but CatalogClient.fetchBookByAuthor is Mono, so upstream should enforce at most one.

**Review checkpoint:** Confirm DTO->Response mapping fields align with existing BookResponse fields.

---

## Step 5 — Describe CatalogClient changes (F3, F4)

**Goal:** Add the CatalogClient interface method and implement it in CatalogClientImpl to call `/catalog/books/by-author/{author}` reactively.

**Suggested additions (text only):**

```java
// in CatalogClient.java (interface)
Mono<CatalogBookDto> fetchBookByAuthor(String author);

// in CatalogClientImpl.java (WebClient-based implementation)
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient
        .get()
        .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}").build(author))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(CatalogBookDto.class)
        ;
}
```

- Notes: Use the existing WebClient bean used elsewhere in the project (do not new one). Do not add timeouts/retries here (per constraints). Sanitize author for header injection/SSRF concerns only if user-controlled URL parts are concatenated — here path variable usage with proper WebClient build is acceptable.

**Review checkpoint:** Confirm which WebClient bean the impl should use and the package/class names.

---

## Step 6 — DTO / Model alignment (F5, F6)

**Goal:** Confirm DTOs and response models used by CatalogClient and controller. If missing, add minimal DTOs.

**Suggested DTO signatures (text only):**

```java
// CatalogBookDto.java
public class CatalogBookDto {
    private String id;
    private String title;
    private String author;
    private String publishedDate; // ISO-8601 string
    private String price; // decimal-as-string
    // getters/setters
}

// BookResponse.java
public class BookResponse {
    private String id;
    private String title;
    private String author;
    private String publishedDate;
    private String price;
    // constructor, getters
}
```

**Review checkpoint:** Verify these align with existing models in the repo and adjust field names accordingly.

---

## Step 7 — Unit tests (F7): controller happy path + blank + not-found

**Goal:** Add JUnit 5 tests for BookController using Reactor StepVerifier and a mocked CatalogClient (or mocked BookService depending on where controller delegates). Aim for ≥90% line coverage on the changed class.

**Suggested test cases (text only):**

1. Happy path: CatalogClient returns Mono.just(CatalogBookDto) -> expect ResponseEntity OK (200) with body matching BookResponse fields. Use StepVerifier on the Mono returned by controller method or use WebTestClient bound to controller.

2. Blank author: call controller method with empty/blank author -> expect 400 response.

3. Not-found: CatalogClient returns Mono.empty() -> expect 404 response.

**Example test snippet (using WebTestClient bound to controller):**

```java
// in BookControllerTest.java
@Test
void getBookByAuthor_happyPath() {
    when(bookService.getBookByAuthor("Jane Doe")).thenReturn(Mono.just(new BookResponse("id1","Title","Jane Doe","2020-01-01","12.00")));

    webTestClient.get().uri("/api/v1/books/by-author/Jane%20Doe")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo("id1")
        .jsonPath("$.author").isEqualTo("Jane Doe");
}
```

- Use Mockito to mock service or client. Use StepVerifier if invoking controller method directly as Mono.
- Ensure tests do not perform any external HTTP calls.

**Review checkpoint:** Confirm test framework and helper utilities (WebTestClient vs direct Mono StepVerifier) match repository conventions.

---

## Step 8 — OpenAPI spec update (F8)

**Goal:** Add the operation to the OpenAPI source so generated code (if any) includes the new endpoint. Do not hand-edit generated code; update spec and re-run generator in implementation phase.

**Suggested OpenAPI operation (YAML fragment):**

```yaml
/path: /books/by-author/{author}:
  get:
    summary: Get a single book by exact (case-insensitive) author name
    parameters:
      - name: author
        in: path
        required: true
        schema:
          type: string
    responses:
      '200':
        description: OK
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BookResponse'
      '400':
        description: Bad Request
      '404':
        description: Not Found
    tags:
      - books
```

**Notes:** If this repo uses code generation from the OpenAPI spec, update the spec and then regenerate the server stubs during the implementation phase. Do not hand-edit generated code outside the generator.

**Review checkpoint:** Confirm the canonical OpenAPI source path and generator steps (mvn plugin, gradle task, etc.).

---

## Step 9 — Coverage and test-run step (CI)

**Goal:** Ensure tests provide ≥90% line coverage for the changed class (controller or service depending on where most changes land).

**Suggested prompt for implementation-phase CI:**

> Run the existing unit test suite (mvn -DskipITs=false test or gradle test) and report coverage for the changed class. If coverage < 90%, add tests that exercise the remaining branches (blank input, found, not found, dto mapping). Ensure no external calls are made in tests.

**Review checkpoint:** Coverage report shows >=90% for the changed class.

---

## Step 10 — Manual validation against Acceptance Criteria

**Goal:** Walk through ACs with the running application (manual checks). This is performed after code + tests are merged and the service is running locally/in test env.

**Manual validation checklist (examples):**

- AC1: curl -i GET http://localhost:8080/api/v1/books/by-author/Jane%20Doe -> 200 + JSON body matching BookResponse
- AC5 (blank): curl -i GET /api/v1/books/by-author/"" -> 400
- AC5 (not found): mock CatalogClient to return empty and confirm GET -> 404
- AC3: verify the controller/service uses CatalogClient.fetchBookByAuthor (code inspection)
- AC6: run unit tests and confirm using StepVerifier that the happy path asserts response body fields

**Review checkpoint:** Mark each AC pass/fail. If fail, loop back to the appropriate implementation/test step.

---

## Step 11 — Convention drift review

**Goal:** Before opening a PR, run a convention drift check against .github/copilot-instructions.md and repo rules.

**Suggested prompt / checklist:**

- Confirm constructor injection used for new dependencies (no field injection).
- Confirm logging uses SLF4J and no PHI is logged.
- Confirm no `.block()` or blocking operators introduced.
- Confirm controller remains thin and business logic is in service.
- Confirm OpenAPI change is the source of record and generated code wasn't hand-modified.

**Review checkpoint:** If any drift is found, fix in implementation-phase edits.

---

## Done criteria (map to Acceptance Criteria)

Before opening a PR, confirm:

- [ ] AC1: BookController exposes GET /api/v1/books/by-author/{author} returning Mono<ResponseEntity<BookResponse>>.
- [ ] AC2: Controller delegates to BookService; no blocking operations anywhere in the call chain.
- [ ] AC3: Service delegates to CatalogClient.fetchBookByAuthor(...); there are no direct downstream HTTP calls from controller/service other than the CatalogClient impl.
- [ ] AC4: Reactive types are composed (Mono) — no collectList()/block() used.
- [ ] AC5: Blank author -> 400; no match -> 404.
- [ ] AC6: Unit test with StepVerifier or WebTestClient covers happy path and asserts response body; changed class has >= 90% line coverage.
- [ ] AC7: OpenAPI spec updated; any generated code is produced from the spec and not hand-modified.

---

## Notes and risks

- Risk: package naming and exact file paths may differ from seed list — Step 1 must confirm and adjust IDs accordingly.
- Risk: repository may use generated DTOs/models from OpenAPI; avoid duplicating model classes — prefer the generated ones. Step 1 must confirm where models live.
- HIPAA / logging: ensure no PHI is logged in any new log statements.
- OWASP: endpoint is public per story; still validate inputs and do not expose internal errors (ControllerAdvice will sanitize errors).

---

Plan written by build-prompt-steps CI-mode. To execute: open this plan, confirm the Impacted Files mapping (Step 1), then paste each implementation step's suggested edits into a code-change session and follow the review checkpoints. Do not implement code changes until the file paths are confirmed in Step 1.

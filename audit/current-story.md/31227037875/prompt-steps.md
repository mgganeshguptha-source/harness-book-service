# Plan for: BOOK-1 — Add "get book by author" endpoint

**Source:** newest file in .github/story-context-files (BOOK-1: Add "get book by author" endpoint)
**Stack:** Backend — Spring Boot WebFlux (Reactive Spring / Reactor), Spring WebClient used by CatalogClient.
**Total steps:** 10
**Unresolved clarifications:** None (all clarifications resolved in the context file)

---

## Before you execute any step

1. Keep the context file (newest in .github/story-context-files containing the BOOK-1 story) in your Copilot Chat context throughout the plan.
2. .github/copilot-instructions.md is auto-loaded by Copilot when present in the repo. If team conventions aren't applied, verify that file exists.
3. Execute steps in order. If you restart a session, re-attach the context file and this plan.
4. This planning phase must not modify any Java or test source. Implementation will follow this plan in a later phase. Do not create or edit any files under src/main or src/test now — the harness will reject such writes in this phase.

---

## Pre-flight

Assumptions the plan makes:

1. Backend uses Spring Boot WebFlux (Reactive) with Reactor and a reactive CatalogClient implemented via WebClient. Project uses constructor injection and standard package layout under src/main/java.
2. Existing reactive `getBookById` pattern exists and will be followed: controllers return Reactor types (Mono<ResponseEntity<...>>) and services/clients return Mono/Flux. The new endpoint preserves existing response shapes and error mapping behaviors.
3. Non-functional constraints (timeouts/retries/circuit-breakers/rate-limiting) are out of scope and handled by infra; this story adds no resilience code.

If any assumption is wrong, stop and correct the context or plan before proceeding.

---

## Impacted Files (seed — Step 1 will confirm exact paths and add any missing files like OpenAPI spec)

| ID | Path (seed) | Role |
|----|-------------|------|
| F1 | src/main/java/**/controller/BookController.java | REST controller — add GET /books/by-author/{author} |
| F2 | src/main/java/**/service/BookService.java | Service layer — add lookup method delegating to CatalogClient |
| F3 | src/main/java/**/client/CatalogClient.java | Client interface — add fetchBookByAuthor(String) : Mono<CatalogBookDto> |
| F4 | src/main/java/**/client/CatalogClientImpl.java | Client implementation — call /catalog/books/by-author/{author} using WebClient |
| F5 | src/main/java/**/model/BookResponse.java | API DTO returned by controller |
| F6 | src/main/java/**/model/CatalogBookDto.java | DTO from CatalogClient (if not present, create a mirror) |
| F7 | src/test/java/**/controller/BookControllerTest.java | Unit test for controller (StepVerifier, mock BookService/CatalogClient) |
| F8 | src/main/resources/openapi.yaml (or src/main/resources/static/openapi.yaml) | OpenAPI spec — add operation for GET /books/by-author/{author} |

> Note: Step 1 must confirm the actual package paths used in the repo and replace `**` with real package segments. Do not hardcode package names until Step 1 confirms them.

---

## Step 1 — Inventory & confirm impacted files (always run first)

Goal: Confirm the exact file paths, packages, and presence/shape of relevant DTOs, tests, and the OpenAPI spec. Ensure non-code files required (OpenAPI spec) are identified.

Suggested prompt (paste into Copilot Chat with the attached context file):

> Planning from: [BOOK-1 context file].
>
> Inventory request: Start with the following candidate files: F1: src/main/java/**/controller/BookController.java, F2: src/main/java/**/service/BookService.java, F3: src/main/java/**/client/CatalogClient.java, F4: src/main/java/**/client/CatalogClientImpl.java, F5: src/main/java/**/model/BookResponse.java, F6: src/main/java/**/model/CatalogBookDto.java, F7: src/test/java/**/controller/BookControllerTest.java, F8: src/main/resources/openapi.yaml. Read the repository and confirm which of these files exist, provide the exact package-qualified paths, and add any missing but required files (for example, a DTO or test package). Also indicate where the existing `getBookById` controller and its test live so we can mirror patterns. Do not make edits — only list and confirm exact paths and roles. Also report whether openapi.yaml is maintained by codegen or manual YAML so we decide where to update the spec.

Review checkpoint: Confirm the Impacted Files table above is replaced with exact package-qualified paths and add any missing FIDs. If the project uses a different OpenAPI location (annotations vs YAML), note that so later steps update the right artifact.

---

## Step 2 — Design: controller method signature, validation, and route

Goal: Decide and confirm the controller method signature and validation behaviour for author path param.

Suggested prompt:

> Using the file list confirmed in Step 1, propose an implementation for the controller endpoint that exactly matches the story ACs: route GET /api/v1/books/by-author/{author} (or the repo's API base path), method returns Mono<ResponseEntity<BookResponse>>, validates that `author` is non-blank and <= 256 chars, and returns 400 on invalid input. Show the exact method signature and required annotations (Spring WebFlux annotations, @PathVariable, validation annotations or manual reactive validation) and explain how to avoid blocking. Do not edit files yet — just propose the signature and short rationale. If the project consistently uses an API base path (e.g., /api/v1), follow that base path.

Review checkpoint: Confirm the proposed method signature, path, and validation approach match ACs (blank → 400, >256 → 400) and match the project's existing `getBookById` pattern.

---

## Step 3 — Design: CatalogClient addition

Goal: Propose the exact interface method and WebClient call details for CatalogClient.fetchBookByAuthor(String).

Suggested prompt:

> In the confirmed CatalogClient (F3) interface and implementation (F4), propose an added method with signature `Mono<CatalogBookDto> fetchBookByAuthor(String author)`. For CatalogClientImpl, propose the WebClient call to `/catalog/books/by-author/{author}` returning a Mono<CatalogBookDto>, including how path variable is encoded (use WebClient.uri with builder). Show the interface method signature and a compact code snippet for the implementation using WebClient.exchangeToMono or retrieve().bodyToMono(...). Ensure the snippet is reactive and contains no blocking calls. Do not implement yet.

Review checkpoint: Confirm method signature and that the implementation snippet follows the project's WebClient conventions (error mapping, status handling) and is reactive.

---

## Step 4 — Implementation plan: BookService change

Goal: Add a service method that delegates to CatalogClient.fetchBookByAuthor and maps CatalogBookDto to BookResponse.

Suggested prompt:

> Edit F2 (BookService) to add a method `public Mono<BookResponse> findBookByAuthor(String author)` that calls `catalogClient.fetchBookByAuthor(author)`, maps the returned CatalogBookDto to BookResponse (via a mapper method or constructor), and returns Mono.empty() mapped to Mono.error(new NotFoundException(...)) or other existing not-found handling so the controller can return 404. Show the exact method signature and a concise reactive composition (no .block(), no .collectList()). If the project uses a mapper utility, use it; otherwise propose a private mapping method in the service. Do not edit files yet — just provide the intended code snippet.

Review checkpoint: Confirm the service method signature, mapping approach, and how empty result is handled to produce a 404 at the controller layer (or service returns Mono.empty and controller converts to 404). The chosen approach must match existing patterns in the project.

---

## Step 5 — Implementation plan: Controller wiring

Goal: Implement the controller endpoint to call BookService.findBookByAuthor and translate results to ResponseEntity.

Suggested prompt:

> Edit F1 (BookController) to add the new handler (use the method signature confirmed in Step 2). The handler must: validate `author` (blank and length), call `bookService.findBookByAuthor(author)`, map the Mono<BookResponse> into `Mono<ResponseEntity<BookResponse>>` returning `ResponseEntity.ok(body)` for present value or `ResponseEntity.notFound().build()` for empty. Show the exact reactive composition (e.g., `bookService.findBookByAuthor(author).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build())`). Ensure no blocking. Provide before/after snippets for the controller file showing only the new method. Do not edit files yet.

Review checkpoint: Confirm controller snippet uses Reactor composition and error/empty handling as required by AC6 and AC5.

---

## Step 6 — DTO mapping & model changes

Goal: Confirm existing DTOs or add lightweight DTOs to mirror CatalogBookDto and BookResponse; provide mapping snippets.

Suggested prompt:

> Inspect F5/F6. If CatalogBookDto exists, reuse it; otherwise design a minimal CatalogBookDto with fields required for BookResponse mapping. Propose BookResponse fields (id, title, author, publishedDate, etc.) consistent with existing API types. Provide exact class signatures (fields, getters/constructors or Lombok annotations used in the repo). Provide a small mapping method: `private BookResponse toBookResponse(CatalogBookDto src) { ... }`. Do not edit files yet.

Review checkpoint: Confirm DTO field choices match other API DTOs for consistency and that mapping is null-safe.

---

## Step 7 — Tests: unit tests for controller (happy path + input validation)

Goal: Add unit tests that achieve the coverage gate: happy path StepVerifier asserting ResponseEntity.ok and response body; validation tests for blank and >256 author returning 400; and empty catalog → 404.

Suggested prompt:

> Create/update F7 (BookControllerTest) to include tests using JUnit5 and Reactor StepVerifier (or WebTestClient depending on existing test patterns). Mock BookService (or CatalogClient if controller calls it directly) to return a Mono.just(CatalogBookDto/BookResponse) for the happy path. Use StepVerifier to assert the controller returns ResponseEntity with expected body. Add tests for blank author (expect 400) and author >256 chars (expect 400) and for empty Mono from service → expect 404. Ensure tests mock reactive types and avoid starting the server. Provide full test method stubs and assertions. Do not edit files yet.

Review checkpoint: Confirm tests use existing test utilities in the repo and that the happy-path test covers the response body fields to meet coverage requirement.

---

## Step 8 — OpenAPI spec update

Goal: Add an operation to the repository's OpenAPI spec (F8) so generated API docs include the new endpoint; ensure generated code / spec generation approach is respected.

Suggested prompt:

> Determine how this project maintains its OpenAPI spec (YAML under resources or annotations). If the canonical spec is F8 (openapi.yaml), add an operation under paths `/books/by-author/{author}` with GET, path parameter `author` (string, maxLength 256), responses 200 (application/json -> BookResponse schema), 400 (validation problem), 404 (problem). If the project uses annotations+codegen, instruct to add controller method-level annotations (springdoc or swagger) instead of editing YAML. Provide the exact YAML fragment or annotation snippet to add; do not modify files now.

Review checkpoint: Confirm where the spec is maintained and that the provided YAML/annotation snippet matches the project's existing schemas (BookResponse schema reference) and generation workflow.

---

## Step 9 — Convention drift review

Goal: After implementing code, run a review against .github/copilot-instructions.md and repository conventions.

Suggested prompt:

> After code changes, run a review: compare each changed file (list them) against .github/copilot-instructions.md and coding conventions. Report any drift (constructor injection, logging style, test patterns, package placement, OpenAPI approach). List exact fixes where conventions were not followed so the developer can apply them.

Review checkpoint: Confirm no convention drift remains. If drift exists, fix it before opening the PR.

---

## Step 10 — Manual validation against Acceptance Criteria (validation step)

Goal: Walk through each AC manually against a running instance or unit-test outputs.

Suggested prompt (manual checklist for developer):

> For each acceptance criterion from the context file, describe a manual verification step:
> - AC1: Call GET /api/v1/books/by-author/Exact%20Author and expect 200 + BookResponse (or 404 if not present). Show curl example.
> - AC2: Inspect controller and service code to confirm no .block() or .collectList() usages were added.
> - AC3: Confirm BookService calls CatalogClient.fetchBookByAuthor(...) — no direct WebClient calls from controller.
> - AC4: Inspect types: Mono<ResponseEntity<BookResponse>> returned by controller; service/client return Mono types.
> - AC5: Verify 400 responses for blank and >256 author, and 404 when catalog returns empty.
> - AC6: Run unit tests and verify StepVerifier-based test(s) and coverage report show ≥90% line coverage on changed class.
> - AC7: Confirm OpenAPI spec includes the new operation.
>
> For each failed item, list which implementation step to revisit.

Review checkpoint: Mark each AC pass/fail after manual verification; if any fail, return to the appropriate step and iterate.

---

## Intended code changes (DESCRIBE only — do NOT implement in this planning phase)

Below are the exact target file paths and the intended method signatures / minimal before/after snippets to be applied during the implementation phase. These are descriptions only; do not create or edit these files now.

1) F3 — CatalogClient interface (add):

```java
// interface: src/main/java/<package>/client/CatalogClient.java
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId); // existing

    // new
    Mono<CatalogBookDto> fetchBookByAuthor(String author);
}
```

2) F4 — CatalogClientImpl (implementation snippet):

```java
// src/main/java/<package>/client/CatalogClientImpl.java
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient
        .get()
        .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}").build(author))
        .retrieve()
        .bodyToMono(CatalogBookDto.class);
}
```

3) F2 — BookService (add method):

```java
// src/main/java/<package>/service/BookService.java
public Mono<BookResponse> findBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
        .map(this::toBookResponse);
}

private BookResponse toBookResponse(CatalogBookDto dto) {
    return new BookResponse(dto.getId(), dto.getTitle(), dto.getAuthor(), dto.getPublishedDate());
}
```

Note: choose whether service should return Mono.empty() for not-found or throw a NotFoundException; match existing project patterns. The controller snippet below assumes the service returns Mono.empty() when not found.

4) F1 — BookController (add handler):

```java
// src/main/java/<package>/controller/BookController.java
@GetMapping("/books/by-author/{author}")
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(@PathVariable("author") String author) {
    if (author == null || author.isBlank() || author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }
    return bookService.findBookByAuthor(author)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

If the project prefers annotation-driven validation, use `@Validated` at class level and a `@Size(max=256) @NotBlank @PathVariable` pattern compatible with WebFlux; otherwise explicit reactive validation as shown is acceptable and avoids blocking.

5) F5 / F6 — DTOs

- CatalogBookDto: mirror fields returned by catalog service.
- BookResponse: public API DTO with fields selected for clients.

Provide minimal class shapes (Lombok `@Data` or explicit getters) consistent with project conventions.

6) F7 — Unit test outlines (controller test):

```java
// src/test/java/<package>/controller/BookControllerTest.java
@Test
void getBookByAuthor_happyPath_returnsBook() {
    when(bookService.findBookByAuthor("Jane Austen"))
        .thenReturn(Mono.just(new BookResponse(...)));

    Mono<ResponseEntity<BookResponse>> result = controller.getBookByAuthor("Jane Austen");

    StepVerifier.create(result)
        .expectNextMatches(resp -> resp.getStatusCode().is2xxSuccessful()
            && resp.getBody().getAuthor().equals("Jane Austen"))
        .verifyComplete();
}

// tests for blank author (expect 400), long author (>256) expect 400, and empty service -> 404
```

7) F8 — OpenAPI YAML fragment example (if repository uses YAML):

```yaml
paths:
  /books/by-author/{author}:
    get:
      summary: Get a single book by exact author (case-insensitive)
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

---

## Done criteria (before opening a PR)

- New endpoint GET /books/by-author/{author} implemented and returns Mono<ResponseEntity<BookResponse>> (AC1).
- Controller delegates to BookService; no blocking operations introduced (AC2).
- BookService delegates to CatalogClient.fetchBookByAuthor(...) (AC3).
- Reactive types used end-to-end (AC4).
- Input validation: blank and >256 author → 400; catalog empty → 404 (AC5).
- Unit tests exist with StepVerifier / JUnit 5 covering happy path, validation, and not-found; changed class has >= 90% line coverage per repository coverage report (AC6).
- OpenAPI spec updated to include the new operation (AC7).
- Convention drift review passed (Step 9) and any flagged items fixed.

---

## Notes and developer guidance

- Match existing package names and coding patterns discovered in Step 1 (constructor injection, Lombok usage, exception translation via ControllerAdvice).
- Prefer the repository's existing error mapping strategy: if the project maps Mono.empty() to 404 at ControllerAdvice, follow that pattern; otherwise use `.defaultIfEmpty(ResponseEntity.notFound().build())` in the controller as shown.
- Do not add resilience (timeouts/retries/circuit-breakers). The CatalogClientImpl snippet intentionally keeps it simple.
- Ensure mapping methods are null-safe and defensive in case catalog returns missing fields.

---

Plan written for CI-mode consumption. To implement: run the steps in order in an implementation phase — creating and editing the files listed above. This planning artifact intentionally contains only descriptive snippets; do not apply changes during this phase.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-07T23:31:23
- phase: coding
- approved impacted files: ['src/main/java/**/client/CatalogClient.java', 'src/main/java/**/client/CatalogClientImpl.java', 'src/main/java/**/controller/BookController.java', 'src/main/java/**/model/BookResponse.java', 'src/main/java/**/model/CatalogBookDto.java', 'src/main/java/**/service/BookService.java', 'src/main/resources/openapi.yaml (or src/main/resources/static/openapi.yaml)', 'src/test/java/**/controller/BookControllerTest.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-application/src/main/resources/openapi.yaml']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-application/src/main/resources/openapi.yaml']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-07T23:31:23

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-07T23:34:48
- phase: coding
- approved impacted files: ['src/main/java/**/client/CatalogClient.java', 'src/main/java/**/client/CatalogClientImpl.java', 'src/main/java/**/controller/BookController.java', 'src/main/java/**/model/BookResponse.java', 'src/main/java/**/model/CatalogBookDto.java', 'src/main/java/**/service/BookService.java', 'src/main/resources/openapi.yaml (or src/main/resources/static/openapi.yaml)', 'src/test/java/**/controller/BookControllerTest.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-07T23:34:48

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-07T23:40:47
- phase: coding
- approved impacted files: ['src/main/java/**/client/CatalogClient.java', 'src/main/java/**/client/CatalogClientImpl.java', 'src/main/java/**/controller/BookController.java', 'src/main/java/**/model/BookResponse.java', 'src/main/java/**/model/CatalogBookDto.java', 'src/main/java/**/service/BookService.java', 'src/main/resources/openapi.yaml (or src/main/resources/static/openapi.yaml)', 'src/test/java/**/controller/BookControllerTest.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-07T23:40:47

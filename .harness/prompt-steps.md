# Plan for: BOOK-1: Add "get book by author" endpoint

**Source:** newest file in .github/story-context-files/ (story: "BOOK-1: Add \"get book by author\" endpoint") + .github/copilot-instructions.md
**Stack:** Backend — Spring Boot Reactive (WebFlux + Reactor), Spring-style layered app
**Total steps:** 10
**Unresolved clarifications:** None

---

## Before you execute any step

1. Keep the context file (newest file in .github/story-context-files/) available to reference while implementing.
2. .github/copilot-instructions.md is available in the repo and will be used by the team — ensure generated code follows those conventions (constructor injection, private final fields, Reactor patterns, no blocking).
3. Execute steps in order. This plan is CI-mode and the coding phase MUST NOT create or modify tests or OpenAPI-generated modules; tests belong to the unit_testing phase.

---

## Pre-flight

The plan assumes:

1. Backend is Spring WebFlux with Reactor; controller code uses reactive return types (Mono<ResponseEntity<...>>), no blocking calls, and WebClient is used in CatalogClientImpl per existing patterns.
2. Behaviour preserved: existing BooksApi interface already declares getBookByAuthor(...) — the controller will implement that interface method (no new @GetMapping on the controller). Existing getBookById behaviour remains unchanged.
3. Non-functional: no new resilience or timeout code — infra/ops owns those concerns.

If any assumption is wrong, stop and update the context file before coding.

---

## Impacted Files

| ID | Path | Role |
|----|------|------|
| F1 | src/main/java/com/example/service/controller/BooksController.java | Controller implementing BooksApi — implement getBookByAuthor(...) by delegating to BookService
| F2 | src/main/java/com/example/service/service/BookService.java | Service interface — add method signature fetchByAuthor / getBookByAuthor
| F3 | src/main/java/com/example/service/service/impl/BookServiceImpl.java | Service implementation — call CatalogClient.fetchBookByAuthor and map DTO → BookResponse
| F4 | src/main/java/com/example/service/client/CatalogClient.java | Catalog client interface — add fetchBookByAuthor(String author): Mono<CatalogBookDto>
| F5 | src/main/java/com/example/service/client/impl/CatalogClientImpl.java | Catalog client implementation — call catalog endpoint /catalog/books/by-author/{author} (reactive WebClient)
| F6 | src/main/java/com/example/service/model/CatalogBookDto.java | DTO for catalog response (if not present, add minimal fields required for mapping)
| F7 | src/main/java/com/example/service/model/BookResponse.java | API response DTO (existing) — map fields from CatalogBookDto into this

> Notes: Step 1 (inventory) must confirm these files exist and add any missing files (e.g., DTOs or package names). If a path differs in this repo, update the ID->Path mappings before coding.

---

## Step 1 — Inventory (seed candidate list)

**Goal:** Confirm the exact file set to change (including any non-code files such as configs/migration if required). Do not modify files yet — only produce a verified list and assign IDs.

**Suggested prompt (for local developer/Copilot run):**

> Planning from: newest file in .github/story-context-files/ (BOOK-1...). Start with these candidate files: src/main/java/**/controller/BooksController.java, src/main/java/**/service/BookService.java, src/main/java/**/service/impl/BookServiceImpl.java, src/main/java/**/client/CatalogClient.java, src/main/java/**/client/impl/CatalogClientImpl.java, src/main/java/**/model/CatalogBookDto.java, src/main/java/**/model/BookResponse.java. Read the repository and confirm which of these paths actually exist and list any additional files required (package names, existing DTOs to reuse). Also list any files that must NOT be edited (generated modules). Return a table of Path + one-line role per file. Do not edit files — only inventory.

**Review checkpoint:** Confirm the Impacted Files table above matches the project's actual paths and package names. If package names differ, update the table before proceeding.

---

## Step 2 — Design: service and client signatures + validation

**Goal:** Decide exact method names, parameter types, and validation responsibilities.

**Decision / agreed design (apply when implementing):**

- CatalogClient interface: add

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

- BookService interface: add

```java
Mono<BookResponse> getBookByAuthor(String author);
```

(Controller returns Mono<ResponseEntity<BookResponse>> and maps empty -> 404.)

- Validation rules: author must not be blank and length <= 256. Controller validates and returns 400 for invalid input (use Reactor-friendly validation or simple imperative null/blank check before delegation). Do not use blocking validation frameworks that trigger synchronous IO.

**Suggested prompt:**

> Propose 2 implementation options for where to perform the "blank or >256" validation: (A) controller-level pre-check (simple, explicit, returns Mono.just(ResponseEntity.badRequest())), (B) service-level validation that returns Mono.error(ValidationException) and relies on @ControllerAdvice mapping. For each option list pros/cons and recommend one consistent with thin-controller rule in .github/copilot-instructions.md.

**Review checkpoint:** Pick the option consistent with the repo's existing validation pattern. If controllers currently do simple checks before delegating, choose (A); if the project centralises validation with exceptions and ControllerAdvice, choose (B).

---

## Step 3 — Implement CatalogClient.fetchBookByAuthor (planned edits)

**Goal:** Add the client method signature to the CatalogClient interface and implement it in CatalogClientImpl using WebClient in a fully reactive way.

**Target files:** F4 (CatalogClient.java), F5 (CatalogClientImpl.java)

**Planned code snippet — CatalogClient (interface):**

```java
// F4: CatalogClient.java
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId);

    // New method
    Mono<CatalogBookDto> fetchBookByAuthor(String author);
}
```

**Planned code snippet — CatalogClientImpl (implementation):**

```java
// F5: CatalogClientImpl.java (reactive, using WebClient)
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}")
            .build(author))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(CatalogBookDto.class);
}
```

**Review checkpoint:** Confirm CatalogClientImpl uses the project's shared WebClient bean or existing client pattern and that no .block() or blocking collectors are introduced.

---

## Step 4 — Implement BookService.getBookByAuthor (planned edits)

**Goal:** Add service interface method and implementation that delegates to CatalogClient.fetchBookByAuthor, maps CatalogBookDto -> BookResponse, returns Mono<BookResponse>.

**Target files:** F2 (BookService.java), F3 (BookServiceImpl.java)

**Planned code snippet — BookService (interface):**

```java
// F2: BookService.java
public interface BookService {
    Mono<BookResponse> getBookById(String id);

    // New method
    Mono<BookResponse> getBookByAuthor(String author);
}
```

**Planned code snippet — BookServiceImpl (implementation):**

```java
// F3: BookServiceImpl.java
@Override
public Mono<BookResponse> getBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
        .map(this::mapCatalogDtoToBookResponse);
}

private BookResponse mapCatalogDtoToBookResponse(CatalogBookDto dto) {
    return BookResponse.builder()
        .id(dto.getId())
        .title(dto.getTitle())
        .author(dto.getAuthor())
        // map other fields as needed
        .build();
}
```

**Review checkpoint:** Confirm mapping follows existing conventions for BookResponse and reuses builder or constructor used elsewhere. Ensure method returns empty Mono -> mapping is not applied; the controller will handle empty -> 404.

---

## Step 5 — Implement Controller method to satisfy BooksApi.getBookByAuthor (planned edits)

**Goal:** Implement the existing interface method on the controller class (F1) so it delegates to BookService.getBookByAuthor and returns Mono<ResponseEntity<BookResponse>>; perform input validation and map empty -> 404.

**Target file:** F1 (BooksController.java)

**Planned code snippet — Controller method (implementing interface method):**

```java
// F1: BooksController.java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    // Validation: blank or >256 -> 400
    if (author == null || author.isBlank() || author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.getBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book))
        .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

**Notes:**
- Do not add @GetMapping at controller level — controller already implements generated BooksApi which declares the mapping.
- Use constructor injection for BookService.

**Review checkpoint:** Confirm the controller implements BooksApi.getBookByAuthor method signature exactly and that no new mapping annotations are added.

---

## Step 6 — DTO and mapping checks (planned edits / additions)

**Goal:** Ensure CatalogBookDto has the fields returned by the catalog service and that BookResponse fields are populated appropriately. Add minimal DTO if missing.

**Target files:** F6 (CatalogBookDto.java), F7 (BookResponse.java)

**Planned snippet — minimal CatalogBookDto:**

```java
// F6: CatalogBookDto.java
public class CatalogBookDto {
    private String id;
    private String title;
    private String author;
    // getters/setters or Lombok annotations
}
```

**Planned snippet:** mapping already shown in Step 4.

**Review checkpoint:** Confirm field names/types match catalog service contract. If more fields are needed, add them but keep mapping minimal.

---

## Step 7 — Error handling and controller advice (notes)

**Goal:** Ensure 400/404 behaviour aligns with the project's centralised error handling. This story uses simple controller-level 400 responses for invalid input. For other validation patterns in the repo, adapt accordingly.

**Notes to implementer:**
- If the project prefers throwing a ValidationException and letting @ControllerAdvice map it into RFC7807 problem JSON, implement that pattern instead of returning ResponseEntity.badRequest() inline. The plan earlier asked to pick the pattern in Step 2.
- Ensure any thrown BusinessException maps to the correct HTTP status by the existing ControllerAdvice.

**Review checkpoint:** The final controller method returns 400 for blank or >256 author and 404 for empty result, and otherwise 200 with body.

---

## Step 8 — Unit tests (unit_testing phase — do NOT implement in coding phase)

**Goal:** Create unit tests in the unit_testing phase. The coding phase must NOT add tests.

**Required tests (to be written in next phase):**
- Happy path: mock CatalogClient.fetchBookByAuthor(author) -> Mono.just(CatalogBookDto) and assert controller's getBookByAuthor returns ResponseEntity.ok(BookResponse) with expected body using StepVerifier (wire controller or WebTestClient as appropriate).
- Not found: mock CatalogClient.fetchBookByAuthor(author) -> Mono.empty() and assert ResponseEntity.notFound().build() (404).
- Validation: author blank and author length > 256 produce 400 responses.

**Coverage note:** Aim for ≥90% line coverage on the changed class (controller). The unit_testing phase must craft tests accordingly.

---

## Step 9 — Manual validation against acceptance criteria (validation, second-to-last step)

**Goal:** Walk through every AC manually against the running application.

**Suggested checklist:**
1. Call the API path declared by the generated OpenAPI wiring (BooksApi.getBookByAuthor) for a valid author string — expect 200 and JSON body matching BookResponse.
2. Call with an author that the CatalogClient yields empty for — expect 404.
3. Call with blank author ("") — expect 400.
4. Call with >256 char author — expect 400.
5. Confirm the controller method delegates to BookService and that service uses CatalogClient.fetchBookByAuthor (review diffs).
6. Confirm no blocking calls (.block(), .collectList(), etc.) exist in changed files.

**Review checkpoint:** All ACs pass; if any fail, revert to the implementation steps to address the cause.

---

## Step 10 — Convention drift check (final code review before PR)

**Goal:** Review changed files for adherence to repo conventions (.github/copilot-instructions.md): constructor injection, private final fields, SLF4J logging patterns, no logging of sensitive data.

**Suggested checklist:**
- No field injection (@Autowired on fields) — use constructors.
- No blocking operators in Reactor chains.
- No direct WebClient instantiation per call; reuse shared bean where project patterns do so.
- Controller implements BooksApi.getBookByAuthor exactly; no new mapping annotations.
- Update import lists and add any required Lombok annotations consistent with project style.

**Review checkpoint:** Fix any convention drift before opening the PR.

---

## Done criteria

Before opening a PR (coding-phase PR), confirm:

- AC1–AC5 mapping: Controller method exists and returns Mono<ResponseEntity<BookResponse>>, delegating to BookService; BookService delegates to CatalogClient.fetchBookByAuthor; empty -> 404; blank or >256 -> 400.
- No changes made to generated OpenAPI modules or specs.
- No tests added in this phase (tests will be added in unit_testing phase).
- All changed files adhere to project conventions (constructor injection, Reactor usage, no blocking).
- Impacted Files table accurately reflects the files changed and package names.

---

## Implementation notes for coder (summary of exact edits to make in coding phase)

- Edit F4 (CatalogClient.java): add method signature

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

- Edit F5 (CatalogClientImpl.java): implement method using WebClient.get().uri("/catalog/books/by-author/{author}", author).retrieve().bodyToMono(CatalogBookDto.class)

- Edit F2 (BookService.java): add Mono<BookResponse> getBookByAuthor(String author);

- Edit F3 (BookServiceImpl.java): implement getBookByAuthor by calling catalogClient.fetchBookByAuthor(author).map(this::mapCatalogDtoToBookResponse)

- Edit F1 (BooksController.java): implement BooksApi.getBookByAuthor(String author) with controller-level validation (blank or >256 -> return Mono.just(ResponseEntity.badRequest().build())), otherwise delegate to bookService.getBookByAuthor(author) and map to ResponseEntity.ok(...) or defaultIfEmpty(ResponseEntity.notFound().build())

- Add or update F6 (CatalogBookDto.java) if DTO not yet present with minimal fields (id,title,author) and F7 (BookResponse.java) mapping aligned.

---

Plan written to: .harness/prompt-steps.md

To execute: open this file and follow Step 1 (inventory) to confirm exact file paths in your repo, then implement the planned code edits in the coding phase only under src/main/java. Do NOT add tests in this phase; unit tests belong to the unit_testing phase.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T06:19:21
- phase: coding
- approved impacted files: ['src/main/java/com/example/service/client/CatalogClient.java', 'src/main/java/com/example/service/client/impl/CatalogClientImpl.java', 'src/main/java/com/example/service/controller/BooksController.java', 'src/main/java/com/example/service/model/BookResponse.java', 'src/main/java/com/example/service/model/CatalogBookDto.java', 'src/main/java/com/example/service/service/BookService.java', 'src/main/java/com/example/service/service/impl/BookServiceImpl.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T06:19:21

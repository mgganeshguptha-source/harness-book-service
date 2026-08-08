# Plan for: BOOK-1: Add "get book by author" endpoint

**Source:** get-book-by-author-context-260808-121218.md + .github/copilot-instructions.md
**Stack:** Backend — Spring Boot (Reactive WebFlux / Reactor)
**Total steps:** 10
**Unresolved clarifications:** None

---

## Before you execute any step

1. Keep get-book-by-author-context-260808-121218.md in your Copilot Chat context throughout the plan.
2. .github/copilot-instructions.md is auto-loaded by Copilot when present in the repo.
3. Execute steps in order in one session when possible. If you restart, re-attach the context file and this plan file.

---

## Pre-flight

The plan assumes:

1. Backend uses Spring WebFlux with Reactor. Controllers return Reactor types (Mono/Flux) and use constructor injection. The generated `BooksApi` interface (from the `*-openapi-code` module) declares `getBookByAuthor(...)` and must be implemented by the controller (no controller-level mapping annotations to add).
2. Existing behaviour such as the `getBookById` reactive pattern is preserved; this new endpoint follows the same controller → service → client delegation pattern with no blocking calls.
3. Non-functional constraints (timeouts/retries/rate-limit/perf) are out of scope; do not add resilience or blocking code. Validation and simple request checks (blank / length) are the only guards added.

If any assumption is wrong, stop and correct the context before proceeding.

---

## Impacted Files (seed list — Step 1 will confirm exact files and add/replace paths)

| ID | Path (seed) | Role |
|----|-------------|------|
| F1 | src/main/java/**/controller/*BooksController.java | REST controller implementing BooksApi (implements getBookByAuthor) |
| F2 | src/main/java/**/service/BookService.java | Service layer — add getBookByAuthor delegating to CatalogClient |
| F3 | src/main/java/**/client/CatalogClient.java | Client interface — add fetchBookByAuthor(String) : Mono<CatalogBookDto> |
| F4 | src/main/java/**/client/CatalogClientImpl.java | Client implementation — call /catalog/books/by-author/{author} reactively |
| F5 | src/main/java/**/model/BookResponse.java | API response model (may already exist) |
| F6 | src/main/java/**/dto/CatalogBookDto.java | Catalog client DTO mapping |

> Note: Step 1 will confirm the real file paths and package names used in this repository (do not hard-code packages yet). If any of these files are missing or located under different packages, Step 1 will record the real paths and update the Impacted Files block.

---

## Step 1 — Inventory (confirm exact files and add non-code artifacts)

**Goal:** Produce a canonical list of files that will be changed in the coding phase. Confirm package names, existing controller class that implements `BooksApi`, and whether BookResponse/CatalogBookDto types exist or need creation.

**Suggested prompt:**

> Planning from: get-book-by-author-context-260808-121218.md
>
> Scan the codebase to confirm which files must be edited or created to implement a reactive "get book by author" endpoint following the existing `getBookById` pattern. Start from these candidate paths: (F1) src/main/java/**/controller/*BooksController.java, (F2) src/main/java/**/service/BookService.java, (F3) src/main/java/**/client/CatalogClient.java, (F4) src/main/java/**/client/CatalogClientImpl.java, (F5) src/main/java/**/model/BookResponse.java, (F6) src/main/java/**/dto/CatalogBookDto.java. For each candidate, return one confirmed path (absolute repo path), its role, and whether it will be edited or created. Also check for non-code files required (e.g., config, migration) and list them. Do NOT make code edits — only return the confirmed Impacted Files table. If multiple controllers implement BooksApi, list them and mark the one used by runtime (if ambiguous, ask).

**Review checkpoint:** Confirm the Impacted Files table above is replaced with exact paths and package names. Each file gets an ID (F1..). If any required file is missing, Step 1 should add it as "to create" (e.g., CatalogBookDto).

---

## Step 2 — Design the controller → service → client flow

**Goal:** Decide exact method signatures and where validation/response mapping lives (controller vs service).

**Suggested prompt:**

> Using the confirmed Impacted Files from Step 1 and the project conventions in .github/copilot-instructions.md, propose 2 options for where to perform input validation (blank/length) and mapping from CatalogBookDto → BookResponse: A) controller validates author and maps client DTO to API model before returning ResponseEntity; B) service validates and maps, controller only delegates and wraps ResponseEntity. For each option list pros/cons and recommend one that matches existing patterns (e.g., how getBookById is implemented). Do NOT write code yet — only recommend.

**Review checkpoint:** Pick the recommended option (likely: controller validates the path variable and returns Mono<ResponseEntity<BookResponse>>; service returns Mono<BookResponse> and CatalogClient returns Mono<CatalogBookDto>). Proceed only if the option aligns with existing getBookById pattern.

---

## Step 3 — Define exact method signatures and DTO mapping (text to include in coding phase)

**Goal:** Record exact method signatures and sample before/after snippets to be implemented in the coding phase.

**Planned changes (text only — do NOT implement in this phase):**

- CatalogClient (interface) — add method:

```java
// F3: CatalogClient.java (interface)
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

- CatalogClientImpl — implement network call (reactive WebClient) to catalog endpoint `/catalog/books/by-author/{author}` and map to CatalogBookDto. Example intended behaviour:

```java
// F4: CatalogClientImpl.java (implementation)
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}").build(author))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(CatalogBookDto.class);
}
```

- BookService — add method:

```java
// F2: BookService.java
public Mono<BookResponse> getBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
        .map(this::mapCatalogBookToBookResponse);
}
```

- Controller (implements BooksApi) — implement the interface method (no @GetMapping added) returning required type and validation checks:

```java
// F1: BooksController.java (implements BooksApi)
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    if (author == null || author.isBlank()) {
        return Mono.just(ResponseEntity.badRequest().build());
    }
    if (author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.getBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
}
```

Notes:
- Do NOT use `.block()` or `.collectList()` anywhere.
- The controller must implement the existing `BooksApi.getBookByAuthor` method (signature may include path param annotations in generated interface; implement exactly).

**Review checkpoint:** Confirm these signatures and snippets match the repository's types (e.g., exact BookResponse/CatalogBookDto class names and packages). If names differ, update the plan accordingly.

---

## Step 4 — Implementation plan (coding phase) — instructions for the engineer/PR

**Goal:** Implement the production code edits described in Step 3. (Coding phase only touches src/main/** files.)

**What to change (textual checklist — engineer to implement in coding phase):**

- Edit F3 (CatalogClient interface): add fetchBookByAuthor(String) : Mono<CatalogBookDto>.
- Edit F4 (CatalogClientImpl): implement fetchBookByAuthor using the existing reactive WebClient pattern used by fetchBook(String). Use the same base WebClient bean and map response to CatalogBookDto.
- Edit F2 (BookService): add getBookByAuthor(String) returning Mono<BookResponse>. Delegate to CatalogClient.fetchBookByAuthor and map DTO → BookResponse.
- Edit F1 (BooksController): implement the generated interface method getBookByAuthor(String) to perform author validation (blank/length) and then delegate to BookService; return Mono<ResponseEntity<BookResponse>>; map empty result to 404.
- If BookResponse or CatalogBookDto types don't exist, create F5/F6 in the appropriate package matching existing models. Keep fields identical to what getBookById uses where applicable.

**Before/After snippets (text only):**

Controller (before):

```java
// BooksController.java
// class implements BooksApi but getBookByAuthor not yet implemented
```

Controller (after):

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
  // validation + delegation as shown in Step 3 snippet
}
```

**Review checkpoint:** The coding phase must not change generated modules or OpenAPI specs. The implementation uses constructor injection and reuses the project's WebClient bean. No blocking calls are present.

---

## Step 5 — Static checks and linting (coding phase)

**Goal:** Ensure the code compiles and static checks pass before unit tests are added.

**Suggested checks to run after coding changes (CI will run these):**

- mvn -DskipTests clean package (or repo's build command) — ensure no compile errors
- Run project's linter/formatter rules (if configured)

**Review checkpoint:** Build succeeds and no formatting/linting issues introduced.

---

## Step 6 — Unit test design (unit_testing phase — NOT to implement in coding phase)

**Goal:** Describe tests to be added in the unit_testing phase so the test author can implement them.

**Tests to add (in unit_testing phase):**

1. BooksControllerTest — happy path
   - Mock BookService (or mock CatalogClient depending on test scope) to return a sample BookResponse. Use WebTestClient or call controller method directly. Use StepVerifier to subscribe and assert ResponseEntity status 200 and the body equals expected BookResponse.
2. BooksControllerTest — not found
   - Mock BookService to return Mono.empty() and assert 404.
3. BooksControllerTest — blank author and >256 char author
   - Call controller method with blank and long author values and assert 400 responses.
4. BookServiceTest — ensure service delegates to CatalogClient.fetchBookByAuthor and maps DTO to BookResponse. Use StepVerifier and mock CatalogClient to return Mono.of(CatalogBookDto).

**Coverage note:** Aim for ≥90% line coverage on the changed controller class (enforced by harness gate). The unit_testing phase will create these tests.

**Review checkpoint:** Tests use JUnit 5 + Reactor StepVerifier, mock CatalogClient (Mockito or project standard). No external calls.

---

## Step 7 — Manual validation (second-to-last step)

**Goal:** Manual verification that acceptance criteria are met against a running instance.

**Suggested manual checklist:**

- Start the service locally (or in harness env) and exercise GET /books/by-author/{author} with a known author that maps to a catalog book — expect 200 and JSON body matching BookResponse.
- Request with an author that the catalog client returns empty for — expect 404.
- Request with blank author (e.g., empty string) — expect 400.
- Request with author string length > 256 — expect 400.
- Confirm logs and MDC include correlationId but do not log PHI.

**Review checkpoint:** Mark each AC (AC1..AC7) pass/fail. If any AC fails, loop back to the appropriate implementation step.

---

## Step 8 — Convention drift review (last code-related step)

**Goal:** Ensure changed files follow .github/copilot-instructions.md and repository conventions (constructor injection, package placement, logging rules, no blocking, reactive patterns).

**Suggested prompt:**

> Review the files changed in this story (list the exact file paths from Step 1). For each file, list any drift from .github/copilot-instructions.md or repo conventions (constructor injection, logging, validation, reactive patterns). Do NOT change code — just list issues and suggested fixes.

**Review checkpoint:** All drifts either fixed or accepted with justification. No automated invasive fixes without human review.

---

## Done criteria (before opening PR)

- AC1: Controller implements BooksApi.getBookByAuthor and returns Mono<ResponseEntity<BookResponse>> (verified by code review).
- AC2: Controller delegates to BookService; code contains no blocking calls (.block(), .collectList()).
- AC3: BookService delegates to CatalogClient.fetchBookByAuthor — no direct HTTP calls in service/controller.
- AC4: Reactive types used consistently; WebClient usage in CatalogClientImpl is reactive.
- AC5: Blank author → 400; author length > 256 → 400; catalog empty → 404.
- AC6: Unit tests described above are implemented in unit_testing phase and show ≥90% line coverage on the controller class.
- AC7: No changes to generated `*-openapi-code` module or OpenAPI specs. Controller implements the pre-existing BooksApi interface method.
- Build passes and static checks/linting pass.

---

## Notes and constraints reminder

- THIS IS A PLANNING PHASE ONLY. Do NOT create or edit any .java, .kt, or test files in this phase. The coding phase implements production changes under src/main/**; the unit_testing phase implements tests under src/test/**.
- Do not touch generated modules or OpenAPI specs.
- No resilience (retry/timeouts/circuit-breakers) or DB changes in this story.

---

Plan written to .harness/prompt-steps.md — execute the next phase (coding) by implementing the production edits described under Step 4 and using the exact method signatures recorded in Step 3. Implement unit tests in the unit_testing phase as described in Step 6.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T12:15:25
- phase: coding
- approved impacted files: ['src/main/java/**/client/CatalogClient.java', 'src/main/java/**/client/CatalogClientImpl.java', 'src/main/java/**/controller/*BooksController.java', 'src/main/java/**/dto/CatalogBookDto.java', 'src/main/java/**/model/BookResponse.java', 'src/main/java/**/service/BookService.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T12:15:25

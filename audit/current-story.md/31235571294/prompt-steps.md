# Plan for: BOOK-1: Add "get book by author" endpoint

**Source:** get-book-by-author-context-260808-024307.md + .github/copilot-instructions.md
**Stack:** Backend — Spring Boot, WebFlux (Reactor) reactive stack
**Total steps:** 10
**Unresolved clarifications:** None

---

## Before you execute any step

1. Keep get-book-by-author-context-260808-024307.md in the copilot context throughout the plan.
2. .github/copilot-instructions.md is auto-loaded by Copilot when present. The plan follows the repo conventions (constructor injection, reactive patterns, no blocking).
3. Execute steps in order. Step 1 is an inventory step that confirms the exact impacted files and must be applied before any implementation step.

---

## Pre-flight

The plan assumes:

1. Backend uses Spring Boot with Spring WebFlux / Reactor and a single shared WebClient / reactive CatalogClient pattern is already present in the codebase.
2. Existing behaviour preserved: getBookById reactive pattern and BooksApi interface wiring remain unchanged; this story only implements the BooksApi.getBookByAuthor method already declared in the generated interface.
3. Non-functional constraints (timeouts/retries/circuit-breakers/rate-limiting) are out of scope; infra/resilience is handled elsewhere.

If any assumption is wrong, stop and update the context or the plan.

---

## Impacted Files (seed candidates — Step 1 will confirm and replace with exact files)

| ID | Path | Role |
|----|------|------|
| F1 | src/main/java/com/example/books/controller/BooksController.java | Controller implementing BooksApi (reactive) |
| F2 | src/main/java/com/example/books/service/BookService.java | Service layer (business orchestration) |
| F3 | src/main/java/com/example/books/client/CatalogClient.java | Catalog client interface (reactive) |
| F4 | src/main/java/com/example/books/client/CatalogClientImpl.java | Catalog client implementation (uses WebClient) |
| F5 | src/main/java/com/example/books/model/BookResponse.java | API response DTO (returned by controller) |
| F6 | src/main/java/com/example/books/client/dto/CatalogBookDto.java | DTO used by CatalogClient |
| F7 | src/main/java/com/example/books/mapper/BookMapper.java | Mapper between CatalogBookDto and BookResponse (optional) |

> Step 1 will confirm actual paths and update this table with canonical IDs. Do not renumber IDs if Step 1 adds files — append new IDs.

---

## Step 1 — Inventory: confirm exact files to change

Goal: Confirm the real file paths and any additional non-code artifacts required (e.g., config). Start with the seed listed in the Impacted Files block and add any genuinely required files (repository, dto, mapper, config). Do not edit files yet — only identify and confirm.

Suggested prompt:

> Planning from: get-book-by-author-context-260808-024307.md. Starting with these candidate files: F1..F7 (list above). Read each candidate file and reply with a confirmed Impacted Files table: ID | Path | Role. If a candidate path does not exist, propose the actual file path used in this repo. If additional files are required (e.g., an existing CatalogClient package, shared WebClient config, existing BookResponse DTO location, or mapper), add them to the list and assign new IDs. Do not propose edits yet.

Review checkpoint: Confirm the Impacted Files table lists the exact files in the repo (paths must exist). If the table contains unexpected files, stop and correct the seed before proceeding.

---

## Step 2 — Design: method signatures and composition contract

Goal: Finalise exact method signatures and return types to implement across layers, matching the reactive patterns and the story decisions.

Suggested prompt:

> Using the confirmed Impacted Files from Step 1, propose the precise method signatures to add/implement for this story. Include: 1) CatalogClient.fetchBookByAuthor(String author): Mono<CatalogBookDto>, 2) BookService.findBookByAuthor(String author): Mono<BookResponse>, and 3) BooksController (implements BooksApi).getBookByAuthor(String author): Mono<ResponseEntity<BookResponse>>. For each method, show the exact package-qualified signature and briefly explain responsibility and error mapping (400 for blank/too-long, 404 for empty result). Do not write implementations yet — only signatures and short rationale.

Review checkpoint: Confirm signatures match project conventions (package names, DTO types) and that BookResponse is the API DTO used elsewhere.

---

## Step 3 — Design: DTO / mapper choices

Goal: Decide whether to reuse an existing BookResponse DTO or add one, and whether to add a BookMapper or map inline in the service.

Suggested prompt:

> Given the confirmed Impacted Files, propose two options for mapping CatalogBookDto → BookResponse: (A) add a BookMapper (single-method component) and keep service logic thin, (B) map inline in BookService. For each option give pros/cons with respect to testability, reuse, and consistency with existing repository patterns (.github/copilot-instructions.md). Recommend one.

Review checkpoint: Pick the mapping option that matches existing code style. If the codebase already has mappers, choose option A; otherwise option B may be acceptable.

---

## Step 4 — Implementation plan (controller wiring) — coding-phase artifact description

Goal: Implement controller method that implements BooksApi.getBookByAuthor and delegates to BookService, returning Mono<ResponseEntity<BookResponse>>. This is a coding-phase change (production code only).

Describe intended edits (TEXT snippets only — do NOT create files in this phase):

Target file: F1 — Controller that implements BooksApi

Method signature to implement (example):

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    // validate author
    // delegate to BookService.findBookByAuthor(author)
    // map empty -> Mono.just(ResponseEntity.notFound().build())
    // map found -> ResponseEntity.ok(bookResponse)
}
```

Important constraints to follow in implementation:
- No blocking calls, no .block(), no .collectList().
- Validate blank or length > 256 early and return Mono.error(new ValidationException(...)) or Mono.just(ResponseEntity.badRequest().build()) depending on project patterns; prefer throwing a ValidationException that the global @ControllerAdvice maps to 400.
- Do not add controller-level @GetMapping annotation here — the controller already implements BooksApi which defines the mapping.

Review checkpoint: Implementation must compile and obey reactive patterns; controller must not introduce blocking.

---

## Step 5 — Implementation plan (service layer) — coding-phase artifact description

Goal: Add/find BookService.findBookByAuthor(String author): Mono<BookResponse> that orchestrates the CatalogClient call and mapping.

Describe intended edits (TEXT snippets only):

Target file: F2 — BookService

Method signature and pseudo-implementation:

```java
public Mono<BookResponse> findBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
        .map(catalogDto -> bookMapper.toBookResponse(catalogDto));
}
```

Notes:
- If CatalogClient returns Mono.empty(), do not map — let the controller translate empty to 404 (or service can return Mono.empty() and controller maps to 404). Decide per existing patterns and make consistent.
- Use constructor injection for CatalogClient and BookMapper.

Review checkpoint: Ensure service method is small, reactive, and delegates mapping to mapper if chosen.

---

## Step 6 — Implementation plan (CatalogClient) — coding-phase artifact description

Goal: Add the new CatalogClient.fetchBookByAuthor(String) signature to the client interface and implement it in CatalogClientImpl using existing WebClient patterns.

Describe intended edits (TEXT snippets only):

Target files: F3 (interface) and F4 (impl)

Interface addition:

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

Impl sketch (reactive WebClient call):

```java
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}").build(author))
        .retrieve()
        .bodyToMono(CatalogBookDto.class)
        .switchIfEmpty(Mono.empty());
}
```

Notes:
- Reuse the service's shared WebClient or the existing CatalogClientImpl wiring. Do not create a new WebClient bean unless the project pattern requires it.
- Do NOT add timeouts/retries/circuit-breakers here per story constraints.

Review checkpoint: Confirm the implementation uses the same WebClient instance/pattern as other CatalogClient methods (e.g., fetchBook) and follows reactive error mapping.

---

## Step 7 — Validation rules and error mapping

Goal: Ensure validation and error responses match ACs: blank or >256 → 400; no match → 404.

Suggested prompt (for implementer to follow when coding):

> Implement author validation in the controller or service consistent with project validation patterns (Bean Validation on controller DTOs or manual checks). If project uses @Validated / @NotBlank on method parameters, prefer that; otherwise, validate manually and throw ValidationException. Ensure ControllerAdvice maps ValidationException → 400 and Mono.empty() → 404.

Include example behaviour mapping (text):

- If author == null or author.trim().isEmpty() → 400 Bad Request
- If author.length() > 256 → 400 Bad Request
- If catalogClient.fetchBookByAuthor(author) yields empty → 404 Not Found

Review checkpoint: Confirm the project's global exception mapper will convert the chosen exception types into application/problem+json responses with correlationId as per error-handling conventions.

---

## Step 8 — Prepare for unit tests (unit_testing phase)

Goal: Make the produced code easy to unit-test in the next phase: keep classes package-visible where necessary, provide constructor injection, and keep mapping logic in a testable place (mapper or service). Do not write tests in coding phase.

Suggested prompt (for unit_testing phase):

> In the unit_testing phase add a JUnit5 test for the controller happy path using StepVerifier. Mock CatalogClient to return a CatalogBookDto; assert ResponseEntity.ok and body matches expected BookResponse. Also add tests for blank author (400) and not-found (404). Use Reactor StepVerifier to subscribe to the Mono returned by the controller method.

Review checkpoint: Ensure code is structured so tests can mock CatalogClient and verify reactive behaviour without starting the server.

---

## Step 9 — Manual validation against acceptance criteria (SECOND-TO-LAST step)

Goal: Manually verify the running application meets the Acceptance Criteria in the context file.

Suggested checklist to run after deploying the coding-phase artifact locally or to an integration environment:

1. Call the existing getBookById path to confirm no regression.
2. Call BooksApi.getBookByAuthor via the generated client wiring (or directly call the controller bean method) with a valid author that Catalog returns — expect 200 and a BookResponse body.
3. Call with an author that yields no result from Catalog — expect 404.
4. Call with blank author (`""` or whitespace) — expect 400.
5. Call with author length > 256 — expect 400.
6. Inspect logs at request entry/exit to confirm correlationId present and no PHI is logged.

If any check fails, revert to the corresponding implementation step.

Review checkpoint: All ACs (AC1–AC5) must pass in the running system before proceeding to the final convention check.

---

## Step 10 — Convention drift review (final code-related step)

Goal: Review all changed files for compliance with .github/copilot-instructions.md and repo Java conventions: constructor injection, private final dependencies, SLF4J logging with MDC correlationId, no blocking operations, and reactive WebClient usage.

Suggested prompt:

> Review the diffs for the files in the Impacted Files table. For each changed file, list any deviations from the repository conventions (constructor injection, private final fields, no .block(), no .collectList(), Bean Validation usage, ControllerAdvice-compatible exceptions). Do not auto-fix — list drift for the developer to correct.

Review checkpoint: No convention drift remains; any outstanding drift items must be fixed before opening a PR.

---

## Done criteria

Before opening a PR, confirm:

- The Impacted Files table is accurate and the code changes are limited to those files.
- AC1–AC5 verified in a running instance (manual validation step passed).
- Code uses reactive patterns throughout; no .block() or .collectList().
- Controller implements BooksApi.getBookByAuthor (no extra @RequestMapping annotations added).
- CatalogClient.fetchBookByAuthor exists and is used by BookService (no direct downstream HTTP calls from controller).
- Validation rules (blank / >256) produce 400; empty catalog result produces 404.
- Prepare unit tests in the unit_testing phase: controller happy-path test with StepVerifier and mocks; tests for 400/404.

---

## Appendix — exact method signatures & sample snippets (for implementer reference)

1) CatalogClient interface (F3) — add:

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

2) BookService (F2) — add:

```java
public Mono<BookResponse> findBookByAuthor(String author);
```

3) Controller (F1) — implement BooksApi method:

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    // validation (blank/length) → throw ValidationException
    return bookService.findBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
}
```

4) CatalogClientImpl (F4) — reactive WebClient call (sketch):

```java
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient.get()
        .uri("/catalog/books/by-author/{author}", author)
        .retrieve()
        .bodyToMono(CatalogBookDto.class);
}
```

Notes: adapt to existing error handling / bodyToMono usage already present in the class.

---

Plan written to .harness/prompt-steps.md

To execute: follow each step in order. Step 1 must be run first to confirm impacted files. Coding-phase changes described here are only textual plans and snippets; do not create or edit source files within this planning phase. Unit tests are produced in the unit_testing phase per the harness rules.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T02:47:10
- phase: coding
- approved impacted files: ['src/main/java/com/example/books/client/CatalogClient.java', 'src/main/java/com/example/books/client/CatalogClientImpl.java', 'src/main/java/com/example/books/client/dto/CatalogBookDto.java', 'src/main/java/com/example/books/controller/BooksController.java', 'src/main/java/com/example/books/mapper/BookMapper.java', 'src/main/java/com/example/books/model/BookResponse.java', 'src/main/java/com/example/books/service/BookService.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T02:47:10

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T02:49:31
- phase: coding
- approved impacted files: ['src/main/java/com/example/books/client/CatalogClient.java', 'src/main/java/com/example/books/client/CatalogClientImpl.java', 'src/main/java/com/example/books/client/dto/CatalogBookDto.java', 'src/main/java/com/example/books/controller/BooksController.java', 'src/main/java/com/example/books/mapper/BookMapper.java', 'src/main/java/com/example/books/model/BookResponse.java', 'src/main/java/com/example/books/service/BookService.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T02:49:31

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T02:55:42
- phase: coding
- approved impacted files: ['src/main/java/com/example/books/client/CatalogClient.java', 'src/main/java/com/example/books/client/CatalogClientImpl.java', 'src/main/java/com/example/books/client/dto/CatalogBookDto.java', 'src/main/java/com/example/books/controller/BooksController.java', 'src/main/java/com/example/books/mapper/BookMapper.java', 'src/main/java/com/example/books/model/BookResponse.java', 'src/main/java/com/example/books/service/BookService.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T02:55:42

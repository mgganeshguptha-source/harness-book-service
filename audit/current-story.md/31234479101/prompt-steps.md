# Plan for: Add "get book by author" endpoint (BOOK-1)

Source: .github/story-context-files/BOOK-1-context.md + .github/copilot-instructions.md
Stack: Backend — Spring WebFlux / Reactor (reactive)
Total steps: 9
Unresolved clarifications: None (story provided explicit decisions)

---

## Before you execute any step

1. Keep .github/story-context-files/BOOK-1-context.md in CI context. .github/copilot-instructions.md is auto-loaded by Copilot when present.
2. This plan is for the coding phase only. Do NOT create or edit tests or OpenAPI-generated modules in this phase. Implementation files listed below are *descriptions only*; do not write .java in this phase — implementation happens in the coding phase later under src/main/** as separate work.
3. Every reactive return must be non-blocking (no .block(), no .collectList()). Use constructor injection for new components.

---

## Pre-flight

Assumptions the plan makes:

1. Backend uses Spring Boot with WebFlux and Reactor. Project uses constructor injection and returns Reactor types (Mono/Flux) in controllers; existing BooksApi interface is part of a generated module and declares getBookByAuthor(String author).
2. Current behaviour preserved: existing getBookById pattern and error handling remain unchanged. This story implements a parallel reactive endpoint via the generated BooksApi method implementation.
3. Non-functional constraints (timeouts, retries, resilience) are out of scope — infrastructure provides them. Validation rules: blank or >256 chars → 400; no-match → 404.

---

## Impacted Files (seed list — Step 1 will confirm exact files)

| ID | Path | Role |
|----|------|------|
| F1 | src/main/java/.../controller/BooksController.java (or *-impl) | Controller implementing BooksApi.getBookByAuthor → returns Mono<ResponseEntity<BookResponse>> and delegates to BookService |
| F2 | src/main/java/.../service/BookService.java | Service interface/impl with reactive method to find book by author; transforms CatalogBookDto → BookResponse |
| F3 | src/main/java/.../client/CatalogClient.java | Catalog client interface — add fetchBookByAuthor(String author): Mono<CatalogBookDto> |
| F4 | src/main/java/.../client/CatalogClientImpl.java | Implementation calling catalog endpoint /catalog/books/by-author/{author} (reactive WebClient) |
| F5 | src/main/java/.../model/BookResponse.java | API response DTO (used by controller) — no schema changes expected; map from CatalogBookDto |
| F6 | src/main/java/.../client/dto/CatalogBookDto.java | Catalog client DTO (returned by CatalogClient) |

> Note: Step 1 (inventory) in the coding phase must confirm the exact package paths and file names used in this repo and add any missing files (e.g., a mapper component) as new IDs. Do not rename generated files.

---

## Step 1 — Inventory (confirm files)

Goal: Confirm exact paths and which files exist in the repo that will be edited in the coding phase. Ensure non-code artifacts required (none expected) are listed.

Suggested prompt (for the coding-phase implementer to run interactively):

> Planning from: .github/story-context-files/BOOK-1-context.md. List the concrete existing Java files that implement BooksApi, BookService, CatalogClient, CatalogClientImpl, CatalogBookDto and BookResponse in this project. For each file, return the full path and one-line role. Also list any needed new files (mapper, DTO) that do not exist yet. Do not make edits — only list existing files and propose any missing supporting files.

Review checkpoint: Confirm the Impacted Files table above is populated with exact repo paths and IDs assigned; add new IDs for any files discovered but not seeded.

---

## Step 2 — Design the reactive flow and signatures

Goal: Decide exact method signatures, null/blank handling, and mapping responsibilities.

Suggested prompt:

> Using the confirmed files from Step 1, propose exact method signatures and reactive flow for the new behaviour. Include: controller method signature (must implement BooksApi.getBookByAuthor), BookService method signature that controller calls, CatalogClient.fetchBookByAuthor signature, and mapping responsibility (where CatalogBookDto → BookResponse happens). Do not write implementation yet, only signatures and a short rationale.

Recommended signatures (to be implemented in coding phase):

- Controller (implements BooksApi):

```java
// implements generated BooksApi
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author)
```

- Service (BookService):

```java
public Mono<BookResponse> findBookByAuthor(String author);
```

- CatalogClient interface:

```java
public Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

Rationale: Controller composes validation → service call → map to ResponseEntity. Service calls CatalogClient.fetchBookByAuthor and maps CatalogBookDto → BookResponse. Empty Mono from client becomes Mono.empty() propagated to controller which converts to 404.

Review checkpoint: Confirm these signatures are consistent with existing patterns in the repo and with BooksApi declaration.

---

## Step 3 — Controller implementation plan (coding-phase description only)

Goal: Implement BooksApi.getBookByAuthor with reactive validation, delegation to BookService, and proper ResponseEntity mapping.

What to change (text description + code sketch):

- File: F1 — implement the existing interface method. Do NOT add @GetMapping annotations (interface provides mapping).
- Validation: if author == null || author.isBlank() || author.length() > 256 → return Mono.just(ResponseEntity.badRequest().build());
- Otherwise: call bookService.findBookByAuthor(author)
  - If service returns a BookResponse → wrap with ResponseEntity.ok(body)
  - If service returns Mono.empty() → return ResponseEntity.status(404).build()
- Do not .block() or collectList(); compose Reactor types only.

Before/after snippet (controller body sketch):

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    if (author == null || author.isBlank() || author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.findBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book))
        .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
}
```

Review checkpoint: Confirm method signature exactly matches BooksApi.getBookByAuthor and that no blocking calls are introduced.

---

## Step 4 — Service implementation plan (coding-phase description only)

Goal: Add reactive service method that calls CatalogClient and maps DTOs.

What to change:

- File: F2 — add method findBookByAuthor(String author): Mono<BookResponse> to service impl.
- Implementation sketch:

```java
public Mono<BookResponse> findBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
        .map(dto -> mapToBookResponse(dto)); // mapToBookResponse is a private mapper method
}
```

- If CatalogClient returns multiple results in future, current decision is to use first element if applicable inside the client — story states client returns at most one.

Review checkpoint: Confirm mapping responsibilities and that no downstream HTTP calls are made from the service other than via CatalogClient.

---

## Step 5 — CatalogClient change plan (coding-phase description only)

Goal: Add CatalogClient.fetchBookByAuthor(String) to interface and implement reactive HTTP call in CatalogClientImpl.

What to change:

- File: F3 — add interface method:

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

- File: F4 — implement a WebClient GET to /catalog/books/by-author/{author}, returning Mono<CatalogBookDto>. Use path variable encoding via WebClient's uri builder; do not manually decode or block.

Sketch for implementation in CatalogClientImpl:

```java
return webClient.get()
  .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}").build(author))
  .accept(MediaType.APPLICATION_JSON)
  .retrieve()
  .bodyToMono(CatalogBookDto.class);
```

Review checkpoint: Confirm WebClient is reused instance (existing pattern) and timeouts/resilience are not added here per story constraints.

---

## Step 6 — Validation & error mapping specifics

Goal: Define how controller returns 400/404 and mapping of empty Mono.

Details (to implement in controller/service):

- Blank or null or >256 chars → controller returns 400 Bad Request immediately (Mono.just(ResponseEntity.badRequest().build())).
- Catalog client returns empty Mono → controller returns 404 Not Found.
- On unexpected exceptions from downstream, let global @ControllerAdvice map exceptions to 5xx per repo conventions (do not catch-and-convert here).

Review checkpoint: Confirm behavior matches AC5 and AC6 in context.md.

---

## Step 7 — Unit testing plan (unit_testing phase — do not create tests in coding phase)

Goal: Describe tests to be authored in the next phase.

Tests to add under src/test/java:

- Test class for controller (BooksControllerTest):
  - Happy path: mock BookService.findBookByAuthor → Mono.just(BookResponse). Use StepVerifier to subscribe to controller.getBookByAuthor("Author Name") and assert ResponseEntity.ok(body) and body fields.
  - Not found path: mock BookService.findBookByAuthor → Mono.empty(); assert 404 response.
  - Validation path: blank author and >256 chars → assert 400 response synchronously (StepVerifier or direct subscribe).

- Service unit test for BookService: mock CatalogClient.fetchBookByAuthor to return Mono.just(CatalogBookDto) and assert mapping to BookResponse.

Coverage note: aim for ≥ 90% line coverage on the changed class (enforced by harness after unit tests run).

Review checkpoint: Ensure tests use Reactor StepVerifier and do not hit external services (mock CatalogClient). Do not add integration tests in this story.

---

## Step 8 — Convention & security review (coding-phase checklist)

Goal: Verify added code follows repo conventions and OWASP/HIPAA guardrails.

Checklist for implementer before PR:

- Use constructor injection (private final) for BookService and CatalogClient.
- Use SLF4J for any logging; do not log author or any PHI-like data.
- Do not add @GetMapping to controller (implement BooksApi method only).
- Do not introduce blocking calls (.block(), .collectList()).
- Ensure CatalogClientImpl reuses existing WebClient bean and uses uri builder (no string concatenation) to avoid injection risks.
- No secrets or keys added.

Review checkpoint: Confirm these checks pass locally and CI linters are happy.

---

## Step 9 — Done criteria (map back to acceptance criteria)

Before opening a PR, confirm:

- AC1: Controller implements BooksApi.getBookByAuthor and returns Mono<ResponseEntity<BookResponse>> (code snippet implemented in F1).
- AC2: Controller delegates to BookService; no blocking operations found (grep for .block(), .collectList()).
- AC3: Service calls CatalogClient.fetchBookByAuthor; no direct downstream HTTP calls elsewhere.
- AC4: Reactor types used end-to-end (controller → service → client). No blocking.
- AC5: Blank author and >256 length result in 400; empty catalog result → 404.
- AC6: Unit test (in unit_testing phase) covers happy path with StepVerifier and asserts body; changed classes meet ≥90% line coverage as validated by harness.
- AC7: No OpenAPI spec or generated module files edited; controller implements existing BooksApi method only.

---

## Notes / Risks

- Ensure the exact BooksApi method signature in the generated module is confirmed in Step 1; mismatch will cause compilation errors. If packages differ, adapt imports accordingly.
- If CatalogClient previously uses different DTO shapes or wrappers (e.g., enveloped responses), update mapping accordingly in the coding phase (add new ID in Impacted Files for mapper if needed).

---

Plan created for coding-phase implementer. Execute Step 1 to confirm exact file paths and then implement steps 2–5 in code under src/main/java only. Do NOT add or modify tests in this phase.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T02:18:38
- phase: coding
- approved impacted files: ['src/main/java/.../client/CatalogClient.java', 'src/main/java/.../client/CatalogClientImpl.java', 'src/main/java/.../client/dto/CatalogBookDto.java', 'src/main/java/.../controller/BooksController.java (or *-impl)', 'src/main/java/.../model/BookResponse.java', 'src/main/java/.../service/BookService.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T02:18:38

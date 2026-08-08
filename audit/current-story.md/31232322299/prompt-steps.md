# Plan for: BOOK-1 - Add get book by author endpoint

**Source:** get-book-by-author-context-260808-011834.md + .github/copilot-instructions.md (defaults)
**Stack:** Backend — Spring Boot, WebFlux (reactive), Reactor
**Total steps:** 10
**Unresolved clarifications:** None (story provided explicit decisions)

---

## Before you execute any step

1. Keep get-book-by-author-context-260808-011834.md in your CI context while following this plan.
2. This plan targets the coding phase only: edit production sources under src/main/**. Do NOT create or edit test files in this phase. Unit tests are created in the unit_testing phase.
3. Do not modify the generated OpenAPI module or spec (sample-book-service-openapi-code). Controllers must implement the existing BooksApi interface method.
4. Follow repository conventions: constructor injection, SLF4J/Lombok logging patterns, no blocking calls (.block(), .collectList()).

---

## Pre-flight

Assumptions the plan makes:

1. Backend uses Spring WebFlux and Reactor; reactive types (Mono) are used end-to-end. No blocking code will be introduced.
2. Behaviour preserved: getBookById patterns (controller implements BooksApi, service delegates to CatalogClient) are preserved and reused for getBookByAuthor.
3. Non-functional constraints: performance/resilience (timeouts/retries/circuit-breakers) are out-of-scope and owned by infra.

If any assumption is wrong, stop and amend the context file before implementing.

---

## Impacted Files

| ID | Path | Role |
|----|------|------|
| F1 | sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java | GENERATED API interface (already declares getBookByAuthor) — DO NOT EDIT |
| F2 | sample-book-service-application/src/main/java/com/example/book/controller/BookController.java | Controller implementing BooksApi — add implementation of getBookByAuthor (validation + delegation) |
| F3 | sample-book-service-application/src/main/java/com/example/book/service/BookService.java | Service interface — add getBookByAuthor signature |
| F4 | sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java | Service impl — implement getBookByAuthor delegating to CatalogClient |
| F5 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java | Common-layer client interface — add fetchBookByAuthor signature |
| F6 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java | Client impl — call downstream /catalog/books/by-author/{author} reactively |
| F7 | sample-book-service-openapi-code/src/main/java/com/example/book/model/BookResponse.java | GENERATED DTO — used as response, no edits expected |
| F8 | sample-book-service-application/src/main/java/com/example/book/model/CatalogBookDto.java | Downstream DTO — used by CatalogClient, no edits expected |

> Notes: F1 and F7 are generated; do NOT edit. All other files live in the application module and are editable in the coding phase.

---

## Step 1 — Inventory (seeded)

Goal: Confirm the file set above is correct and sufficient for the change (coding phase). In CI mode, the plan seeds the impacted file list; implementation steps below reference the IDs.

Suggested prompt (for human-run Copilot Chat; CI has already seeded):

> Review these candidate files: F2, F3, F4, F5, F6, F8. Add any genuinely required files (config, beans) and remove any that are not impacted. Do not modify generated files F1 or F7. Return a one-line role for each confirmed file.

Review checkpoint: Confirm the Impacted Files block above matches the repository layout. If additional files (e.g., WebClient bean qualifier config) are required, add them to the Impacted Files block before proceeding.

---

## Step 2 — Design: validation & mapping responsibilities

Goal: Decide where to validate the incoming author parameter and where to map CatalogBookDto → BookResponse.

Decision (recommended):
- Controller (F2) performs parameter validation (blank, length > 256) and returns 400 synchronously (wrapped in Mono). This keeps DTO-level validation close to the API boundary and aligns with existing getBookById patterns.
- Service (F4) performs mapping from CatalogBookDto → BookResponse and composes Reactor types. CatalogClient (F5/F6) returns Mono<CatalogBookDto>.

Suggested prompt:

> Propose pros/cons of placing validation in the controller vs service for getBookByAuthor. Recommend the approach used by getBookById in the repo and proceed accordingly. Do not implement code yet.

Review checkpoint: Confirm controller-based validation is chosen. If the codebase favors @Validated + @Size on path variables, adjust prompts accordingly. This plan proceeds with explicit runtime validation in the controller to avoid changing generated interfaces.

---

## Step 3 — Add CatalogClient.fetchBookByAuthor (interface) (edit F5)

Goal: Add a new interface method to CatalogClient to support author lookup.

Change description (text + snippet):

- File: F5 (sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java)
- Add method signature:

```java
// new in CatalogClient
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

Review checkpoint: The interface compiles and contains the new method. No behaviour implemented yet.

---

## Step 4 — Implement CatalogClientImpl.fetchBookByAuthor (edit F6)

Goal: Implement the client call to the catalog common-layer endpoint reactively.

Change description (text + snippet):

- File: F6 (sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java)
- Implement method (use existing webClient pattern used by fetchBook):

```java
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    log.info("Calling catalog common-layer for author:{}", author);
    return webClient.get()
            .uri("/catalog/books/by-author/{author}", author)
            .retrieve()
            .bodyToMono(CatalogBookDto.class);
}
```

Notes: keep reactive; do not add retries/timeouts here (infra owns resilience). Do not block.

Review checkpoint: Method implemented, follows same error handling pattern as fetchBook (no catch here, let ControllerAdvice handle errors).

---

## Step 5 — Add BookService.getBookByAuthor signature (edit F3)

Goal: Expand the BookService interface to include the author-based lookup.

Change description (text + snippet):

- File: F3 (sample-book-service-application/src/main/java/com/example/book/service/BookService.java)
- Add method signature:

```java
Mono<BookResponse> getBookByAuthor(String author);
```

Review checkpoint: Interface compiled with new method.

---

## Step 6 — Implement BookServiceImpl.getBookByAuthor (edit F4)

Goal: Implement the service method to call CatalogClient.fetchBookByAuthor and map to BookResponse.

Change description (text + snippet):

- File: F4 (sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java)
- Implement method:

```java
@Override
public Mono<BookResponse> getBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
            .map(dto -> new BookResponse(dto.getBookId(), dto.getTitle(), dto.getAuthor()));
}
```

Notes: If CatalogClient returns empty Mono, this method returns Mono.empty() (controller handles 404). No blocking. Mapping is identical to getBook by id mapping.

Review checkpoint: Service method implemented, compiles, and uses reactive composition only.

---

## Step 7 — Implement BooksApi.getBookByAuthor in BookController (edit F2)

Goal: Add the controller method implementation that validates input, delegates to BookService, and composes the Mono<ResponseEntity<BookResponse>> result.

Change description (text + snippet):

- File: F2 (sample-book-service-application/src/main/java/com/example/book/controller/BookController.java)
- Add method (matches BooksApi signature). Implementation must:
  - Validate author: if null/blank or length > 256 → return Mono.just(ResponseEntity.badRequest().build())
  - Delegate to bookService.getBookByAuthor(author)
  - Map present result to ResponseEntity.ok(body)
  - On empty → ResponseEntity.notFound().build()

Example snippet to add inside BookController:

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    log.info("Request received getBookByAuthor author:{}", author);

    if (author == null || author.trim().isEmpty() || author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.getBookByAuthor(author)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

Notes:
- Use defaultIfEmpty to convert Mono.empty() → 404 response.
- Do not perform case transformation here; story states exact case-insensitive match and expects catalog endpoint to honor it OR the client to return matching result. If additional comparison is required, implement it reactively in the service (avoid blocking).

Review checkpoint: Controller compiles, implements BooksApi.getBookByAuthor, uses constructor injection, does not add new @RequestMapping (interface defines mappings). No blocking calls present.

---

## Step 8 — Small ancillary checks (config/beans)

Goal: Ensure a WebClient bean named "catalogWebClient" exists and is configured; if not, add a TODO and reference to the existing WebClient config.

Suggested changes (if necessary):
- Confirm existence of a WebClient bean with @Qualifier("catalogWebClient"). If missing, register it in existing config — do not change external properties in this story. If a new bean is required, add it in the same module's config package.

Review checkpoint: CatalogClientImpl constructor injection resolves. If unresolved, fix DI in coding phase before running unit tests.

---

## Step 9 — Convention drift review

Goal: After implementing the changes, review modified files against repo conventions (constructor injection, logging with placeholders, no blocking, no PHI logging).

Suggested checklist:
- All new fields are private final and injected via constructor.
- Logging uses {} placeholders and does not log request bodies or PHI.
- No .block() or .collectList() anywhere in changed files.
- Controller implements BooksApi.getBookByAuthor (no mapping annotations added to controller class).

Review checkpoint: Fix any drift found here before marking coding complete.

---

## Step 10 — Done criteria (coding phase) + next-phase notes

Coding-phase acceptance (before opening PR):
- F2, F3, F4, F5, F6 modified as described. F1 and F7 unchanged.
- Code compiles locally (mvn -DskipTests package or equivalent). No new warnings from static analysis relevant to the change.
- No blocking calls introduced.
- Controller implements BooksApi.getBookByAuthor and returns Mono<ResponseEntity<BookResponse>>.
- Input validation implemented in controller: blank or >256 → 400.
- Empty catalog result yields 404 via defaultIfEmpty or equivalent.

Unit_testing phase (to run after coding phase completes):
- Add unit tests for BookController.getBookByAuthor using StepVerifier and a mocked BookService/CatalogClient. Tests should include:
  - Happy path: catalog returns a CatalogBookDto → controller returns 200 + BookResponse body (assert fields).
  - Not found: catalog returns Mono.empty() → controller returns 404.
  - Validation: blank author and >256 author → controller returns 400.
- Ensure test coverage meets the harness gate (changed class ≥ 90% lines covered). Use Mockito to mock CatalogClient for service-layer tests and BookService for controller tests. Do not perform integration or downstream HTTP calls in unit tests.

---

## Implementation snippets (before/after) — coding-phase edits only

F5 (CatalogClient) — before:

```java
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId);
}
```

F5 — after (add method):

```java
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId);
    Mono<CatalogBookDto> fetchBookByAuthor(String author);
}
```

F6 (CatalogClientImpl) — before: (existing fetchBook method present)

F6 — after (add method):

```java
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    log.info("Calling catalog common-layer for author:{}", author);
    return webClient.get()
            .uri("/catalog/books/by-author/{author}", author)
            .retrieve()
            .bodyToMono(CatalogBookDto.class);
}
```

F3 (BookService) — before:

```java
public interface BookService {
    Mono<BookResponse> getBook(String bookId);
}
```

F3 — after:

```java
public interface BookService {
    Mono<BookResponse> getBook(String bookId);
    Mono<BookResponse> getBookByAuthor(String author);
}
```

F4 (BookServiceImpl) — before contains getBook implementation.

F4 — after (add method):

```java
@Override
public Mono<BookResponse> getBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
            .map(dto -> new BookResponse(dto.getBookId(), dto.getTitle(), dto.getAuthor()));
}
```

F2 (BookController) — before contains getBookById implementation.

F2 — after (add method):

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    log.info("Request received getBookByAuthor author:{}", author);

    if (author == null || author.trim().isEmpty() || author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.getBookByAuthor(author)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

---

## Final notes / Rationale

- All reactive composition stays with Mono; no blocking is introduced.
- Validation is explicit and simple to avoid touching generated API model annotations.
- Catalog client mirrors existing fetchBook implementation and calls the designated endpoint.
- The controller implements the generated BooksApi method (per AC7) and therefore does not add mapping annotations at class-level.

Plan written to .harness/prompt-steps.md

To execute: follow the numbered steps above during the coding phase. After coding-phase PR is created, run the unit_testing phase (separate plan) to add StepVerifier-based tests as described.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T01:23:48
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/CatalogBookDto.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/model/BookResponse.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-08-08T01:23:48

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T01:24:58
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/CatalogBookDto.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/model/BookResponse.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-08-08T01:24:58

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T01:28:23
- phase: coding
- approved impacted files: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/CatalogBookDto.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java', 'sample-book-service-openapi-code/src/main/java/com/example/book/model/BookResponse.java']
- actually touched: (none)
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-08-08T01:28:23

# Plan for: BOOK-1: Add "get book by author" endpoint

**Source:** newest file in .github/story-context-files/ (CI mode)
**Stack:** Backend — Spring WebFlux (Reactive), Spring Boot, Reactor
**Total steps:** 9
**Unresolved clarifications:** None (all clarifications resolved in the story)

---

## Before you execute any step

1. The context file is the newest file in .github/story-context-files/ (CI mode). Keep that file available for reference while implementing.
2. Follow repository conventions: constructor injection, no blocking calls, controller should implement the generated `BooksApi.getBookByAuthor` method (do not add controller-level mappings), centralised error handling remains in place.
3. Validation rules from the story: blank author → 400; author length >256 → 400. Behavior preservation: existing `getBookById` reactive pattern and use of `CatalogClient` are preserved.

---

## Pre-flight

This plan assumes:

1. Backend uses Spring WebFlux (Reactive) with Reactor and a shared `WebClient`-based `CatalogClient` already present. New code must compose Monos and not block.
2. The `BooksApi` interface already declares `getBookByAuthor(String author)` and is part of the generated module; controller must implement that method (no new mapping annotations).
3. Non-functional constraints (timeouts/retries/circuit-breakers) are out-of-scope for this story and must not be added.

If any assumption is incorrect, stop and update the context/spec before continuing.

---

## Impacted Files (seed candidates — Step 1 will confirm exact paths used in this repo)

| ID | Path | Role |
|----|------|------|
| F1 | src/main/java/.../controller/BooksController.java | Controller implementing BooksApi.getBookByAuthor — validate input, delegate to BookService, compose ResponseEntity in reactive style |
| F2 | src/main/java/.../service/BookService.java | Service interface — declare new method to fetch by author |
| F3 | src/main/java/.../service/impl/BookServiceImpl.java | Service implementation — call CatalogClient.fetchBookByAuthor and map DTO → BookResponse |
| F4 | src/main/java/.../client/CatalogClient.java | Catalog client interface — add fetchBookByAuthor signature returning Mono<CatalogBookDto> |
| F5 | src/main/java/.../client/impl/CatalogClientImpl.java | Catalog client implementation — implement fetchBookByAuthor using reactive WebClient to GET /catalog/books/by-author/{author} |
| F6 | src/main/java/.../model/CatalogBookDto.java | (if mapping type exists) DTO from catalog; used as return type of CatalogClient.fetchBookByAuthor — likely already exists |
| F7 | src/main/java/.../model/BookResponse.java | API response model — mapping target; likely already exists |

> Notes: Step 1 (inventory) in the coding phase must confirm exact package paths and whether DTO/response classes already exist. If a file already exists under a different package, add that file as a new ID rather than renaming.

---

## Step 1 — Inventory (seed & confirm files)

**Goal:** Confirm the exact files the change will touch (controller, service, client, DTOs). Ensure no generated `*-openapi-code` module files are modified.

**Suggested prompt (for the coding-phase developer):**

> Planning from: newest file in .github/story-context-files/ (BOOK-1). Start with these candidate files: `BooksController`, `BookService`, `BookServiceImpl`, `CatalogClient`, `CatalogClientImpl`, `CatalogBookDto`, `BookResponse`. Read the repository to confirm the exact file paths and package names for each candidate and add any genuinely required files (e.g., mappers, exceptions, validators, or config). Return a table with one-line role descriptions and the confirmed file paths. Do not propose edits — only list the confirmed impacted files and any missing non-code artifacts (DB migrations, config) required by this story.

**Review checkpoint:** Confirm the Impacted Files table above is populated with exact paths and package names by replacing the seed entries with the confirmed files (IDs F1..F7). If additional files (mapper, util) are needed, add them as new IDs.

---

## Step 2 — Design the reactive flow and validation

**Goal:** Agree on method signatures and reactive flow across layers (controller → service → catalog client) and how empty results / validation are handled.

**Suggested prompt:**

> Given the confirmed file set from Step 1, propose explicit method signatures for: (a) `CatalogClient.fetchBookByAuthor(String author): Mono<CatalogBookDto>`, (b) `BookService.getBookByAuthor(String author): Mono<BookResponse>`, and (c) `BooksApi.getBookByAuthor(String author): Mono<ResponseEntity<BookResponse>>` (controller implementation). Describe how the controller will validate the `author` path variable (blank / length > 256) and how an empty Mono from the client will map to a 404 response in the controller without blocking. Present a concise Reactive composition for the controller (one-liner chain) and mention error handling expectations (use existing ControllerAdvice). Do not implement code yet.

**Review checkpoint:** Confirm signatures match repository conventions and reactive composition is acceptable. If the team prefers service to return `Mono<ResponseEntity<...>>` instead of `Mono<BookResponse>`, decide and document which pattern to follow. This plan assumes `BookService` returns `Mono<BookResponse>` and the controller builds the ResponseEntity.

---

## Step 3 — Add CatalogClient API (interface) (F4)

**Goal:** Add the new method signature to the CatalogClient interface.

**Intended edit (coding phase — description only):**

File: F4 — CatalogClient.java

Add signature:

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

Rationale: mirrors existing `fetchBook(String bookId)` method. No blocking, returns Mono. Keep Javadoc comment referencing `/catalog/books/by-author/{author}` endpoint.

**Review checkpoint:** Interface compiles and matches existing CatalogClient method style (same package, same reactive type). No new dependencies.

---

## Step 4 — Implement CatalogClientImpl.fetchBookByAuthor (F5)

**Goal:** Implement the reactive outbound call to the catalog service using the shared WebClient; return Mono<CatalogBookDto>.

**Intended edit (coding phase — description/snippet):**

File: F5 — CatalogClientImpl.java

Add method implementation (example snippet):

```java
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder.path("/catalog/books/by-author/{author}").build(author))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(CatalogBookDto.class);
}
```

Notes: use existing `webClient` bean, propagate `X-Correlation-Id` via exchange filter already in place. Do NOT add timeouts/retries here.

**Review checkpoint:** Method is non-blocking, uses bodyToMono(CatalogBookDto.class), and follows existing CatalogClient implementation patterns.

---

## Step 5 — Service layer: declare and implement getBookByAuthor (F2, F3)

**Goal:** Add method to service interface and implement in BookServiceImpl to call CatalogClient and map CatalogBookDto → BookResponse.

**Intended edits (coding phase — description/snippet):**

File: F2 — BookService.java (interface)

```java
Mono<BookResponse> getBookByAuthor(String author);
```

File: F3 — BookServiceImpl.java

```java
@Override
public Mono<BookResponse> getBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
        .map(this::toBookResponse);
}

private BookResponse toBookResponse(CatalogBookDto dto) {
    // map fields: id, title, author, publishedDate, etc.
    return BookResponse.builder()
        .id(dto.getId())
        .title(dto.getTitle())
        .author(dto.getAuthor())
        // ... other mappings
        .build();
}
```

Notes: If `CatalogClient` returns empty Mono, service propagates empty Mono. Keep mapping logic simple and unit-testable.

**Review checkpoint:** Service compiles, uses constructor-injected CatalogClient, no blocking, mapping covers required fields and handles null-safe mapping.

---

## Step 6 — Controller implementation: implement BooksApi.getBookByAuthor (F1)

**Goal:** Implement the generated API method in the controller, validate `author`, delegate to BookService, and return proper ResponseEntity in a reactive chain.

**Intended edit (coding phase — description/snippet):**

File: F1 — BooksController.java

Key method implementation (snippet):

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    if (author == null || author.isBlank() || author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.getBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book))
        .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

Notes:
- Do NOT add `@GetMapping` or other mapping annotations — implement the interface method only.
- Validation uses simple checks per story. Alternatively, if project prefers an exception-based validation, throw a ValidationException and let ControllerAdvice convert to 400. The snippet uses an immediate 400 ResponseEntity to keep behavior explicit and avoid changing error-handling wiring.

**Review checkpoint:** Method is non-blocking, composes Monos correctly, and returns 400/404/200 as per ACs.

---

## Step 7 — Unit testing (unit_testing phase only)

**Goal:** Add unit tests (in the unit_testing phase) covering happy path and validation cases using JUnit5 + StepVerifier and mocking CatalogClient.

**Test cases to add (describe — do NOT implement in coding phase):**
- Happy path: mock CatalogClient.fetchBookByAuthor(author) → Mono.just(dto). Call controller.getBookByAuthor(author) and StepVerifier expectNextMatches(ResponseEntity.ok(bookResponse)) then verifyComplete.
- Not found: mock fetchBookByAuthor → Mono.empty(). Expect ResponseEntity.notFound() from controller.
- Blank author: call controller.getBookByAuthor("") and expect ResponseEntity.badRequest().
- Too long author (>256): expect badRequest.

**Review checkpoint (after unit_testing phase):** Tests run locally; coverage for the changed class ≥ 90%.

---

## Step 8 — Conformance and convention drift check (final coding-phase review)

**Goal:** Verify changed files follow repo conventions: constructor injection, SLF4J logging (no PHI), no blocking, reactive WebClient usage, no modifications to generated OpenAPI module.

**Suggested checklist for reviewer:**
- No use of .block(), .collectList(), or other blocking calls.
- Constructor injection used for CatalogClient and BookService dependencies.
- No new public endpoints annotated on controller — only implementation of BooksApi.
- No changes to generated `*-openapi-code` module.

**Review checkpoint:** Fix any drift flagged before opening PR.

---

## Step 9 — Done criteria (pre-PR checklist)

Before opening a PR, confirm:

- AC1: Controller implements `BooksApi.getBookByAuthor` returning `Mono<ResponseEntity<BookResponse>>` (see F1 snippet).
- AC2: Controller delegates to BookService; no blocking calls appear in controller or service.
- AC3: BookService uses `CatalogClient.fetchBookByAuthor(...)` — no direct downstream HTTP calls from service/controller (CatalogClientImpl uses WebClient).
- AC4: Reactive patterns are followed across client → service → controller.
- AC5: Blank author and length checks return 400; empty client result returns 404.
- AC6: Unit tests (in unit_testing phase) cover happy path and validation; target ≥ 90% line coverage for changed class.
- AC7: No edits to generated OpenAPI code or specs; controller implements the generated interface only.

---

## Minimal rationale / implementation notes for reviewers

- Prefer mapping in the service layer to keep controller thin. The controller's responsibility should be validation and mapping the Mono<BookResponse> → Mono<ResponseEntity<BookResponse>>.
- Keep CatalogClient implementations consistent with existing client methods (same error handling and JSON mapping semantics).
- If the codebase uses a centralized validation approach for path variables, convert the simple checks in the controller to that style — but ensure the semantics remain: blank or >256 → 400.

---

## Example before/after method signatures (text only — do NOT edit generated modules)

Before (controller stub from generated module):

```java
// In generated BooksApi interface (already present) - do not edit
Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author);
```

After (controller implementation — to be added in F1):

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    if (author == null || author.isBlank() || author.length() > 256) {
        return Mono.just(ResponseEntity.badRequest().build());
    }

    return bookService.getBookByAuthor(author)
        .map(book -> ResponseEntity.ok(book))
        .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

CatalogClient interface (add in F4):

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

CatalogClientImpl (F5) example call:

```java
return webClient.get()
    .uri("/catalog/books/by-author/{author}", author)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(CatalogBookDto.class);
```

Service interface & impl (F2 / F3):

```java
// BookService
Mono<BookResponse> getBookByAuthor(String author);

// BookServiceImpl
public Mono<BookResponse> getBookByAuthor(String author) {
    return catalogClient.fetchBookByAuthor(author)
        .map(this::toBookResponse);
}
```

---

Plan file written to the harness location for the coding phase. Execute these steps in the coding phase only; unit tests belong to the unit_testing phase. Do NOT create or modify any Java or test files in this planning step — this file only describes the intended edits for the coding phase.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-08-08T04:14:03
- phase: coding
- approved impacted files: ['src/main/java/.../client/CatalogClient.java', 'src/main/java/.../client/impl/CatalogClientImpl.java', 'src/main/java/.../controller/BooksController.java', 'src/main/java/.../model/BookResponse.java', 'src/main/java/.../model/CatalogBookDto.java', 'src/main/java/.../service/BookService.java', 'src/main/java/.../service/impl/BookServiceImpl.java']
- actually touched: ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['sample-book-service-application/src/main/java/com/example/book/controller/BookController.java', 'sample-book-service-application/src/main/java/com/example/book/model/BookResponse.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookService.java', 'sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java', 'sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-08-08T04:14:03

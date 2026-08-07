# Plan for: BOOK-1: Add "get book by author" endpoint

**Source:** get-book-by-author-context-260807-125607.md + .github/copilot-instructions.md
**Stack:** backend — Spring Boot WebFlux (reactive), Spring Reactor, generated OpenAPI support
**Total steps:** 10
**Unresolved clarifications:** None (all explicit in the context file)

---

## Before you execute any step

1. Keep get-book-by-author-context-260807-125607.md in your Copilot Chat context throughout the plan.
2. .github/copilot-instructions.md is auto-loaded by Copilot when present in the repo. You do not need to attach it manually.
3. Execute steps in one Copilot Chat session when possible. If you restart, paste the full plan back into the chat alongside the context file.
4. This is PLANNING ONLY. Do NOT create, edit, or write any .java source or test files in this phase. The implementation will be performed in a later phase. The only file produced now is this plan: .harness/prompt-steps.md

---

## Pre-flight

Assumptions the plan makes (confirm before implementing):

1. Backend uses Spring WebFlux with Reactor and WebClient; the project already contains a reactive BookController, BookService, and CatalogClient (sample implementations exist under sample-book-service-application and sample-book-service-openapi-code).
2. Behaviour preservation: existing getBookById endpoint is preserved; no behavioural changes to existing endpoints other than adding the new by-author endpoint.
3. Non-functional handling: timeouts/retries/circuit-breakers are out of scope; infrastructure will handle resilience. No new resilience or blocking calls will be added.

If any assumption is wrong, update the context file and re-generate the plan before coding.

---

## Impacted Files (seed list)

| ID | Path | Role |
|----|------|------|
| F1 | sample-book-service-application/src/main/java/com/example/book/controller/BookController.java | REST controller implementing generated BooksApi — add new endpoint wiring to BookService |
| F2 | sample-book-service-application/src/main/java/com/example/book/service/BookService.java | Service interface — add method signature for lookup by author |
| F3 | sample-book-service-application/src/main/java/com/example/book/service/BookServiceImpl.java | Service implementation — delegate to CatalogClient and map DTO to BookResponse |
| F4 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClient.java | Common-layer client interface — add fetchBookByAuthor signature |
| F5 | sample-book-service-application/src/main/java/com/example/book/webclient/CatalogClientImpl.java | Client implementation — call /catalog/books/by-author/{author} with WebClient, return Mono<CatalogBookDto> |
| F6 | sample-book-service-openapi-code/src/main/java/com/example/book/model/BookResponse.java | Generated response DTO (used by controller API). No structural change expected, but ensure mapping aligns. |
| F7 | sample-book-service-application/src/test/java/com/example/book/controller/BookControllerByAuthorTest.java | Unit test (new) — controller-level test using StepVerifier and mocked BookService/CatalogClient as appropriate |
| F8 | sample-book-service-openapi-code/src/main/java/com/example/book/api/BooksApi.java | Generated OpenAPI interface — will need an operation for GET /books/by-author/{author} (update spec and regenerate code) |

> Note: Step 1 (inventory) will confirm whether F7/F8 exist already or need to be created/updated by the OpenAPI generator. Do not hand-edit generated code except to add wiring if the project convention allows; prefer updating the OpenAPI spec and re-generating.

---

## Step 1 — Inventory and confirm exact files to change

**Goal:** Verify the seed file set is accurate, discover any additional required files (e.g., CatalogBookDto class, OpenAPI YAML/JSON location, controller test packages), and confirm where generated code is produced so the spec change is done in the correct place.

**Suggested prompt:**

> Planning from: get-book-by-author-context-260807-125607.md. Start with these candidate files: F1..F8 (list above). Read each file and report back a one-line role plus whether the file is generated (do not propose edits yet). Also check for: the CatalogBookDto type, the OpenAPI spec file (e.g., openapi.yaml or openapi.yml under sample-book-service-openapi-code or resources), and existing controller tests that can be extended. If any candidate path does not exist, add the actual path. Return a final confirmed Impacted Files table with stable IDs.

**Review checkpoint:** Confirm the final Impacted Files table lists every file that must be edited or created (including OpenAPI spec location) and that generated code paths are identified (so the spec can be updated rather than hand-editing generated files).

---

## Step 2 — Design the service & client contract (small decision)

**Goal:** Decide exact method signatures and DTO classes to use, ensuring reactive types and mapping choices align with existing patterns.

**Suggested prompt:**

> Using get-book-by-author-context-260807-125607.md and the confirmed Impacted Files table, propose the exact method signatures to add to the existing interfaces and the mapping plan. Provide 2 short options if mapping needs a new DTO vs reusing CatalogBookDto → BookResponse mapping. Prefer an approach that mirrors existing fetchBook(String) usage. Do not modify any files yet — only recommend signatures and mapping code snippets.

**Recommended decision (from context):**
- Add to CatalogClient: Mono<CatalogBookDto> fetchBookByAuthor(String author)
- Add to BookService: Mono<BookResponse> getBookByAuthor(String author)
- BookServiceImpl will call catalogClient.fetchBookByAuthor(author) and map CatalogBookDto → BookResponse using the same mapping used in getBook(bookId).

**Review checkpoint:** Confirm chosen signatures and mapping approach. If generated DTOs differ, plan small adapter mapping in the service layer.

---

## Step 3 — Spec change (OpenAPI)

**Goal:** Add operation GET /books/by-author/{author} to the OpenAPI spec so the generated BooksApi includes the new method. Do not manually edit generated BooksApi except where project conventions permit — prefer updating the spec and regenerating.

**Suggested prompt:**

> Update the OpenAPI spec (path: confirm in Step 1) to add a new operation:
>
> GET /books/by-author/{author}
> parameters:
>  - name: author
>    in: path
>    required: true
>    schema:
>      type: string
> responses:
>  200: content application/json -> BookResponse schema
>  400: application/problem+json
>  404: application/problem+json
>
> Include operationId: getBookByAuthor and summary/description per the context file. After updating the spec, regenerate the API code (the harness's existing generator step) and report which files changed (BooksApi interface and model classes if any).

**Review checkpoint:** Confirm the OpenAPI regeneration produces a BooksApi#getBookByAuthor method with signature matching: Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) and that generated code location is F8.

---

## Step 4 — Add CatalogClient.fetchBookByAuthor (interface + impl)

**Goal:** Add the new method to the common-layer client interface and implement it in the CatalogClientImpl using WebClient; fully reactive and non-blocking.

**Intended edits (text snippets, do not apply in this phase):**

CatalogClient.java — add method signature:

```java
Mono<CatalogBookDto> fetchBookByAuthor(String author);
```

CatalogClientImpl.java — add implementation mirroring fetchBook(String):

```java
@Override
public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
    log.info("Calling catalog common-layer for author:{}", author);
    return webClient.get()
            .uri("/catalog/books/by-author/{author}", author)
            .retrieve()
            .bodyToMono(CatalogBookDto.class)
            .map(dto -> dto); // no further mapping here
}
```

**Review checkpoint:** Implementation uses WebClient, returns Mono<CatalogBookDto>, and contains no blocking calls. Confirm error handling is consistent with existing client methods.

---

## Step 5 — Add BookService method and implementation

**Goal:** Add a new service method getBookByAuthor(String) to the BookService interface and implement it in BookServiceImpl by delegating to CatalogClient.fetchBookByAuthor and mapping to BookResponse.

**Intended edits (text snippets):**

BookService.java (interface):

```java
Mono<BookResponse> getBookByAuthor(String author);
```

BookServiceImpl.java (implementation outline):

```java
@Override
public Mono<BookResponse> getBookByAuthor(String author) {
    if (author == null || author.trim().isEmpty()) {
        return Mono.error(new ValidationException("author must not be blank")); // map to 400 by ControllerAdvice
    }
    return catalogClient.fetchBookByAuthor(author)
            .map(dto -> new BookResponse(dto.getBookId(), dto.getTitle(), dto.getAuthor()));
}
```

Notes:
- The story requires blank author → 400; implement validation in the service by returning a ValidationException (or alternatively validate in controller); follow existing project pattern for where ValidationException is thrown so the global @ControllerAdvice maps it to 400.
- If CatalogClient returns empty → propagate empty Mono so controller can translate to 404. The code above will result in empty(); do not convert to errors here except for blank-author.

**Review checkpoint:** Confirm no .block(), returns Mono<BookResponse>, blank author handling chosen consistent with existing validation patterns.

---

## Step 6 — Controller wiring: GET /books/by-author/{author}

**Goal:** Add an endpoint implementation that delegates to BookService.getBookByAuthor and returns Mono<ResponseEntity<BookResponse>>. Follow existing BooksApi+Controller conventions.

**Intended edits (text snippets):**

BooksApi (generated) method signature expected after regen:

```java
Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author);
```

BookController.java — add implementation (example):

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    log.info("Request received getBookByAuthor author:{}", author);
    if (author == null || author.trim().isEmpty()) {
        return Mono.just(ResponseEntity.badRequest().build());
    }
    return bookService.getBookByAuthor(author)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

Notes:
- This approach returns 400 immediately for blank author (as required by AC5).
- For empty result from service, returns 404 via defaultIfEmpty.
- Alternatively, throw a NotFoundException in the service and let ControllerAdvice map it to 404; both are acceptable — pick the project-consistent pattern discovered in Step 1.

**Review checkpoint:** Verify the controller returns Mono<ResponseEntity<BookResponse>>, performs no blocking, and delegates to BookService.

---

## Step 7 — Unit tests (controller-level) using StepVerifier

**Goal:** Add unit tests that cover the happy path and boundary cases for the new controller method; mock BookService or CatalogClient as appropriate. Target ≥90% line coverage on the changed controller class per AC6.

**Test cases to implement (outline):**
1. Happy path: CatalogClient/BookService returns a CatalogBookDto -> expect 200 OK and response body matches BookResponse fields. Use StepVerifier on controller.getBookByAuthor("Some Author") to assert ResponseEntity has status 200 and body non-null with expected author.
2. No match: BookService returns Mono.empty() -> expect 404 Not Found.
3. Blank author: controller called with "" (or whitespace) -> expect 400 Bad Request.

**Suggested test skeleton (pseudo):**

- Use JUnit5, Mockito, Reactor Test (StepVerifier).
- Mock BookService bean, instantiate BookController with the mock, call the controller method directly.

**Review checkpoint:** Tests assert response status and body where applicable and exercise the controller branch logic to achieve ≥90% line coverage for the controller class.

---

## Step 8 — OpenAPI regeneration and verification

**Goal:** After spec update (Step 3), regenerate API code and confirm BooksApi contains getBookByAuthor; ensure the generated method signature matches the controller implementation and no hand-editing of generated code is required.

**Suggested prompt:**

> Regenerate the OpenAPI client/server code using the project's existing generation command (maven/gradle plugin). Report any mismatches between generated BooksApi#getBookByAuthor signature and the controller method; if there are mismatches, propose minimal reconciliations (prefer updating the spec).

**Review checkpoint:** BooksApi#getBookByAuthor exists and signature is Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author). If generator produces different types, reconcile in the spec until signatures match.

---

## Step 9 — Convention drift check

**Goal:** Ensure all changed/added code follows repository conventions (.github/copilot-instructions.md), including constructor injection, logging, package placement, and reactive patterns (no blocking). Do not automatically fix; list any drift for manual correction.

**Suggested prompt:**

> Review the diffs for the files in the Impacted Files table and list any deviations from .github/copilot-instructions.md and the repo Java instructions (constructor injection, private final fields, slf4j/lombok usage, no .block()). Provide suggested fixes for each deviation.

**Review checkpoint:** All drift items either accepted (with a rationale) or scheduled for manual correction.

---

## Step 10 — Manual validation against Acceptance Criteria (validation runbook)

**Goal:** Manually verify ACs against a running instance after implementation.

**Suggested prompt (for checklist generation):**

> For each acceptance criterion in get-book-by-author-context-260807-125607.md, produce a one-line manual test that can be executed against the running service (HTTP requests using curl or a REST client). Include expected HTTP status and example assertions.

Manual checklist (examples):
- AC1 (endpoint exists): curl -v GET /api/v1/books/by-author/"J.K.%20Rowling" → expect 200 (happy path) or 404; response Content-Type application/json; step verifier earlier validated body shape.
- AC5 (blank author): curl -v GET /api/v1/books/by-author/"" (or request with empty path segment) → expect 400 Bad Request.
- AC6 (unit coverage): Ensure unit tests pass and coverage gate ≥90% for the controller class.

**Review checkpoint:** Run these checks against a local dev instance and mark pass/fail per AC. If any AC fails, return to the specific step that implemented the failing behaviour.

---

## Done criteria (before opening PR)

- Implementations created for the files listed in the confirmed Impacted Files block (F1..F8) with no blocking calls and reactive composition used everywhere.
- OpenAPI spec updated and regenerated so BooksApi contains getBookByAuthor with the required signature.
- Controller returns Mono<ResponseEntity<BookResponse>> and delegates to BookService (no blocking). Blank author → 400, no match → 404.
- Unit tests added (StepVerifier) covering happy path, blank author, and not-found; controller class line coverage ≥ 90%.
- Conventions check (constructor injection, logging, no .block()) passes or items are documented for follow-up.
- All tests pass locally. Manual validation checklist executed and all ACs pass on a running instance.

---

## Implementation notes & exact signatures (copy into code-phase prompts)

- CatalogClient (F4) — add:

```java
Mono<com.example.book.model.CatalogBookDto> fetchBookByAuthor(String author);
```

- CatalogClientImpl (F5) — implement using WebClient:

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

- BookService (F2) — add:

```java
Mono<com.example.book.model.BookResponse> getBookByAuthor(String author);
```

- BookServiceImpl (F3) — add:

```java
@Override
public Mono<BookResponse> getBookByAuthor(String author) {
    if (author == null || author.trim().isEmpty()) {
        return Mono.error(new com.example.book.exception.ValidationException("author must not be blank"));
    }
    return catalogClient.fetchBookByAuthor(author)
            .map(dto -> new BookResponse(dto.getBookId(), dto.getTitle(), dto.getAuthor()));
}
```

- BooksApi (F8) / OpenAPI spec — add operationId `getBookByAuthor` producing 200/400/404 and returning BookResponse on 200.

- BookController (F1) — add implementation in controller that calls bookService.getBookByAuthor(author) and maps to ResponseEntity:

```java
@Override
public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
    log.info("Request received getBookByAuthor author:{}", author);
    if (author == null || author.trim().isEmpty()) {
        return Mono.just(ResponseEntity.badRequest().build());
    }
    return bookService.getBookByAuthor(author)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

- Tests (F7) — controller-level tests using StepVerifier. Mock BookService to return Mono.just(book) for happy path, Mono.empty() for not-found, and assert 400 for blank author.

---

## Notes & risks

- The plan assumes CatalogClient's downstream endpoint exists and returns a single matching book for exact case-insensitive author matches. If the downstream returns multiple items, the implementation will pick the first as per the explicit decision in the context file.
- ValidationException type and global exception mapping must exist in the project; Step 1 will confirm the preferred place to raise validation errors (controller vs service) so behaviour is consistent with global ControllerAdvice.
- OpenAPI regeneration is preferred to hand-editing generated interfaces; CI pipelines often enforce generated-code checks — verify generator commands in the project (maven/gradle) in Step 1.

---

Plan written to .harness/prompt-steps.md from context file: get-book-by-author-context-260807-125607.md

To execute: follow each step in order. For steps that ask to modify files, paste the suggested code snippets into the real files and run the project's test suite and OpenAPI generator as described.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>

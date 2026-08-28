---
applyTo: "**/src/test/**/*.java"
---

# Reactive Testing Conventions (Reactor Test / StepVerifier)

These rules apply to tests for BCBSM backend microservices. The code under test is reactive
(Spring WebFlux / Reactor), so tests assert on `Mono`/`Flux` streams rather than plain return
values. Stack: JUnit 5 + Mockito, with `StepVerifier` (Reactor Test) for reactive assertions.

## Framework

- Use **JUnit 5** (`@Test`, `@ExtendWith(MockitoExtension.class)`) and **Mockito** for mocking.
  Both are provided via `mem-starter-parent`.
- Assert reactive types with **`StepVerifier`** (from `reactor-test`). Do **not** call
  `.block()` to get a value and then assert on it — verify the stream.

## Asserting on Mono / Flux

- Wrap the publisher under test in `StepVerifier.create(...)` and assert its signals:
  - Value(s): `.expectNext(expected)` / `.expectNextMatches(...)` / `.expectNextCount(n)`.
  - Completion: `.verifyComplete()`.
  - Empty (e.g. a validation filter that returns empty `Mono`): `.expectComplete()` with no
    `expectNext`, or `.expectNextCount(0).verifyComplete()`.
  - Errors: `.expectError(SomeException.class)` / `.expectErrorMatches(...)` then `.verify()`.
- Every `StepVerifier` chain must end in a terminal verify call (`.verifyComplete()`,
  `.verify()`, etc.) — a StepVerifier that is never verified does not actually run.

## Mocking reactive dependencies

- When stubbing a dependency that returns a reactive type, return a real publisher, not null:
  `when(commonService.getX(any())).thenReturn(Mono.just(response))` or `Mono.empty()` /
  `Flux.fromIterable(...)` as appropriate.
- For error paths, stub with `Mono.error(new SomeException(...))`.

## Testing a WebClient-based client

**Never mock the `WebClient` fluent chain.** Stubbing `webClient.get()` →
`.uri(...)` → `.retrieve()` → `.bodyToMono(...)` with Mockito does not work
reliably: each step returns a different generic wildcard type
(`RequestHeadersUriSpec<?>`, `RequestHeadersSpec<?>`), so `thenReturn(...)` fails
to compile with "no suitable method found" / "reference to uri is ambiguous", and
overloaded `uri(String, Map)` vs `uri(String, Function)` is genuinely ambiguous to
the compiler. This is a dead end — it does not compile, so it cannot be fixed by
adjusting the stubs.

Choose the right level instead:

**1. Testing a SERVICE that depends on a client interface** — mock the *interface*,
never the WebClient behind it:

```java
@Mock CatalogClient catalogClient;   // the interface, not WebClient
when(catalogClient.fetchBookByAuthor("Tolkien"))
        .thenReturn(Mono.just(new CatalogBookDto(...)));
```
This is the common case and where most coverage should come from.

**2. Testing the client IMPLEMENTATION itself** — build a real `WebClient` over a
stub exchange function. No mocking of the fluent API, no new dependency, no port:

```java
ExchangeFunction exchange = request -> Mono.just(
        ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"title\":\"The Hobbit\",\"author\":\"Tolkien\"}")
                .build());

WebClient webClient = WebClient.builder()
        .baseUrl("http://catalog")
        .exchangeFunction(exchange)
        .build();

CatalogClientImpl client = new CatalogClientImpl(webClient);

StepVerifier.create(client.fetchBookByAuthor("Tolkien"))
        .assertNext(dto -> assertThat(dto.author()).isEqualTo("Tolkien"))
        .verifyComplete();
```
Vary the stubbed `ClientResponse` for the 404/empty and 5xx branches. If assertions
on the outgoing request are needed, capture `request.url()` / `request.headers()`
inside the exchange function.

`MockWebServer` (okhttp3) is also acceptable where a real socket is wanted, but it
adds a test dependency and a port — prefer `exchangeFunction` unless there is a
reason.

## What to cover

- Cover the reactive branches explicitly: the happy path (value emitted + completes), the
  empty path (e.g. validation filter returns empty `Mono`), and the error path
  (dependency emits `Mono.error`). Reactive coverage is easy to under-count if the empty and
  error branches are not each driven by their own test.
- Assert the composed behaviour (the operators in the chain), not just that the dependency was
  called — verify what the chain emits.

## Do not

- Do not `.block()` in a test to unwrap a value for assertion — use `StepVerifier`.
- Do not leave a `StepVerifier` without a terminal `.verify*()` call.
- Do not stub a reactive method to return `null` — return `Mono.empty()` / `Mono.error(...)`.
- Do not assert only on mock interactions when the point of the test is the emitted result.
- Do not mock the `WebClient` fluent chain (`webClient.get()`, `.uri()`, `.retrieve()`,
  `.bodyToMono()`). It does not compile — mock the client interface, or build a real
  `WebClient` with `.exchangeFunction(...)`. See "Testing a WebClient-based client".

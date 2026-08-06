# sample-book-service

A **neutral, BCBSM-shaped sample microservice** for practising the build harness.
No client code or naming — a simple "book" domain — but the same structure a real
service uses, so a harness run here rehearses real work.

## Shape (mirrors a real service)

```
sample-book-service/                      # one repo = one microservice
├── pom.xml                               # root aggregator POM (packaging: pom)
├── sample-book-service-application/      # module 1: hand-written reactive code
│   └── src/main/java/com/example/book/
│       ├── controller/  BookController   # implements generated BooksApi, returns Mono
│       ├── service/     BookServiceImpl  # business logic, reactive
│       ├── webclient/   CatalogClientImpl# calls the common layer via WebClient
│       └── config/      WebClientConfig
└── sample-book-service-openapi-code/     # module 2: generated stubs (BooksApi, DTOs)
```

Key traits it shares with BCBSM services:
- **Multi-module Maven** — the harness auto-detects the `-application` module and
  write-excludes the `-openapi-code` module.
- **Reactive** (WebFlux / Reactor) — `Mono` returns, no `.block()`, no `.collectList()`.
- **No database** — the service calls a common service layer over reactive WebClient
  (here a stubbed catalog endpoint), it does not touch a DB.
- **JaCoCo** emits `jacoco.csv` for the coverage gate.

## Run the harness against it

1. Push this repo to GitHub; enable **Actions**.
2. Add repo secret **`COPILOT_GITHUB_TOKEN`** (fine-grained PAT, Copilot-licensed
   seat, Contents RW + Pull requests write).
3. Copy the **copilot-toolkit** skills/instructions into `.github/`.
4. Actions → **Harness** → Run workflow → enter a feature id (e.g. `BOOK-1`).

`.harness/config.yaml` and `.github/workflows/harness.yml` (the caller) are already
here. Sample stories are in `stories/`.

## First experiments

- Run **BOOK-1** (a clean, well-specified story) → expect a green run + PR.
- Then try a deliberately vague story → watch the harness **halt** at the
  clarification gate. The halt is the point: it never ships an unclear change.

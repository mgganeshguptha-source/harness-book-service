VERDICT: PASS

[NOTE]: Controller validation uses `author.trim().isEmpty()` to catch blank/whitespace-only authors. The null-check-first short-circuit logic (author == null ||) ensures no NPE when author is null.

[NOTE]: The `switchIfEmpty()` in the controller is defensive; in practice, the catalog client either emits a CatalogBookDto or throws an exception on non-2xx status. However, this defensive pattern is safe and documents the intended 404 behavior.

[NOTE]: HTTP errors from the downstream catalog service (4xx, 5xx) are not caught in the client/service layer, per the story's stated design: "error handling left to the shared advice". This is correct per the reactive-webclient instructions.

[NOTE]: Logging of author name follows the existing pattern (e.g., `log.info("Request received getBookById bookId:{}", bookId)`). In the book-service context, author is not PHI and is consistent with the repository's logging approach.

[NOTE]: WebClient URI parameter substitution in CatalogClientImpl (`uri("/catalog/books/by-author/{author}", author)`) is correct and safe; Spring's WebClient automatically URL-encodes the author parameter, so no manual encoding is needed.

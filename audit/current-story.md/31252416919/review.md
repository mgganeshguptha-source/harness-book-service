VERDICT: CHANGES_REQUESTED

[ISSUE]: **CatalogClientImpl.fetchBookByAuthor() breaks method-chaining pattern** (line 40-45)
The implementation assigns intermediate WebClient types to local variables (`uriSpec`, `requestSpec`, `responseSpec`) instead of using fluent chaining like the existing `fetchBook()` method. This reduces readability and violates the established code pattern in the same class.

**Current (inconsistent):**
```java
WebClient.RequestHeadersUriSpec<?> uriSpec = webClient.get();
WebClient.RequestHeadersSpec<?> requestSpec = uriSpec.uri("/catalog/books/by-author/{author}", author);
WebClient.ResponseSpec responseSpec = requestSpec.retrieve();
return responseSpec.bodyToMono(CatalogBookDto.class);
```

**Should match fetchBook() pattern:**
```java
return webClient.get()
        .uri("/catalog/books/by-author/{author}", author)
        .retrieve()
        .bodyToMono(CatalogBookDto.class);
```

---

## Summary

**Strengths:**
- ✅ Reactive patterns correct across all layers (returns Mono, no blocking)
- ✅ Validation logic correct: null, blank, and >256-char author all return 400
- ✅ Delegation chain correct: controller → service → CatalogClient
- ✅ 404 handling via `switchIfEmpty()` is reactive and proper
- ✅ Logging consistent with existing code (Request/Call patterns match)
- ✅ No blocking operations (no `.block()`, no `.collectList()`)
- ✅ Constructor injection used throughout
- ✅ No direct HTTP calls in service layer

**Issue:**
- ❌ **Code consistency**: CatalogClientImpl violates the fluent chaining pattern established by the existing `fetchBook()` method on the same class. This is a readability and maintainability concern.

**Recommendation:**
Simplify the `fetchBookByAuthor()` implementation in CatalogClientImpl to use fluent method chaining, matching the style of `fetchBook()`. This is the only production-code problem; all functional and reactive requirements are met.

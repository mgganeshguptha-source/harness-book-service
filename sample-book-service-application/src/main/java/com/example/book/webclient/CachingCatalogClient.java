package com.example.book.webclient;

import com.example.book.model.CatalogBookDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory reactive cache wrapper around the real CatalogClient implementation.
 * - Caches fetchBook(bookId) results only
 * - Time-based expiry: 10 minutes
 * - Capacity: 1000 entries, LRU eviction
 * - Does not cache empty results or errors
 * - Non-blocking from reactive perspective (no .block()). Uses short synchronized
 *   sections for map access.
 */
@Service
@Primary
@Log4j2
public class CachingCatalogClient implements CatalogClient {

    // expiry and capacity per story
    private static final long EXPIRY_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final int MAX_ENTRIES = 1000;

    private final CatalogClientImpl delegate;

    // access-ordered LinkedHashMap for LRU behaviour. Small synchronized sections guard it.
    private final Map<String, CacheEntry> cache = new LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public CachingCatalogClient(CatalogClientImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<CatalogBookDto> fetchBook(String bookId) {
        // quick check for cached, non-expired entry
        CacheEntry entry;
        synchronized (cache) {
            entry = cache.get(bookId);
            if (entry != null) {
                if (!isExpired(entry)) {
                    log.debug("Cache hit for bookId={}", bookId);
                    return Mono.just(entry.value);
                } else {
                    // expired — remove and treat as miss
                    log.debug("Cache expired for bookId={}", bookId);
                    cache.remove(bookId);
                }
            }
        }

        log.debug("Cache miss for bookId={}", bookId);
        // Miss — delegate to underlying client and populate cache on successful non-empty result
        return delegate.fetchBook(bookId)
                .doOnNext(dto -> {
                    if (dto != null) {
                        synchronized (cache) {
                            cache.put(bookId, new CacheEntry(dto, System.currentTimeMillis()));
                        }
                        log.debug("Cached bookId={}", bookId);
                    }
                });
    }

    @Override
    public Mono<CatalogBookDto> fetchBookByAuthor(String author) {
        // Out of scope for caching per story
        return delegate.fetchBookByAuthor(author);
    }

    private boolean isExpired(CacheEntry e) {
        return System.currentTimeMillis() - e.timestampMillis >= EXPIRY_MILLIS;
    }

    private static class CacheEntry {
        final CatalogBookDto value;
        final long timestampMillis;

        CacheEntry(CatalogBookDto value, long timestampMillis) {
            this.value = value;
            this.timestampMillis = timestampMillis;
        }
    }
}

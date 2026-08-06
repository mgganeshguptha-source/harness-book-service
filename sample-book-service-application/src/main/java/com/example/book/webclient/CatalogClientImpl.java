package com.example.book.webclient;

import com.example.book.model.CatalogBookDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Reactive common-layer client. Mirrors BCBSM conventions:
 *  - @Service, WebClient injected by @Qualifier
 *  - returns Mono; never blocks; no service-level retry (infra owns retries)
 *  - error handling left to the shared advice (not caught-and-wrapped here)
 *
 * NOTE: the downstream base URL points at a stubbed "common layer". Replace the
 * WebClient bean config to point at a real service. No DB access here by design —
 * the service calls the common layer, it does not touch a database.
 */
@Service
@Log4j2
public class CatalogClientImpl implements CatalogClient {

    private final WebClient webClient;

    public CatalogClientImpl(@Qualifier("catalogWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<CatalogBookDto> fetchBook(String bookId) {
        log.info("Calling catalog common-layer for bookId:{}", bookId);
        return webClient.get()
                .uri("/catalog/books/{id}", bookId)
                .retrieve()
                .bodyToMono(CatalogBookDto.class);
    }
}

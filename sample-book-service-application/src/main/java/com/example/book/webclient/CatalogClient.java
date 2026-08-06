package com.example.book.webclient;

import com.example.book.model.CatalogBookDto;
import reactor.core.publisher.Mono;

/**
 * Common-layer client interface. In BCBSM the impl extends AbstractCommonService
 * and calls the common service layer over reactive WebClient. Here it stands in
 * for that downstream call.
 */
public interface CatalogClient {
    Mono<CatalogBookDto> fetchBook(String bookId);
}

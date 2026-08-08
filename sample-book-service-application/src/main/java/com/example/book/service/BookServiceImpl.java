package com.example.book.service;

import com.example.book.model.BookResponse;
import com.example.book.webclient.CatalogClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Business logic. Calls the common-layer client and maps the result.
 * Fully reactive; no .block(), no .collectList().
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class BookServiceImpl implements BookService {

    private final CatalogClient catalogClient;

    @Override
    public Mono<BookResponse> getBook(String bookId) {
        return catalogClient.fetchBook(bookId)
                .map(dto -> new BookResponse(dto.getBookId(), dto.getTitle(), dto.getAuthor()));
    }

    @Override
    public Mono<BookResponse> getBookByAuthor(String author) {
        return catalogClient.fetchBookByAuthor(author)
                .map(dto -> new BookResponse(dto.getBookId(), dto.getTitle(), dto.getAuthor()));
    }
}

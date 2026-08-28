package com.example.book.controller;

import com.example.book.api.BooksApi;
import com.example.book.model.BookResponse;
import com.example.book.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Reactive controller. Mirrors BCBSM conventions:
 *  - @RestController + @RequiredArgsConstructor + @Log4j2
 *  - implements the generated BooksApi interface (no @RequestMapping here)
 *  - returns Mono<...>; never blocks
 *  - delegates to the service via .map/.flatMap
 */
@RestController
@RequiredArgsConstructor
@Log4j2
public class BookController implements BooksApi {

    private final BookService bookService;

    @Override
    public Mono<ResponseEntity<BookResponse>> getBookById(String bookId) {
        log.info("Request received getBookById bookId:{}", bookId);
        return bookService.getBook(bookId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<BookResponse>> getBookByAuthor(String author) {
        log.info("Request received getBookByAuthor author:{}", author);

        if (!StringUtils.hasText(author) || author.length() > 256) {
            log.warn("Invalid author parameter, blank or too long (len={}):{}", author == null ? 0 : author.length(), author);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return bookService.getBookByAuthor(author)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}

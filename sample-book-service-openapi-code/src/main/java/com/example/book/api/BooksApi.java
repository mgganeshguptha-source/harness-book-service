package com.example.book.api;

import com.example.book.model.BookResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

/**
 * GENERATED API INTERFACE (simulated).
 * In a real BCBSM service this is generated from the OpenAPI spec into the
 * -openapi-code module. Controllers implement it; they do NOT declare their own
 * @RequestMapping. Do not hand-edit generated interfaces (the harness excludes them).
 */
public interface BooksApi {

    @GetMapping("/books/{bookId}")
    Mono<ResponseEntity<BookResponse>> getBookById(@PathVariable("bookId") String bookId);

    @GetMapping("/books/by-author/{author}")
    Mono<ResponseEntity<BookResponse>> getBookByAuthor(@PathVariable("author") String author);
}

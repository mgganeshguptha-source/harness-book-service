package com.example.book.service;

import com.example.book.model.BookResponse;
import reactor.core.publisher.Mono;

public interface BookService {
    Mono<BookResponse> getBook(String bookId);
}

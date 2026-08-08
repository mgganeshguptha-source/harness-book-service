package com.example.book;

import com.example.book.controller.BookController;
import com.example.book.model.BookResponse;
import com.example.book.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class BookControllerExtraTest {

    @Mock
    BookService bookService;

    BookController controller;

    @BeforeEach
    void setUp() {
        controller = new BookController(bookService);
    }

    @Test
    void getBookByAuthor_null_returnsBadRequest() {
        Mono<org.springframework.http.ResponseEntity<BookResponse>> mono = controller.getBookByAuthor(null);

        StepVerifier.create(mono)
                .expectNextMatches(resp -> resp.getStatusCodeValue() == 400)
                .verifyComplete();

        verifyNoInteractions(bookService);
    }

    @Test
    void getBookByAuthor_blank_spaces_returnsBadRequest() {
        Mono<org.springframework.http.ResponseEntity<BookResponse>> mono = controller.getBookByAuthor("   ");

        StepVerifier.create(mono)
                .expectNextMatches(resp -> resp.getStatusCodeValue() == 400)
                .verifyComplete();

        verifyNoInteractions(bookService);
    }

    @Test
    void getBookByAuthor_tooLong_returnsBadRequest() {
        String longAuthor = "a".repeat(257);
        Mono<org.springframework.http.ResponseEntity<BookResponse>> mono = controller.getBookByAuthor(longAuthor);

        StepVerifier.create(mono)
                .expectNextMatches(resp -> resp.getStatusCodeValue() == 400)
                .verifyComplete();

        verifyNoInteractions(bookService);
    }

    @Test
    void getBookByAuthor_notFound_returns404() {
        String author = "Unknown Author";
        when(bookService.getBookByAuthor(author)).thenReturn(Mono.empty());

        StepVerifier.create(controller.getBookByAuthor(author))
                .expectNextMatches(resp -> resp.getStatusCodeValue() == 404)
                .verifyComplete();

        verify(bookService).getBookByAuthor(author);
    }

    @Test
    void getBookByAuthor_happyPath_returns200AndBody() {
        String author = "Jane Doe";
        BookResponse response = new BookResponse("book-1", "Some Title", author);
        when(bookService.getBookByAuthor(author)).thenReturn(Mono.just(response));

        StepVerifier.create(controller.getBookByAuthor(author))
                .expectNextMatches(resp -> resp.getStatusCodeValue() == 200
                        && resp.getBody() != null
                        && "book-1".equals(resp.getBody().getBookId())
                        && "Some Title".equals(resp.getBody().getTitle())
                        && author.equals(resp.getBody().getAuthor()))
                .verifyComplete();

        verify(bookService).getBookByAuthor(author);
    }

    @Test
    void getBookById_serviceEmpty_emitsNoItem() {
        String bookId = "no-such-id";
        when(bookService.getBook(bookId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.getBookById(bookId))
                .expectComplete()
                .verify();

        verify(bookService).getBook(bookId);
    }
}

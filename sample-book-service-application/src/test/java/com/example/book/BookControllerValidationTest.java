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
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class BookControllerValidationTest {

    @Mock
    BookService bookService;

    BookController controller;

    @BeforeEach
    void setUp() {
        controller = new BookController(bookService);
    }

    @Test
    void nullAuthorReturnsBadRequest() {
        Mono<ResponseEntity<BookResponse>> mono = controller.getBookByAuthor(null);
        StepVerifier.create(mono)
                .assertNext(resp -> assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void blankAuthorReturnsBadRequest() {
        Mono<ResponseEntity<BookResponse>> mono = controller.getBookByAuthor("   ");
        StepVerifier.create(mono)
                .assertNext(resp -> assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void tooLongAuthorReturnsBadRequest() {
        String longAuthor = "a".repeat(257);
        Mono<ResponseEntity<BookResponse>> mono = controller.getBookByAuthor(longAuthor);
        StepVerifier.create(mono)
                .assertNext(resp -> assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void notFoundReturns404() {
        when(bookService.getBookByAuthor("Unknown")).thenReturn(Mono.empty());
        Mono<ResponseEntity<BookResponse>> mono = controller.getBookByAuthor("Unknown");
        StepVerifier.create(mono)
                .assertNext(resp -> assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void happyPathReturns200WithBody() {
        BookResponse br = new BookResponse("id-1", "The Title", "Author Name");
        when(bookService.getBookByAuthor("Author Name")).thenReturn(Mono.just(br));

        Mono<ResponseEntity<BookResponse>> mono = controller.getBookByAuthor("Author Name");

        StepVerifier.create(mono)
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertEquals("id-1", resp.getBody().getBookId());
                    assertEquals("The Title", resp.getBody().getTitle());
                    assertEquals("Author Name", resp.getBody().getAuthor());
                })
                .verifyComplete();
    }
}

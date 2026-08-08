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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookControllerTest {

    @Mock
    BookService bookService;

    BookController controller;

    @BeforeEach
    void setUp() {
        controller = new BookController(bookService);
    }

    @Test
    void getBookByAuthor_happyPath_returnsOkWithBody() {
        String author = "Jane Doe";
        BookResponse book = new BookResponse("id-1", "Title", author);
        when(bookService.getBookByAuthor(author)).thenReturn(Mono.just(book));

        StepVerifier.create(controller.getBookByAuthor(author))
                .expectNextMatches(resp -> resp.getStatusCode().is2xxSuccessful()
                        && resp.getBody() != null
                        && "id-1".equals(resp.getBody().getBookId())
                        && author.equals(resp.getBody().getAuthor()))
                .verifyComplete();

        verify(bookService).getBookByAuthor(author);
    }

    @Test
    void getBookByAuthor_notFound_returns404() {
        String author = "Unknown";
        when(bookService.getBookByAuthor(author)).thenReturn(Mono.empty());

        StepVerifier.create(controller.getBookByAuthor(author))
                .expectNextMatches(resp -> resp.getStatusCode().value() == 404 && resp.getBody() == null)
                .verifyComplete();

        verify(bookService).getBookByAuthor(author);
    }

    @Test
    void getBookByAuthor_blankAuthor_returns400() {
        String author = "  ";

        StepVerifier.create(controller.getBookByAuthor(author))
                .expectNextMatches(resp -> resp.getStatusCode().value() == 400)
                .verifyComplete();
    }

    @Test
    void getBookByAuthor_tooLongAuthor_returns400() {
        String author = "a".repeat(257);

        StepVerifier.create(controller.getBookByAuthor(author))
                .expectNextMatches(resp -> resp.getStatusCode().value() == 400)
                .verifyComplete();
    }

    @Test
    void getBookByAuthor_nullAuthor_returns400() {
        StepVerifier.create(controller.getBookByAuthor(null))
                .expectNextMatches(resp -> resp.getStatusCode().value() == 400)
                .verifyComplete();
    }
}

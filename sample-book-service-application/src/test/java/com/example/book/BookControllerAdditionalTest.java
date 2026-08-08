package com.example.book;

import com.example.book.controller.BookController;
import com.example.book.model.BookResponse;
import com.example.book.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class BookControllerAdditionalTest {

    @Mock
    BookService bookService;

    @Test
    void getBookByAuthor_happyPath_returns200AndBody() {
        BookController controller = new BookController(bookService);
        BookResponse resp = new BookResponse("id1", "Title 1", "Alice");
        Mockito.when(bookService.getBookByAuthor("Alice")).thenReturn(Mono.just(resp));

        StepVerifier.create(controller.getBookByAuthor("Alice"))
                .expectNextMatches(entity -> entity.getStatusCode().is2xxSuccessful()
                        && entity.getBody() != null
                        && "Alice".equals(entity.getBody().getAuthor())
                        && "id1".equals(entity.getBody().getBookId()))
                .verifyComplete();
    }

    @Test
    void getBookByAuthor_notFound_returns404() {
        BookController controller = new BookController(bookService);
        Mockito.when(bookService.getBookByAuthor("Unknown")).thenReturn(Mono.empty());

        StepVerifier.create(controller.getBookByAuthor("Unknown"))
                .expectNextMatches(entity -> entity.getStatusCode().is4xxClientError()
                        && entity.getStatusCode().value() == 404)
                .verifyComplete();
    }

    @Test
    void getBookByAuthor_blankAuthor_returns400() {
        BookController controller = new BookController(bookService);

        StepVerifier.create(controller.getBookByAuthor("  "))
                .expectNextMatches(entity -> entity.getStatusCode().is4xxClientError()
                        && entity.getStatusCode().value() == 400)
                .verifyComplete();
    }

    @Test
    void getBookByAuthor_nullAuthor_returns400() {
        BookController controller = new BookController(bookService);

        StepVerifier.create(controller.getBookByAuthor(null))
                .expectNextMatches(entity -> entity.getStatusCode().is4xxClientError()
                        && entity.getStatusCode().value() == 400)
                .verifyComplete();
    }

    @Test
    void getBookByAuthor_tooLongAuthor_returns400() {
        BookController controller = new BookController(bookService);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 260; i++) sb.append('a');
        String longAuthor = sb.toString();

        StepVerifier.create(controller.getBookByAuthor(longAuthor))
                .expectNextMatches(entity -> entity.getStatusCode().is4xxClientError()
                        && entity.getStatusCode().value() == 400)
                .verifyComplete();
    }
}

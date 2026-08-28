package com.example.book;

import com.example.book.controller.BookController;
import com.example.book.model.BookResponse;
import com.example.book.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookControllerTest {

    @Test
    public void shouldReturnBadRequestForBlankAuthor() {
        BookService bookService = Mockito.mock(BookService.class);
        BookController controller = new BookController(bookService);

        StepVerifier.create(controller.getBookByAuthor("") )
                .expectNextMatches(resp -> resp.getStatusCode().is4xxClientError())
                .verifyComplete();
    }

    @Test
    public void shouldReturnBadRequestForTooLongAuthor() {
        BookService bookService = Mockito.mock(BookService.class);
        BookController controller = new BookController(bookService);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 260; i++) sb.append('x');
        String longAuthor = sb.toString();

        StepVerifier.create(controller.getBookByAuthor(longAuthor))
                .expectNextMatches(resp -> resp.getStatusCode().is4xxClientError())
                .verifyComplete();
    }

    @Test
    public void shouldReturnNotFoundWhenServiceReturnsEmpty() {
        BookService bookService = Mockito.mock(BookService.class);
        when(bookService.getBookByAuthor(anyString())).thenReturn(Mono.empty());
        BookController controller = new BookController(bookService);

        StepVerifier.create(controller.getBookByAuthor("Unknown Author"))
                .expectNextMatches(resp -> resp.getStatusCode().is4xxClientError() && resp.getStatusCode().value() == 404)
                .verifyComplete();
    }

    @Test
    public void shouldReturnOkWithBodyForHappyPath() {
        BookService bookService = Mockito.mock(BookService.class);
        BookResponse response = new BookResponse("b1", "Title", "Jane Doe");
        when(bookService.getBookByAuthor("Jane Doe")).thenReturn(Mono.just(response));

        BookController controller = new BookController(bookService);

        StepVerifier.create(controller.getBookByAuthor("Jane Doe"))
                .expectNextMatches(resp -> resp.getStatusCode().is2xxSuccessful()
                        && resp.getBody() != null
                        && "b1".equals(resp.getBody().getBookId())
                        && "Jane Doe".equals(resp.getBody().getAuthor())
                )
                .verifyComplete();
    }

    @Test
    public void shouldReturnBadRequestForNullAuthor() {
        BookService bookService = Mockito.mock(BookService.class);
        BookController controller = new BookController(bookService);

        StepVerifier.create(controller.getBookByAuthor((String) null))
                .expectNextMatches(resp -> resp.getStatusCode().is4xxClientError())
                .verifyComplete();
    }
}

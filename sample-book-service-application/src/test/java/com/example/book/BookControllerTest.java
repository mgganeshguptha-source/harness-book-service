package com.example.book;

import com.example.book.controller.BookController;
import com.example.book.model.BookResponse;
import com.example.book.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class BookControllerTest {

    @ExtendWith(MockitoExtension.class)
    public static class WithMockito {

        @Mock
        BookService bookService;

        @InjectMocks
        BookController controller;

        @Test
        void getBookByAuthor_happyPath_returns200AndBody() {
            String author = "Jane Doe";
            BookResponse expected = new BookResponse("b1", "Title", author);

            when(bookService.getBookByAuthor(author)).thenReturn(Mono.just(expected));

            Mono<org.springframework.http.ResponseEntity<BookResponse>> result = controller.getBookByAuthor(author);

            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
                        BookResponse body = response.getBody();
                        assertThat(body).isNotNull();
                        assertThat(body.getBookId()).isEqualTo("b1");
                        assertThat(body.getTitle()).isEqualTo("Title");
                        assertThat(body.getAuthor()).isEqualTo(author);
                    })
                    .verifyComplete();
        }

        @Test
        void getBookByAuthor_notFound_returns404() {
            String author = "Unknown";
            when(bookService.getBookByAuthor(author)).thenReturn(Mono.empty());

            Mono<org.springframework.http.ResponseEntity<BookResponse>> result = controller.getBookByAuthor(author);

            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.getStatusCode().value()).isEqualTo(404);
                        assertThat(response.getBody()).isNull();
                    })
                    .verifyComplete();
        }

        @Test
        void getBookByAuthor_blankAuthor_returns400() {
            Mono<org.springframework.http.ResponseEntity<BookResponse>> resultEmpty = controller.getBookByAuthor("");
            StepVerifier.create(resultEmpty)
                    .assertNext(response -> assertThat(response.getStatusCode().value()).isEqualTo(400))
                    .verifyComplete();

            Mono<org.springframework.http.ResponseEntity<BookResponse>> resultWhitespace = controller.getBookByAuthor("   ");
            StepVerifier.create(resultWhitespace)
                    .assertNext(response -> assertThat(response.getStatusCode().value()).isEqualTo(400))
                    .verifyComplete();

            Mono<org.springframework.http.ResponseEntity<BookResponse>> resultNull = controller.getBookByAuthor(null);
            StepVerifier.create(resultNull)
                    .assertNext(response -> assertThat(response.getStatusCode().value()).isEqualTo(400))
                    .verifyComplete();
        }

        @Test
        void getBookByAuthor_authorTooLong_returns400() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 257; i++) sb.append('a');
            String longAuthor = sb.toString();

            Mono<org.springframework.http.ResponseEntity<BookResponse>> result = controller.getBookByAuthor(longAuthor);

            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.getStatusCode().value()).isEqualTo(400))
                    .verifyComplete();
        }
    }
}

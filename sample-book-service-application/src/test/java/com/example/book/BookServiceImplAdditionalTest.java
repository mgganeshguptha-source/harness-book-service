package com.example.book;

import com.example.book.model.BookResponse;
import com.example.book.model.CatalogBookDto;
import com.example.book.service.BookServiceImpl;
import com.example.book.webclient.CatalogClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class BookServiceImplAdditionalTest {

    @Mock
    CatalogClient catalogClient;

    @Test
    void getBookByAuthor_delegatesToCatalogClient_and_mapsDto() {
        BookServiceImpl svc = new BookServiceImpl(catalogClient);
        CatalogBookDto dto = new CatalogBookDto();
        dto.setBookId("b-123");
        dto.setTitle("The Book");
        dto.setAuthor("Bob");

        Mockito.when(catalogClient.fetchBookByAuthor("Bob")).thenReturn(Mono.just(dto));

        StepVerifier.create(svc.getBookByAuthor("Bob"))
                .expectNextMatches(br -> "b-123".equals(br.getBookId())
                        && "The Book".equals(br.getTitle())
                        && "Bob".equals(br.getAuthor()))
                .verifyComplete();
    }

    @Test
    void getBook_delegatesToCatalogClient_and_mapsDto() {
        BookServiceImpl svc = new BookServiceImpl(catalogClient);
        CatalogBookDto dto = new CatalogBookDto();
        dto.setBookId("b-999");
        dto.setTitle("Another");
        dto.setAuthor("Eve");

        Mockito.when(catalogClient.fetchBook("b-999")).thenReturn(Mono.just(dto));

        StepVerifier.create(svc.getBook("b-999"))
                .expectNextMatches(br -> "b-999".equals(br.getBookId())
                        && "Another".equals(br.getTitle())
                        && "Eve".equals(br.getAuthor()))
                .verifyComplete();
    }
}

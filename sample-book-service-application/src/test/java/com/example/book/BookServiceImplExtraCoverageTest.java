package com.example.book;

import com.example.book.model.CatalogBookDto;
import com.example.book.model.BookResponse;
import com.example.book.service.BookServiceImpl;
import com.example.book.webclient.CatalogClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceImplExtraCoverageTest {

    @Mock
    CatalogClient catalogClient;

    BookServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookServiceImpl(catalogClient);
    }

    @Test
    void getBookByAuthor_mapsDtoToResponse() {
        CatalogBookDto dto = mock(CatalogBookDto.class);
        when(dto.getBookId()).thenReturn("bid-1");
        when(dto.getTitle()).thenReturn("Mapped Title");
        when(dto.getAuthor()).thenReturn("Author X");

        when(catalogClient.fetchBookByAuthor("Author X")).thenReturn(Mono.just(dto));

        StepVerifier.create(service.getBookByAuthor("Author X"))
                .assertNext(resp -> {
                    assertThat(resp).isNotNull();
                    assertThat(resp.getBookId()).isEqualTo("bid-1");
                    assertThat(resp.getTitle()).isEqualTo("Mapped Title");
                    assertThat(resp.getAuthor()).isEqualTo("Author X");
                })
                .verifyComplete();

        verify(catalogClient).fetchBookByAuthor("Author X");
    }

    @Test
    void getBookByAuthor_empty_propagatesEmpty() {
        when(catalogClient.fetchBookByAuthor("Nobody")).thenReturn(Mono.empty());

        StepVerifier.create(service.getBookByAuthor("Nobody"))
                .expectComplete()
                .verify();

        verify(catalogClient).fetchBookByAuthor("Nobody");
    }

    @Test
    void getBook_mapsDtoToResponse() {
        CatalogBookDto dto = mock(CatalogBookDto.class);
        when(dto.getBookId()).thenReturn("b-2");
        when(dto.getTitle()).thenReturn("Title 2");
        when(dto.getAuthor()).thenReturn("Author Y");

        when(catalogClient.fetchBook("b-2")).thenReturn(Mono.just(dto));

        StepVerifier.create(service.getBook("b-2"))
                .assertNext(resp -> {
                    assertThat(resp.getBookId()).isEqualTo("b-2");
                    assertThat(resp.getTitle()).isEqualTo("Title 2");
                    assertThat(resp.getAuthor()).isEqualTo("Author Y");
                })
                .verifyComplete();

        verify(catalogClient).fetchBook("b-2");
    }

    @Test
    void getBook_empty_propagatesEmpty() {
        when(catalogClient.fetchBook("missing")).thenReturn(Mono.empty());

        StepVerifier.create(service.getBook("missing"))
                .expectComplete()
                .verify();

        verify(catalogClient).fetchBook("missing");
    }
}

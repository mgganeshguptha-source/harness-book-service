package com.example.book;

import com.example.book.model.BookResponse;
import com.example.book.model.CatalogBookDto;
import com.example.book.service.BookServiceImpl;
import com.example.book.webclient.CatalogClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class BookServiceImplExtraTest {

    @Mock
    CatalogClient catalogClient;

    BookServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookServiceImpl(catalogClient);
    }

    @Test
    void getBookByAuthor_happyPath_mapsDtoToResponse() {
        CatalogBookDto dto = mock(CatalogBookDto.class);
        when(dto.getBookId()).thenReturn("id1");
        when(dto.getTitle()).thenReturn("Title A");
        when(dto.getAuthor()).thenReturn("Author A");

        when(catalogClient.fetchBookByAuthor("Author A")).thenReturn(Mono.just(dto));

        StepVerifier.create(service.getBookByAuthor("Author A"))
                .assertNext(resp -> {
                    assertEquals("id1", resp.getBookId());
                    assertEquals("Title A", resp.getTitle());
                    assertEquals("Author A", resp.getAuthor());
                })
                .verifyComplete();
    }

    @Test
    void getBookByAuthor_emptyCompletesEmpty() {
        when(catalogClient.fetchBookByAuthor("Nobody")).thenReturn(Mono.empty());
        StepVerifier.create(service.getBookByAuthor("Nobody"))
                .verifyComplete();
    }
}

package com.example.book;

import com.example.book.model.BookResponse;
import com.example.book.model.CatalogBookDto;
import com.example.book.service.BookServiceImpl;
import com.example.book.webclient.CatalogClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    CatalogClient catalogClient;

    @InjectMocks
    BookServiceImpl service;

    @Test
    void getBookByAuthor_delegatesToCatalogClient_andMapsDtoToResponse() {
        String author = "Jane Doe";
        CatalogBookDto dto = mock(CatalogBookDto.class);
        when(dto.getBookId()).thenReturn("b-123");
        when(dto.getTitle()).thenReturn("A Good Book");
        when(dto.getAuthor()).thenReturn(author);

        when(catalogClient.fetchBookByAuthor(author)).thenReturn(Mono.just(dto));

        Mono<BookResponse> result = service.getBookByAuthor(author);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertThat(resp).isNotNull();
                    assertThat(resp.getBookId()).isEqualTo("b-123");
                    assertThat(resp.getTitle()).isEqualTo("A Good Book");
                    assertThat(resp.getAuthor()).isEqualTo(author);
                })
                .verifyComplete();
    }
}
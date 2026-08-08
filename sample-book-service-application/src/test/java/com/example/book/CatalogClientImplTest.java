package com.example.book;

import com.example.book.model.CatalogBookDto;
import com.example.book.webclient.CatalogClientImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class CatalogClientImplTest {

    @Mock
    WebClient webClient;

    @Mock
    WebClient.RequestHeadersUriSpec uriSpec;

    @Mock
    WebClient.RequestHeadersSpec headersSpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @Test
    void fetchBookByAuthor_usesWebClient_and_returnsDto() {
        CatalogBookDto dto = new CatalogBookDto();
        dto.setBookId("c1");
        dto.setTitle("C Title");
        dto.setAuthor("Carol");

        Mockito.when(webClient.get()).thenReturn(uriSpec);
        Mockito.when(uriSpec.uri("/catalog/books/by-author/{author}", "Carol")).thenReturn(headersSpec);
        Mockito.when(headersSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(CatalogBookDto.class)).thenReturn(Mono.just(dto));

        CatalogClientImpl client = new CatalogClientImpl(webClient);

        StepVerifier.create(client.fetchBookByAuthor("Carol"))
                .expectNextMatches(d -> "Carol".equals(d.getAuthor()) && "c1".equals(d.getBookId()))
                .verifyComplete();
    }

    @Test
    void fetchBook_usesWebClient_and_returnsDto() {
        CatalogBookDto dto = new CatalogBookDto();
        dto.setBookId("c2");
        dto.setTitle("Other");
        dto.setAuthor("Dan");

        Mockito.when(webClient.get()).thenReturn(uriSpec);
        Mockito.when(uriSpec.uri("/catalog/books/{id}", "c2")).thenReturn(headersSpec);
        Mockito.when(headersSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(CatalogBookDto.class)).thenReturn(Mono.just(dto));

        CatalogClientImpl client = new CatalogClientImpl(webClient);

        StepVerifier.create(client.fetchBook("c2"))
                .expectNextMatches(d -> "Dan".equals(d.getAuthor()) && "c2".equals(d.getBookId()))
                .verifyComplete();
    }
}

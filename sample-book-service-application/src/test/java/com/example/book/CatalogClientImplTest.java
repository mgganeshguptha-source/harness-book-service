package com.example.book;

import com.example.book.model.CatalogBookDto;
import com.example.book.webclient.CatalogClientImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CatalogClientImplTest {

    @Test
    void fetchBookByAuthor_chainsWebClientAndReturnsDto() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec requestUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestUriSpec);
        when(requestUriSpec.uri("/catalog/books/by-author/{author}", "Author X")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        CatalogBookDto dto = mock(CatalogBookDto.class);
        when(responseSpec.bodyToMono(CatalogBookDto.class)).thenReturn(Mono.just(dto));

        CatalogClientImpl client = new CatalogClientImpl(webClient);

        StepVerifier.create(client.fetchBookByAuthor("Author X"))
                .expectNext(dto)
                .verifyComplete();

        verify(webClient).get();
        verify(requestUriSpec).uri("/catalog/books/by-author/{author}", "Author X");
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(CatalogBookDto.class);
    }

    @Test
    void fetchBook_chainsWebClientAndReturnsDto() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec requestUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestUriSpec);
        when(requestUriSpec.uri("/catalog/books/{id}", "book-1")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        CatalogBookDto dto = mock(CatalogBookDto.class);
        when(responseSpec.bodyToMono(CatalogBookDto.class)).thenReturn(Mono.just(dto));

        CatalogClientImpl client = new CatalogClientImpl(webClient);

        StepVerifier.create(client.fetchBook("book-1"))
                .expectNext(dto)
                .verifyComplete();

        verify(webClient).get();
        verify(requestUriSpec).uri("/catalog/books/{id}", "book-1");
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(CatalogBookDto.class);
    }
}

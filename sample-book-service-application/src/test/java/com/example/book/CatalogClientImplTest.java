package com.example.book;

import com.example.book.model.CatalogBookDto;
import com.example.book.webclient.CatalogClientImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CatalogClientImplTest {

    @Test
    public void fetchBookByAuthorUsesWebClientChainAndReturnsDto() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        String author = "Jane Doe";
        CatalogBookDto dto = new CatalogBookDto();
        dto.setBookId("b-1");
        dto.setTitle("Title 1");
        dto.setAuthor("Jane Doe");

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(eq("/catalog/books/by-author/{author}"), eq(author))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CatalogBookDto.class)).thenReturn(Mono.just(dto));

        CatalogClientImpl client = new CatalogClientImpl(webClient);

        StepVerifier.create(client.fetchBookByAuthor(author))
                .expectNextMatches(d -> d != null && "b-1".equals(d.getBookId()) && "Jane Doe".equals(d.getAuthor()))
                .verifyComplete();

        verify(webClient).get();
        verify(uriSpec).uri(eq("/catalog/books/by-author/{author}"), eq(author));
        verify(headersSpec).retrieve();
        verify(responseSpec).bodyToMono(CatalogBookDto.class);
    }

    @Test
    public void fetchBookByIdUsesWebClientChainAndReturnsDto() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        String id = "id-77";
        CatalogBookDto dto = new CatalogBookDto();
        dto.setBookId(id);
        dto.setTitle("Some Title");
        dto.setAuthor("Some Author");

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(eq("/catalog/books/{id}"), eq(id))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CatalogBookDto.class)).thenReturn(Mono.just(dto));

        CatalogClientImpl client = new CatalogClientImpl(webClient);

        StepVerifier.create(client.fetchBook(id))
                .expectNextMatches(d -> d != null && id.equals(d.getBookId()) && "Some Author".equals(d.getAuthor()))
                .verifyComplete();

        verify(webClient).get();
        verify(uriSpec).uri(eq("/catalog/books/{id}"), eq(id));
        verify(headersSpec).retrieve();
        verify(responseSpec).bodyToMono(CatalogBookDto.class);
    }
}

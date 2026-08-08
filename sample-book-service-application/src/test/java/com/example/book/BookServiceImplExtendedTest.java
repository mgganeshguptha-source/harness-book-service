package com.example.book;

import com.example.book.model.BookResponse;
import com.example.book.model.CatalogBookDto;
import com.example.book.service.BookService;
import com.example.book.service.BookServiceImpl;
import com.example.book.webclient.CatalogClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookServiceImplExtendedTest {

    @Test
    public void getBookByAuthorMapsDtoToResponse() {
        CatalogClient catalogClient = mock(CatalogClient.class);
        CatalogBookDto dto = new CatalogBookDto();
        dto.setBookId("id-123");
        dto.setTitle("The Title");
        dto.setAuthor("Author X");
        when(catalogClient.fetchBookByAuthor(eq("Author X"))).thenReturn(Mono.just(dto));

        BookService service = new BookServiceImpl(catalogClient);

        StepVerifier.create(service.getBookByAuthor("Author X"))
                .expectNextMatches(resp -> "id-123".equals(resp.getBookId())
                        && "The Title".equals(resp.getTitle())
                        && "Author X".equals(resp.getAuthor()))
                .verifyComplete();
    }

    @Test
    public void getBookByAuthorReturnsEmptyWhenClientEmpty() {
        CatalogClient catalogClient = mock(CatalogClient.class);
        when(catalogClient.fetchBookByAuthor(eq("No One"))).thenReturn(Mono.empty());

        BookService service = new BookServiceImpl(catalogClient);

        StepVerifier.create(service.getBookByAuthor("No One"))
                .expectNextCount(0)
                .verifyComplete();
    }
}

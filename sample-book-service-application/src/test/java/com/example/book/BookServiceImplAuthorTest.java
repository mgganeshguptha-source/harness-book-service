package com.example.book;

import com.example.book.model.CatalogBookDto;
import com.example.book.model.BookResponse;
import com.example.book.webclient.CatalogClient;
import com.example.book.service.BookServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class BookServiceImplAuthorTest {

    @Mock
    CatalogClient catalogClient;

    @Test
    void getBookByAuthor_delegatesToCatalog_andMapsDto() {
        CatalogBookDto dto = new CatalogBookDto();
        dto.setBookId("id42");
        dto.setTitle("The Answer");
        dto.setAuthor("Douglas Adams");

        when(catalogClient.fetchBookByAuthor("Douglas Adams")).thenReturn(Mono.just(dto));
        BookServiceImpl svc = new BookServiceImpl(catalogClient);

        Mono<BookResponse> result = svc.getBookByAuthor("Douglas Adams");

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertThat(resp.getBookId()).isEqualTo("id42");
                    assertThat(resp.getTitle()).isEqualTo("The Answer");
                    assertThat(resp.getAuthor()).isEqualTo("Douglas Adams");
                })
                .verifyComplete();

        verify(catalogClient).fetchBookByAuthor("Douglas Adams");
    }

    @Test
    void getBookByAuthor_notFound_propagatesEmpty() {
        when(catalogClient.fetchBookByAuthor("No One")).thenReturn(Mono.empty());
        BookServiceImpl svc = new BookServiceImpl(catalogClient);

        StepVerifier.create(svc.getBookByAuthor("No One"))
                .verifyComplete();

        verify(catalogClient).fetchBookByAuthor("No One");
    }
}

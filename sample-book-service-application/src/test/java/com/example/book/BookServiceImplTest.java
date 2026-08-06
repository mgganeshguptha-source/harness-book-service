package com.example.book;

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

import static org.mockito.Mockito.when;

/**
 * Reactive unit test using StepVerifier (no .block()).
 * Shows the happy-path assertion the harness's generated tests should follow.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    void getBook_returnsMappedResponse() {
        CatalogBookDto dto = new CatalogBookDto();
        dto.setBookId("B1");
        dto.setTitle("Reactive Spring");
        dto.setAuthor("J. Doe");
        when(catalogClient.fetchBook("B1")).thenReturn(Mono.just(dto));

        StepVerifier.create(bookService.getBook("B1"))
                .expectNextMatches(r ->
                        r.getBookId().equals("B1")
                        && r.getTitle().equals("Reactive Spring")
                        && r.getAuthor().equals("J. Doe"))
                .verifyComplete();
    }
}

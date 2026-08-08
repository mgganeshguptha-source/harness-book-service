package com.example.book;

import com.example.book.model.BookResponse;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class BookResponseTest {

    @Test
    void constructor_and_getters_setters_work() {
        BookResponse br = new BookResponse("idX", "T", "AuthorX");
        assertThat(br.getBookId()).isEqualTo("idX");
        assertThat(br.getTitle()).isEqualTo("T");
        assertThat(br.getAuthor()).isEqualTo("AuthorX");

        br.setBookId("idY");
        br.setTitle("TT");
        br.setAuthor("AuthorY");

        assertThat(br.getBookId()).isEqualTo("idY");
        assertThat(br.getTitle()).isEqualTo("TT");
        assertThat(br.getAuthor()).isEqualTo("AuthorY");
    }
}

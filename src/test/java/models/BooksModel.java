package models;

import lombok.Data;

import java.util.List;

@Data
public class BooksModel {
    private List<BookModel> books;
}
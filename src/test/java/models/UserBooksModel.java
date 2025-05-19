package models;

import lombok.Data;

import java.util.List;

@Data
public class UserBooksModel {
    private String userId;
    private String username;
    private List<BookModel> books;
}
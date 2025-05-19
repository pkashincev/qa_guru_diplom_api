package models;

import lombok.Data;

import java.util.List;

@Data
public class AddBooksResponseModel {
    private List<IsbnModel> books;
}
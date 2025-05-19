package models;

import lombok.Data;

import java.util.List;

@Data
public class AddBooksRequestModel {
    private String userId;
    private List<IsbnModel> collectionOfIsbns;
}
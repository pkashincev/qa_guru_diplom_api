package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RegistrationResponseModel {
    @JsonProperty("userID")
    private String userId;
    private String username;
    private List<BookModel> books;
}
package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BookModel {
    private String isbn;
    private String title;
    private String subTitle;
    private String author;
    @JsonProperty("publish_date")
    private String publishDate;
    private String publisher;
    private Integer pages;
    private String description;
    private String website;
}
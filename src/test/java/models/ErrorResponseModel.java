package models;

import lombok.Data;

@Data
public class ErrorResponseModel {
    private String code;
    private String message;
}
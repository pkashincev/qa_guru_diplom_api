package api;

import models.*;

import java.util.List;

import static io.restassured.RestAssured.given;
import static specs.DemoqaSpecs.*;

public class BooksApi {

    public static BooksModel getAllBooks() {
        return
                given(noBodyRequestSpec)
                        .when()
                        .get("/BookStore/v1/Books")
                        .then()
                        .spec(getResponseSpecByStatusCode(200))
                        .extract().as(BooksModel.class);
    }

    public static void addBooks(String userId, String token, List<IsbnModel> isbn) {
        AddBooksRequestModel request = new AddBooksRequestModel();
        request.setUserId(userId);
        request.setCollectionOfIsbns(isbn);
        given(withBodyRequestSpec)
                .body(request)
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/BookStore/v1/Books")
                .then()
                .spec(getResponseSpecByStatusCode(201));
    }

    public static List<BookModel> getUserBooks(String userId, String token) {
        return
                given(noBodyRequestSpec)
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .get("/Account/v1/User/" + userId)
                        .then()
                        .spec(getResponseSpecByStatusCode(200))
                        .extract().as(UserBooksModel.class).getBooks();
    }
}
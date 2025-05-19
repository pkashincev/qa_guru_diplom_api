package tests;

import api.BooksApi;
import api.RegistrationApi;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import models.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.TestData;

import java.util.List;

import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static specs.DemoqaSpecs.*;
import static utils.RandomUtils.getRandomInt;

@Feature("BookStore")
public class BookStoreTests extends BaseTest {

    private static final TestData data = new TestData();

    @Tag("POSITIVE")
    @Story("Добавление книг в профиль")
    @Test
    @DisplayName("Успешное добавление книги в профиль пользователя")
    void successfulAddBookTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        List<BookModel> books = step("Получить список доступных книг", () ->
                BooksApi.getAllBooks().getBooks());
        BookModel book = step("Выбрать произвольную книгу из полученного списка", () ->
                books.get(getRandomInt(0, books.size() - 1)));
        AddBooksResponseModel addBooksResponse = step("Выполнить запрос на добавление книги в профиль пользователя", () -> {
            AddBooksRequestModel request = new AddBooksRequestModel();
            request.setUserId(registrationResponse.getUserId());
            request.setCollectionOfIsbns(List.of(new IsbnModel(book.getIsbn())));
            return
                    given(withBodyRequestSpec)
                            .body(request)
                            .header("Authorization", "Bearer " + generateTokenResponse.getToken())
                            .when()
                            .post("/BookStore/v1/Books")
                            .then()
                            .spec(getResponseSpecByStatusCode(201))
                            .extract().as(AddBooksResponseModel.class);
        });
        step("Проверить, что в ответе присутствует isbn добавленной книги", () ->
                assertThat(addBooksResponse.getBooks()).extracting(IsbnModel::getIsbn).contains(book.getIsbn())
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("NEGATIVE")
    @Story("Добавление книг в профиль")
    @Test
    @DisplayName("Добавление книги которая уже есть в профиле пользователя")
    void AddAlreadyPresentBookInProfileTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        List<BookModel> books = step("Получить список доступных книг", () ->
                BooksApi.getAllBooks().getBooks());
        BookModel book = step("Выбрать произвольную книгу из полученного списка", () ->
                books.get(getRandomInt(0, books.size() - 1)));
        step("Добавить книгу в профиль пользователя", () ->
                BooksApi.addBooks(registrationResponse.getUserId(), generateTokenResponse.getToken(),
                        List.of(new IsbnModel(book.getIsbn())))
        );
        ErrorResponseModel errorResponse = step("Выполнить повторный запрос на добавление той же самой книги в профиль пользователя", () -> {
            AddBooksRequestModel request = new AddBooksRequestModel();
            request.setUserId(registrationResponse.getUserId());
            request.setCollectionOfIsbns(List.of(new IsbnModel(book.getIsbn())));
            return
                    given(withBodyRequestSpec)
                            .body(request)
                            .header("Authorization", "Bearer " + generateTokenResponse.getToken())
                            .when()
                            .post("/BookStore/v1/Books")
                            .then()
                            .spec(getResponseSpecByStatusCode(400))
                            .extract().as(ErrorResponseModel.class);
        });
        step("Проверить значение атрибута code в ответе с ошибкой", () ->
                assertThat(errorResponse.getCode()).isEqualTo("1210")
        );
        step("Проверить значение атрибута message в ответе с ошибкой", () ->
                assertThat(errorResponse.getMessage()).isEqualTo("ISBN already present in the User's Collection!")
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("NEGATIVE")
    @Story("Добавление книг в профиль")
    @Test
    @DisplayName("Добавление несуществующей книги")
    void AddNonExistentBookTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        ErrorResponseModel errorResponse = step("Выполнить запрос на добавление несуществующей книги в профиль пользователя", () -> {
            AddBooksRequestModel request = new AddBooksRequestModel();
            request.setUserId(registrationResponse.getUserId());
            request.setCollectionOfIsbns(List.of(new IsbnModel(data.getRandomString(13))));
            return
                    given(withBodyRequestSpec)
                            .body(request)
                            .header("Authorization", "Bearer " + generateTokenResponse.getToken())
                            .when()
                            .post("/BookStore/v1/Books")
                            .then()
                            .spec(getResponseSpecByStatusCode(400))
                            .extract().as(ErrorResponseModel.class);
        });
        step("Проверить значение атрибута code в ответе с ошибкой", () ->
                assertThat(errorResponse.getCode()).isEqualTo("1205")
        );
        step("Проверить значение атрибута message в ответе с ошибкой", () ->
                assertThat(errorResponse.getMessage()).isEqualTo("ISBN supplied is not available in Books Collection!")
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("NEGATIVE")
    @Story("Добавление книг в профиль")
    @Test
    @DisplayName("Добавление книги, токен недействителен")
    void AddBookWithExpiredTokenTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        List<BookModel> books = step("Получить список доступных книг", () ->
                BooksApi.getAllBooks().getBooks());
        BookModel book = step("Выбрать произвольную книгу из полученного списка", () ->
                books.get(getRandomInt(0, books.size() - 1)));
        ErrorResponseModel errorResponse = step("Выполнить запрос на добавление книги c недействительным токеном", () -> {
            AddBooksRequestModel request = new AddBooksRequestModel();
            request.setUserId(registrationResponse.getUserId());
            request.setCollectionOfIsbns(List.of(new IsbnModel(book.getIsbn())));
            return
                    given(withBodyRequestSpec)
                            .body(request)
                            .header("Authorization", "Bearer " + data.getRandomString(170))
                            .when()
                            .post("/BookStore/v1/Books")
                            .then()
                            .spec(getResponseSpecByStatusCode(401))
                            .extract().as(ErrorResponseModel.class);
        });
        step("Проверить значение атрибута code в ответе с ошибкой", () ->
                assertThat(errorResponse.getCode()).isEqualTo("1200")
        );
        step("Проверить значение атрибута message в ответе с ошибкой", () ->
                assertThat(errorResponse.getMessage()).isEqualTo("User not authorized!")
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("NEGATIVE")
    @Story("Добавление книг в профиль")
    @Test
    @DisplayName("Добавление книги для несуществующего пользователя")
    void AddBookForNonExistentUserTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        List<BookModel> books = step("Получить список доступных книг", () ->
                BooksApi.getAllBooks().getBooks());
        BookModel book = step("Выбрать произвольную книгу из полученного списка", () ->
                books.get(getRandomInt(0, books.size() - 1)));
        ErrorResponseModel errorResponse = step("Выполнить запрос на добавление книги для несуществующего пользователя", () -> {
            AddBooksRequestModel request = new AddBooksRequestModel();
            request.setUserId(data.getRandomUUID());
            request.setCollectionOfIsbns(List.of(new IsbnModel(book.getIsbn())));
            return
                    given(withBodyRequestSpec)
                            .body(request)
                            .header("Authorization", "Bearer " + generateTokenResponse.getToken())
                            .when()
                            .post("/BookStore/v1/Books")
                            .then()
                            .spec(getResponseSpecByStatusCode(401))
                            .extract().as(ErrorResponseModel.class);
        });
        step("Проверить значение атрибута code в ответе с ошибкой", () ->
                assertThat(errorResponse.getCode()).isEqualTo("1207")
        );
        step("Проверить значение атрибута message в ответе с ошибкой", () ->
                assertThat(errorResponse.getMessage()).isEqualTo("User Id not correct!")
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("POSITIVE")
    @Story("Удаление книг из профиля")
    @Test
    @DisplayName("Успешное удаление книги из профиля пользователя")
    void successfulDeleteBookTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        List<BookModel> books = step("Получить список доступных книг", () ->
                BooksApi.getAllBooks().getBooks());
        List<IsbnModel> addedIsbn = step("Выбрать 2 книги для добавления в профиль пользователя", () ->
                List.of(new IsbnModel(books.get(0).getIsbn()), new IsbnModel(books.get(1).getIsbn()))
        );
        step("Добавить книги в профиль пользователя", () ->
                BooksApi.addBooks(registrationResponse.getUserId(), generateTokenResponse.getToken(), addedIsbn)
        );
        step("Выполнить запрос на удаление одной из книг", () -> {
            DeleteBookRequestModel request = new DeleteBookRequestModel();
            request.setIsbn(addedIsbn.get(0).getIsbn());
            request.setUserId(registrationResponse.getUserId());
            given(withBodyRequestSpec)
                    .body(request)
                    .header("Authorization", "Bearer " + generateTokenResponse.getToken())
                    .when()
                    .delete("/BookStore/v1/Book")
                    .then()
                    .spec(getResponseSpecByStatusCode(204)
                            .body(emptyOrNullString()));
        });
        List<BookModel> userBooks = step("Запросить список книг пользователя", () ->
                BooksApi.getUserBooks(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
        step("Проверить, что в списке отсутствует удаленная книга", () ->
                assertThat(userBooks).extracting(BookModel::getIsbn).doesNotContain(addedIsbn.get(0).getIsbn())
        );
        step("Проверить, что в списке присутсвует та книга, что не удалялась", () ->
                assertThat(userBooks).extracting(BookModel::getIsbn).contains(addedIsbn.get(1).getIsbn())
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("NEGATIVE")
    @Story("Удаление книг из профиля")
    @Test
    @DisplayName("Удаление книги которой нет в профиле пользователя")
    void deleteNonExistentBookTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        ErrorResponseModel errorResponse = step("Выполнить запрос на удаление книги", () -> {
            DeleteBookRequestModel request = new DeleteBookRequestModel();
            request.setIsbn(data.getRandomString(13));
            request.setUserId(registrationResponse.getUserId());
            return
                    given(withBodyRequestSpec)
                            .body(request)
                            .header("Authorization", "Bearer " + generateTokenResponse.getToken())
                            .when()
                            .delete("/BookStore/v1/Book")
                            .then()
                            .spec(getResponseSpecByStatusCode(400))
                            .extract().as(ErrorResponseModel.class);
        });
        step("Проверить значение атрибута code в ответе с ошибкой", () ->
                assertThat(errorResponse.getCode()).isEqualTo("1206")
        );
        step("Проверить значение атрибута message в ответе с ошибкой", () ->
                assertThat(errorResponse.getMessage()).isEqualTo("ISBN supplied is not available in User's Collection!")
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("NEGATIVE")
    @Story("Удаление книг из профиля")
    @Test
    @DisplayName("Удаление книги для несуществующего пользователя")
    void deleteBookForNonExistentUserTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        List<BookModel> books = step("Получить список доступных книг", () ->
                BooksApi.getAllBooks().getBooks());
        BookModel book = step("Выбрать произвольную книгу из полученного списка", () ->
                books.get(getRandomInt(0, books.size() - 1)));
        ErrorResponseModel errorResponse = step("Выполнить запрос на удаление книги", () -> {
            DeleteBookRequestModel request = new DeleteBookRequestModel();
            request.setIsbn(book.getIsbn());
            request.setUserId(data.getRandomUUID());
            return
                    given(withBodyRequestSpec)
                            .body(request)
                            .header("Authorization", "Bearer " + generateTokenResponse.getToken())
                            .when()
                            .delete("/BookStore/v1/Book")
                            .then()
                            .spec(getResponseSpecByStatusCode(401))
                            .extract().as(ErrorResponseModel.class);
        });
        step("Проверить значение атрибута code в ответе с ошибкой", () ->
                assertThat(errorResponse.getCode()).isEqualTo("1207")
        );
        step("Проверить значение атрибута message в ответе с ошибкой", () ->
                assertThat(errorResponse.getMessage()).isEqualTo("User Id not correct!")
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("POSITIVE")
    @Story("Удаление книг из профиля")
    @Test
    @DisplayName("Успешное удаление всех книг из профиля пользователя")
    void successfulDeleteAllBooksTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        List<BookModel> books = step("Получить список доступных книг", () ->
                BooksApi.getAllBooks().getBooks());
        List<IsbnModel> addedIsbn = step("Выбрать 2 книги для добавления в профиль пользователя", () ->
                List.of(new IsbnModel(books.get(0).getIsbn()), new IsbnModel(books.get(1).getIsbn()))
        );
        step("Добавить книги в профиль пользователя", () ->
                BooksApi.addBooks(registrationResponse.getUserId(), generateTokenResponse.getToken(), addedIsbn)
        );
        step("Выполнить запрос на удаление всех книг", () ->
                given(noBodyRequestSpec)
                        .param("UserId", registrationResponse.getUserId())
                        .header("Authorization", "Bearer " + generateTokenResponse.getToken())
                        .when()
                        .delete("/BookStore/v1/Books")
                        .then()
                        .spec(getResponseSpecByStatusCode(204))
                        .body(emptyOrNullString())
        );
        List<BookModel> userBooks = step("Запросить список книг пользователя", () ->
                BooksApi.getUserBooks(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
        step("Проверить, что в списке нет ни одной книги", () ->
                assertThat(userBooks).isEmpty()
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }
}
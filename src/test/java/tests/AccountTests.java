package tests;

import api.RegistrationApi;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import models.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import utils.TestData;

import java.util.stream.Stream;

import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static specs.DemoqaSpecs.*;

@Feature("Account")
public class AccountTests extends BaseTest {

    private static final TestData data = new TestData();

    private static Stream<Arguments> emptyDataProviderForLoginRequest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        return Stream.of(
                Arguments.of(new LoginRequestModel(userName, "")),
                Arguments.of(new LoginRequestModel(userName, null)),
                Arguments.of(new LoginRequestModel("", password)),
                Arguments.of(new LoginRequestModel(null, password))
        );
    }

    @Tag("POSITIVE")
    @Story("Создание пользователя")
    @Test
    @DisplayName("Уcпешное создание нового пользователя")
    void successfulRegistrationTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Выполнить запрос на создание нового пользователя", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/User")
                        .then()
                        .spec(getResponseSpecByStatusCode(201))
                        .extract().as(RegistrationResponseModel.class)
        );
        step("Проверить, что в ответе присутствует атрибут userID и он соответствует формату UUID", () ->
                assertThat(registrationResponse.getUserId()).matches(data.getUUIDPattern())
        );
        step("Проверить, что в ответе присутствует атрибут userName и он равен userName из запроса", () ->
                assertThat(registrationResponse.getUsername()).isEqualTo(userName)
        );
        step("Проверить, что в ответе присутствует массив books, который для нового пользователя является пустым", () ->
                assertThat(registrationResponse.getBooks()).isEmpty()
        );
        step("Удалить созданного пользователя", () -> {
            GenerateTokenResponseModel generateTokenResponse = RegistrationApi.generateToken(userName, password);
            RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken());
        });
    }

    @Tag("NEGATIVE")
    @Story("Создание пользователя")
    @Test
    @DisplayName("Создание пользователя. Пароль не соответствует заданным требованиям")
    void unsuccessfulRegistrationWithIncorrectPasswordTest() {
        String userName = data.getUserName();
        String password = data.getRandomString(10);
        ErrorResponseModel registrationResponse = step("Выполнить запрос на создание нового пользователя", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/User")
                        .then()
                        .spec(getResponseSpecByStatusCode(400))
                        .extract().as(ErrorResponseModel.class)
        );
        step("Проверить значение атрибута code в ответе с ошибкой", () ->
                assertThat(registrationResponse.getCode()).isEqualTo("1300")
        );
        step("Проверить значение атрибута message в ответе с ошибкой", () ->
                assertThat(registrationResponse.getMessage()).isEqualTo("Passwords must have at least one non alphanumeric" +
                        " character, one digit ('0'-'9'), one uppercase ('A'-'Z'), one lowercase ('a'-'z'), one special" +
                        " character and Password must be eight characters or longer.")
        );
    }

    @Tag("NEGATIVE")
    @Story("Создание пользователя")
    @Test
    @DisplayName("Создание пользователя. Пользователь уже существует")
    void unsuccessfulRegistrationUserAlreadyExistsTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        ErrorResponseModel errorResponse = step("Выполнить запрос на содание пользователя с таким же userName", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/User")
                        .then()
                        .spec(getResponseSpecByStatusCode(406))
                        .extract().as(ErrorResponseModel.class)
        );
        step("Проверить значение атрибута code в ответе с ошибкой", () ->
                assertThat(errorResponse.getCode()).isEqualTo("1204")
        );
        step("Проверить значение атрибута message в ответе с ошибкой", () ->
                assertThat(errorResponse.getMessage()).isEqualTo("User exists!")
        );
        step("Удалить созданного пользователя", () -> {
            GenerateTokenResponseModel generateTokenResponse = RegistrationApi.generateToken(userName, password);
            RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken());
        });
    }

    @Tag("NEGATIVE")
    @Story("Создание пользователя")
    @MethodSource("emptyDataProviderForLoginRequest")
    @ParameterizedTest
    @DisplayName("Создание пользователя. В запросе отсутствуют имя или пароль")
    void unsuccessfulRegistrationEmptyUserNameOrPasswordTest(LoginRequestModel request) {
        ErrorResponseModel errorResponse = step("Выполнить запрос на создание нового пользователя", () ->
                given(withBodyRequestSpec)
                        .body(request)
                        .when()
                        .post("/Account/v1/User")
                        .then()
                        .spec(getResponseSpecByStatusCode(400))
                        .extract().as(ErrorResponseModel.class)
        );
        step("Проверить значение атрибута code в ответе с ошибкой", () ->
                assertThat(errorResponse.getCode()).isEqualTo("1200")
        );
        step("Проверить значение атрибута message в ответе с ошибкой", () ->
                assertThat(errorResponse.getMessage()).isEqualTo("UserName and Password required.")
        );
    }

    @Tag("POSITIVE")
    @Story("Проверка авторизации пользователя")
    @Test
    @DisplayName("Проверка авторизации пользователя. Пользователь авторизован")
    void checkAuthorizationForAuthorizedUserTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        Boolean isAuthorized = step("Выполнить запрос на проверку авторизации", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/Authorized")
                        .then()
                        .spec(getResponseSpecByStatusCode(200))
                        .extract().as(Boolean.class)
        );
        step("Проверить, что в ответе было получено значение 'true'", () ->
                assertThat(isAuthorized).isEqualTo(true)
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("POSITIVE")
    @Story("Проверка авторизации пользователя")
    @Test
    @DisplayName("Проверка авторизации пользователя. Пользователь не авторизован")
    void checkAuthorizationForNonAuthorizedUserTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        Boolean isAuthorized = step("Выполнить запрос на проверку авторизации", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/Authorized")
                        .then()
                        .spec(getResponseSpecByStatusCode(200))
                        .extract().as(Boolean.class)
        );
        step("Проверить, что в ответе было получено значение 'false'", () ->
                assertThat(isAuthorized).isEqualTo(false)
        );
        step("Удалить созданного пользователя", () -> {
            GenerateTokenResponseModel generateTokenResponse = RegistrationApi.generateToken(userName, password);
            RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken());
        });
    }

    @Tag("NEGATIVE")
    @Story("Проверка авторизации пользователя")
    @Test
    @DisplayName("Проверка авторизации пользователя. Пользователь не существует")
    void checkAuthorizationForNonExistentUserTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        ErrorResponseModel errorResponse = step("Выполнить запрос на проверку авторизации для несуществующего пользователя", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/Authorized")
                        .then()
                        .spec(getResponseSpecByStatusCode(404))
                        .extract().as(ErrorResponseModel.class)
        );
        step("Проверить значение атрибута code в ответе с ошибкой", () ->
                assertThat(errorResponse.getCode()).isEqualTo("1207")
        );
        step("Проверить значение атрибута message в ответе с ошибкой", () ->
                assertThat(errorResponse.getMessage()).isEqualTo("User not found!")
        );
    }

    @Tag("POSITIVE")
    @Story("Удаление пользователя")
    @Test
    @DisplayName("Успешное удаление пользователя")
    void successfulDeleteUserTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        step("Выполнить запрос на удаление", () ->
                given(noBodyRequestSpec)
                        .header("Authorization", "Bearer " + generateTokenResponse.getToken())
                        .when()
                        .delete("/Account/v1/User/" + registrationResponse.getUserId())
                        .then()
                        .spec(getResponseSpecByStatusCode(204))
                        .body(emptyOrNullString())
        );
        step("Проверить, что ранее созданный пользователь не существует", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/Authorized")
                        .then()
                        .spec(getResponseSpecByStatusCode(404))
        );
    }

    @Tag("NEGATIVE")
    @Story("Удаление пользователя")
    @Test
    @DisplayName("Удаление пользователя, токен недействителен")
    void deleteUserWithExpiredTokenTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        ErrorResponseModel errorResponse = step("Выполнить запрос на удаление с недействительным токеном", () ->
                given(noBodyRequestSpec)
                        .header("Authorization", "Bearer " + data.getRandomString(170))
                        .when()
                        .delete("/Account/v1/User/" + registrationResponse.getUserId())
                        .then()
                        .spec(getResponseSpecByStatusCode(401))
                        .extract().as(ErrorResponseModel.class)
        );
        step("Проверить значение атрибута code в ответе с ошибкой", () -> {
            assertThat(errorResponse.getCode()).isEqualTo("1200");
        });
        step("Проверить значение атрибута message в ответе с ошибкой", () -> {
            assertThat(errorResponse.getMessage()).isEqualTo("User not authorized!");
        });
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("POSITIVE")
    @Story("Генерация токена")
    @Test
    @DisplayName("Успешная генерация токена для пользователя")
    void successfulGenerationTokenTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Выполнить запрос на генерацию токена", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/GenerateToken")
                        .then()
                        .spec(getResponseSpecByStatusCode(200))
                        .extract().as(GenerateTokenResponseModel.class)
        );
        step("Проверить, что в ответе присутствует атрибут token с непустым значением", () ->
                assertThat(generateTokenResponse.getToken()).isNotBlank()
        );
        step("Проверить, что в ответе присутствует атрибут expires, значение которого соответствует паттерну yyy-MM-ddTHH:mm:ss.SSSZ", () ->
                assertThat(generateTokenResponse.getExpires()).matches(data.getDateTimePattern())
        );
        step("Проверить, что в ответе присутствует атрибут status со значением 'Success'", () ->
                assertThat(generateTokenResponse.getStatus()).isEqualTo("Success")
        );
        step("Проверить, что в ответе присутствует атрибут result с текстовым описанием результата запроса", () ->
                assertThat(generateTokenResponse.getResult()).isEqualTo("User authorized successfully.")
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenResponse.getToken())
        );
    }

    @Tag("NEGATIVE")
    @Story("Генерация токена")
    @MethodSource("emptyDataProviderForLoginRequest")
    @ParameterizedTest
    @DisplayName("Неуспешная генерация токена. В запросе отсутствуют имя или пароль")
    void unsuccessfulGenerationTokenEmptyUserNameOrPasswordTest(LoginRequestModel request) {
        ErrorResponseModel errorResponse = step("Выполнить запрос на генерацию токена", () ->
                given(withBodyRequestSpec)
                        .body(request)
                        .when()
                        .post("/Account/v1/GenerateToken")
                        .then()
                        .spec(getResponseSpecByStatusCode(400))
                        .extract().as(ErrorResponseModel.class)
        );
        step("Проверить значение атрибута code в ответе с ошибкой", () -> {
            assertThat(errorResponse.getCode()).isEqualTo("1200");
        });
        step("Проверить значение атрибута message в ответе с ошибкой", () -> {
            assertThat(errorResponse.getMessage()).isEqualTo("UserName and Password required.");
        });
    }

    @Tag("NEGATIVE")
    @Story("Генерация токена")
    @Test
    @DisplayName("Неуспешная генерация токена. В запросе некорректное имя")
    void unsuccessfulGenerationTokenInvalidUserNameTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Выполнить запрос на генерацию токена", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(data.getRandomString(10), password))
                        .when()
                        .post("/Account/v1/GenerateToken")
                        .then()
                        .spec(getResponseSpecByStatusCode(200))
                        .extract().as(GenerateTokenResponseModel.class)
        );
        step("Проверить, что в ответе отсутствует атрибут token", () ->
                assertThat(generateTokenResponse.getToken()).isNull()
        );
        step("Проверить, что в ответе отсутствует атрибут expires", () ->
                assertThat(generateTokenResponse.getExpires()).isNull()
        );
        step("Проверить, что в ответе присутствует атрибут status со значением 'Failed'", () ->
                assertThat(generateTokenResponse.getStatus()).isEqualTo("Failed")
        );
        step("Проверить, что в ответе присутствует атрибут result с текстовым описанием результата запроса", () ->
                assertThat(generateTokenResponse.getResult()).isEqualTo("User authorization failed.")
        );
        step("Удалить созданного пользователя", () -> {
            GenerateTokenResponseModel generateTokenSuccessResponse = RegistrationApi.generateToken(userName, password);
            RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenSuccessResponse.getToken());
        });
    }

    @Tag("NEGATIVE")
    @Story("Генерация токена")
    @Test
    @DisplayName("Неуспешная генерация токена. В запросе некорректный пароль")
    void unsuccessfulGenerationTokenInvalidPasswordTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Выполнить запрос на генерацию токена", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, data.getRandomString(10)))
                        .when()
                        .post("/Account/v1/GenerateToken")
                        .then()
                        .spec(getResponseSpecByStatusCode(200))
                        .extract().as(GenerateTokenResponseModel.class)
        );
        step("Проверить, что в ответе отсутствует атрибут token", () ->
                assertThat(generateTokenResponse.getToken()).isNull()
        );
        step("Проверить, что в ответе отсутствует атрибут expires", () ->
                assertThat(generateTokenResponse.getExpires()).isNull()
        );
        step("Проверить, что в ответе присутствует атрибут status со значением 'Failed'", () ->
                assertThat(generateTokenResponse.getStatus()).isEqualTo("Failed")
        );
        step("Проверить, что в ответе присутствует атрибут result с текстовым описанием результата запроса", () ->
                assertThat(generateTokenResponse.getResult()).isEqualTo("User authorization failed.")
        );
        step("Удалить созданного пользователя", () -> {
            GenerateTokenResponseModel generateTokenSuccessResponse = RegistrationApi.generateToken(userName, password);
            RegistrationApi.deleteUser(registrationResponse.getUserId(), generateTokenSuccessResponse.getToken());
        });
    }

    @Tag("POSITIVE")
    @Story("Авторизация пользователя")
    @Test
    @DisplayName("Уcпешная авторизация")
    void successfulAuthorizationTest() {
        String userName = data.getUserName();
        String password = data.getPassword();
        RegistrationResponseModel registrationResponse = step("Создать нового пользователя", () ->
                RegistrationApi.registerUser(userName, password)
        );
        GenerateTokenResponseModel generateTokenResponse = step("Сгенерировать для пользователя токен доступа", () ->
                RegistrationApi.generateToken(userName, password)
        );
        LoginResponseModel loginResponse = step("Выполнить запрос на авторизацию", () ->
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/Login")
                        .then()
                        .spec(getResponseSpecByStatusCode(200))
                        .extract().as(LoginResponseModel.class)
        );
        step("Проверить, что в ответе присутствует атрибут userID и он равен id созданного пользователя", () ->
                assertThat(loginResponse.getUserId()).isEqualTo(registrationResponse.getUserId())
        );
        step("Проверить, что в ответе присутствует атрибут username и он равен userName из запроса", () ->
                assertThat(loginResponse.getUsername()).isEqualTo(userName)
        );
        step("Проверить, что в ответе присутствует атрибут password и он равен password из запроса", () ->
                assertThat(loginResponse.getPassword()).isEqualTo(password)
        );
        step("Проверить, что в ответе присутствует атрибут token c непустым значением", () ->
                assertThat(loginResponse.getToken()).isNotBlank()
        );
        step("Проверить, что в ответе присутствует атрибут expires, значение которого соответствует паттерну yyy-MM-ddTHH:mm:ss.SSSZ", () ->
                assertThat(loginResponse.getExpires()).matches(data.getDateTimePattern())
        );
        step("Проверить, что в ответе присутствует атрибут created_date, значение которого соответствует паттерну yyy-MM-ddTHH:mm:ss.SSSZ", () ->
                assertThat(loginResponse.getCreated_date()).matches(data.getDateTimePattern())
        );
        step("Удалить созданного пользователя", () ->
                RegistrationApi.deleteUser(registrationResponse.getUserId(), loginResponse.getToken())
        );
    }
}
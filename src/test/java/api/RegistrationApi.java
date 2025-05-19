package api;

import models.*;

import static io.restassured.RestAssured.given;
import static specs.DemoqaSpecs.*;

public class RegistrationApi {

    public static RegistrationResponseModel registerUser(String userName, String password) {
        return
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/User")
                        .then()
                        .spec(getResponseSpecByStatusCode(201))
                        .extract().as(RegistrationResponseModel.class);
    }

    public static GenerateTokenResponseModel generateToken(String userName, String password) {
        return
                given(withBodyRequestSpec)
                        .body(new LoginRequestModel(userName, password))
                        .when()
                        .post("/Account/v1/GenerateToken")
                        .then()
                        .spec(getResponseSpecByStatusCode(200))
                        .extract().as(GenerateTokenResponseModel.class);
    }

    public static void deleteUser(String userId, String token) {
        given(noBodyRequestSpec)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/Account/v1/User/" + userId)
                .then()
                .spec(getResponseSpecByStatusCode(204));
    }
}
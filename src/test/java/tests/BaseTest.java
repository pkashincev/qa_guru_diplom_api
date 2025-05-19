package tests;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public class BaseTest {

    @BeforeAll
    static void setConfig() {
        RestAssured.baseURI = "https://demoqa.com";
    }
}
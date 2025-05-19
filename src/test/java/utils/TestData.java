package utils;

import com.github.javafaker.Faker;

import java.util.Locale;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public class TestData {
    private final Faker faker = new Faker(new Locale("en-GB"));

    public String getUserName() {
        return faker.regexify("auto_user_\\d{1,}");
    }

    public String getPassword() {
        return faker.regexify("[a-z]{6}\\d[A-Z][!@#$%^&*]");
    }

    public String getRandomString(int length) {
        return randomAlphanumeric(length);
    }

    public String getRandomUUID() {
        return faker.internet().uuid();
    }

    public Pattern getUUIDPattern() {
        return Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    public Pattern getDateTimePattern() {
        return Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$");
    }
}
package com.sportmate.app.presentation.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.R;
import com.sportmate.app.presentation.common.UiText;

import org.junit.Test;

import java.util.Map;

/** Mirrors the backend's UserInputRules limits. */
public class UserInputRulesTest {
    @Test
    public void validRegistrationHasNoErrors() {
        assertTrue(UserInputRules.validateRegister("a@example.com", "secret-pass1", "Alex", "+84 (912) 345-678").isEmpty());
        assertTrue(UserInputRules.validateRegister("a@example.com", "secret-pass1", "Alex", "").isEmpty());
    }

    @Test
    public void emailRules() {
        assertEquals(UiText.of(R.string.error_email_required), registerError("   ", UserInputRules.FIELD_EMAIL));
        assertEquals(UiText.of(R.string.error_email_invalid), registerError("a@localhost", UserInputRules.FIELD_EMAIL));
        assertEquals(UiText.of(R.string.error_email_invalid), registerError("a b@example.com", UserInputRules.FIELD_EMAIL));
        assertEquals(UiText.of(R.string.error_email_invalid), registerError("a@example.com.", UserInputRules.FIELD_EMAIL));
        String tooLong = repeat('a', 243) + "@example.com";
        assertEquals(UiText.of(R.string.error_email_too_long), registerError(tooLong, UserInputRules.FIELD_EMAIL));
    }

    @Test
    public void passwordRules() {
        assertEquals(UiText.of(R.string.error_password_required), passwordError(""));
        assertEquals(UiText.of(R.string.error_password_length), passwordError("abc1234"));
        assertEquals(UiText.of(R.string.error_password_length), passwordError(repeat('a', 128) + "1"));
        assertEquals(UiText.of(R.string.error_password_letter_digit), passwordError("abcdefgh"));
        assertEquals(UiText.of(R.string.error_password_letter_digit), passwordError("12345678"));
        assertNull(passwordError("abcdefg1"));
    }

    @Test
    public void displayNameRules() {
        assertEquals(UiText.of(R.string.error_display_name_required), displayNameError("  "));
        assertEquals(UiText.of(R.string.error_display_name_too_long), displayNameError(repeat('x', 101)));
        assertNull(displayNameError("  " + repeat('x', 100) + "  "));
    }

    @Test
    public void phoneRules() {
        UiText invalid = UiText.of(R.string.error_phone_invalid);
        assertEquals(invalid, phoneError("123456"));
        assertEquals(invalid, phoneError("1234567890123456"));
        assertEquals(invalid, phoneError("12345abc"));
        assertEquals(invalid, phoneError("+1 (234) 567-8901-2345"));
        assertNull(phoneError("+1234567"));
        assertNull(phoneError("   "));
    }

    @Test
    public void profileUsesTheSameDisplayNameAndPhoneRules() {
        assertTrue(UserInputRules.validateProfile("Alex", "").isEmpty());
        assertTrue(UserInputRules.validateProfile("Alex", "+84 912 345 678").isEmpty());
        Map<String, UiText> errors = UserInputRules.validateProfile(" ", "123");
        assertEquals(UiText.of(R.string.error_display_name_required), errors.get(UserInputRules.FIELD_DISPLAY_NAME));
        assertEquals(UiText.of(R.string.error_phone_invalid), errors.get(UserInputRules.FIELD_PHONE));
        assertEquals(2, errors.size());
    }

    @Test
    public void loginOnlyRequiresPresence() {
        assertTrue(UserInputRules.validateLogin("anything", "x").isEmpty());
        Map<String, UiText> errors = UserInputRules.validateLogin(null, null);
        assertEquals(2, errors.size());
    }

    private static UiText registerError(String email, String field) {
        return UserInputRules.validateRegister(email, "secret-pass1", "Alex", null).get(field);
    }

    private static UiText passwordError(String password) {
        return UserInputRules.validateRegister("a@example.com", password, "Alex", null).get(UserInputRules.FIELD_PASSWORD);
    }

    private static UiText displayNameError(String displayName) {
        return UserInputRules.validateRegister("a@example.com", "secret-pass1", displayName, null).get(UserInputRules.FIELD_DISPLAY_NAME);
    }

    private static UiText phoneError(String phone) {
        return UserInputRules.validateRegister("a@example.com", "secret-pass1", "Alex", phone).get(UserInputRules.FIELD_PHONE);
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}

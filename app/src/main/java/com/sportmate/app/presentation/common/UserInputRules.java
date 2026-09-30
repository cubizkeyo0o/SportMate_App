package com.sportmate.app.presentation.common;

import androidx.annotation.Nullable;

import com.sportmate.app.R;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Immediate client-side checks for obvious mistakes, mirroring the backend's UserInputRules. The
 * backend stays authoritative. The limits also appear in the error strings; keep them in sync. Field keys are the backend's snake_case request field names, so
 * client and backend validation errors land on the same inputs.
 */
public final class UserInputRules {
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_DISPLAY_NAME = "display_name";
    public static final String FIELD_PHONE = "phone";

    static final int EMAIL_MAX_LENGTH = 254;
    static final int PASSWORD_MIN_LENGTH = 8;
    static final int PASSWORD_MAX_LENGTH = 128;
    static final int DISPLAY_NAME_MAX_LENGTH = 100;
    static final int PHONE_MAX_LENGTH = 20;
    static final int PHONE_MIN_DIGITS = 7;
    static final int PHONE_MAX_DIGITS = 15;

    // Deliberately loose: catches obvious typos; the backend does the full check.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private UserInputRules() {
    }

    public static String normalizeEmail(@Nullable String email) {
        return email == null ? "" : email.trim();
    }

    /** Blank or whitespace-only input means "no phone", as on the backend. */
    @Nullable
    public static String normalizePhone(@Nullable String phone) {
        String trimmed = phone == null ? "" : phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static Map<String, UiText> validateLogin(@Nullable String email, @Nullable String password) {
        Map<String, UiText> errors = new LinkedHashMap<>();
        if (normalizeEmail(email).isEmpty()) {
            errors.put(FIELD_EMAIL, UiText.of(R.string.error_email_required));
        }
        if (password == null || password.isEmpty()) {
            errors.put(FIELD_PASSWORD, UiText.of(R.string.error_password_required));
        }
        return errors;
    }

    public static Map<String, UiText> validateRegister(
            @Nullable String email,
            @Nullable String password,
            @Nullable String displayName,
            @Nullable String phone) {
        Map<String, UiText> errors = new LinkedHashMap<>();
        putIfInvalid(errors, FIELD_EMAIL, email(email));
        putIfInvalid(errors, FIELD_PASSWORD, newPassword(password));
        putIfInvalid(errors, FIELD_DISPLAY_NAME, displayName(displayName));
        putIfInvalid(errors, FIELD_PHONE, phone(phone));
        return errors;
    }

    /** Editable profile fields (PUT users/me); the backend applies the same rules as registration. */
    public static Map<String, UiText> validateProfile(@Nullable String displayName, @Nullable String phone) {
        Map<String, UiText> errors = new LinkedHashMap<>();
        putIfInvalid(errors, FIELD_DISPLAY_NAME, displayName(displayName));
        putIfInvalid(errors, FIELD_PHONE, phone(phone));
        return errors;
    }

    @Nullable
    private static UiText email(@Nullable String value) {
        String email = normalizeEmail(value);
        if (email.isEmpty()) {
            return UiText.of(R.string.error_email_required);
        }
        if (email.length() > EMAIL_MAX_LENGTH) {
            return UiText.of(R.string.error_email_too_long);
        }
        if (!EMAIL.matcher(email).matches() || email.endsWith(".")) {
            return UiText.of(R.string.error_email_invalid);
        }
        return null;
    }

    @Nullable
    private static UiText newPassword(@Nullable String password) {
        if (password == null || password.isEmpty()) {
            return UiText.of(R.string.error_password_required);
        }
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            return UiText.of(R.string.error_password_length);
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            hasLetter |= Character.isLetter(c);
            hasDigit |= Character.isDigit(c);
        }
        if (!hasLetter || !hasDigit) {
            return UiText.of(R.string.error_password_letter_digit);
        }
        return null;
    }

    @Nullable
    private static UiText displayName(@Nullable String value) {
        String displayName = value == null ? "" : value.trim();
        if (displayName.isEmpty()) {
            return UiText.of(R.string.error_display_name_required);
        }
        if (displayName.length() > DISPLAY_NAME_MAX_LENGTH) {
            return UiText.of(R.string.error_display_name_too_long);
        }
        return null;
    }

    @Nullable
    private static UiText phone(@Nullable String value) {
        String phone = normalizePhone(value);
        if (phone == null) {
            return null;
        }
        String body = phone.startsWith("+") ? phone.substring(1) : phone;
        int digits = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c >= '0' && c <= '9') {
                digits++;
            } else if (c != ' ' && c != '-' && c != '(' && c != ')' && c != '.') {
                return phoneError();
            }
        }
        if (phone.length() > PHONE_MAX_LENGTH || digits < PHONE_MIN_DIGITS || digits > PHONE_MAX_DIGITS) {
            return phoneError();
        }
        return null;
    }

    private static UiText phoneError() {
        return UiText.of(R.string.error_phone_invalid);
    }

    private static void putIfInvalid(Map<String, UiText> errors, String field, @Nullable UiText error) {
        if (error != null) {
            errors.put(field, error);
        }
    }
}

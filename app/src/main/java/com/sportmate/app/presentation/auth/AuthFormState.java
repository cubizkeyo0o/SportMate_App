package com.sportmate.app.presentation.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.presentation.common.UiText;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * State of a login or register form: whether a submit is in flight, errors per input (keyed like
 * {@link AuthInputRules}) and an error not tied to an input, rendered with ErrorMessages.
 */
public final class AuthFormState {
    private final boolean loading;
    private final Map<String, UiText> fieldErrors;
    @Nullable
    private final AppError error;

    private AuthFormState(boolean loading, Map<String, UiText> fieldErrors, @Nullable AppError error) {
        this.loading = loading;
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
        this.error = error;
    }

    public static AuthFormState idle() {
        return new AuthFormState(false, Collections.emptyMap(), null);
    }

    public static AuthFormState loading() {
        return new AuthFormState(true, Collections.emptyMap(), null);
    }

    public static AuthFormState invalid(@NonNull Map<String, UiText> fieldErrors) {
        return new AuthFormState(false, fieldErrors, null);
    }

    /**
     * Places backend validation messages on the form's inputs. Anything that cannot be shown on an
     * input (another error kind, or a field this form lacks) becomes the general error.
     */
    public static AuthFormState fromError(@NonNull AppError error, @NonNull Set<String> formFields) {
        if (error.getKind() != AppError.Kind.VALIDATION || error.getFieldErrors().isEmpty()) {
            return new AuthFormState(false, Collections.emptyMap(), error);
        }

        Map<String, UiText> fieldErrors = new LinkedHashMap<>();
        boolean unplaced = false;
        for (Map.Entry<String, List<String>> entry : error.getFieldErrors().entrySet()) {
            List<String> messages = entry.getValue();
            if (formFields.contains(entry.getKey()) && messages != null && !messages.isEmpty()) {
                fieldErrors.put(entry.getKey(), UiText.raw(join(messages)));
            } else {
                unplaced = true;
            }
        }
        return new AuthFormState(false, fieldErrors, unplaced || fieldErrors.isEmpty() ? error : null);
    }

    /** The same state with one input's error removed, e.g. once the user edits that input. */
    public AuthFormState withoutFieldError(@NonNull String field) {
        if (!fieldErrors.containsKey(field)) {
            return this;
        }
        Map<String, UiText> remaining = new LinkedHashMap<>(fieldErrors);
        remaining.remove(field);
        return new AuthFormState(loading, remaining, error);
    }

    public boolean isLoading() {
        return loading;
    }

    @NonNull
    public Map<String, UiText> getFieldErrors() {
        return fieldErrors;
    }

    @Nullable
    public UiText getFieldError(@NonNull String field) {
        return fieldErrors.get(field);
    }

    @Nullable
    public AppError getError() {
        return error;
    }

    private static String join(List<String> messages) {
        StringBuilder builder = new StringBuilder();
        for (String message : messages) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(message);
        }
        return builder.toString();
    }
}

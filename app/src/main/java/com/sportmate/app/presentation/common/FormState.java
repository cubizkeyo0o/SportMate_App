package com.sportmate.app.presentation.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sportmate.app.domain.common.AppError;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * State of a form: whether a submit is in flight, errors per input (keyed like
 * {@link UserInputRules}) and an error not tied to an input, rendered with ErrorMessages.
 */
public final class FormState {
    private final boolean loading;
    private final Map<String, UiText> fieldErrors;
    @Nullable
    private final AppError error;

    private FormState(boolean loading, Map<String, UiText> fieldErrors, @Nullable AppError error) {
        this.loading = loading;
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
        this.error = error;
    }

    public static FormState idle() {
        return new FormState(false, Collections.emptyMap(), null);
    }

    public static FormState loading() {
        return new FormState(true, Collections.emptyMap(), null);
    }

    public static FormState invalid(@NonNull Map<String, UiText> fieldErrors) {
        return new FormState(false, fieldErrors, null);
    }

    /**
     * Places backend validation messages on the form's inputs. Anything that cannot be shown on an
     * input (another error kind, or a field this form lacks) becomes the general error.
     */
    public static FormState fromError(@NonNull AppError error, @NonNull Set<String> formFields) {
        if (error.getKind() != AppError.Kind.VALIDATION || error.getFieldErrors().isEmpty()) {
            return new FormState(false, Collections.emptyMap(), error);
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
        return new FormState(false, fieldErrors, unplaced || fieldErrors.isEmpty() ? error : null);
    }

    /** The same state with one input's error removed, e.g. once the user edits that input. */
    public FormState withoutFieldError(@NonNull String field) {
        if (!fieldErrors.containsKey(field)) {
            return this;
        }
        Map<String, UiText> remaining = new LinkedHashMap<>(fieldErrors);
        remaining.remove(field);
        return new FormState(loading, remaining, error);
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

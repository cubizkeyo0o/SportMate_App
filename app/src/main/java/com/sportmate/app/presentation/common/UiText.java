package com.sportmate.app.presentation.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Arrays;

/**
 * Text a ViewModel can hold without a Context: a string resource (with format arguments) or text
 * received from the backend.
 */
public final class UiText {
    @StringRes
    private final int resId;
    private final Object[] args;
    @Nullable
    private final String text;

    private UiText(@StringRes int resId, Object[] args, @Nullable String text) {
        this.resId = resId;
        this.args = args;
        this.text = text;
    }

    public static UiText of(@StringRes int resId, Object... args) {
        return new UiText(resId, args, null);
    }

    public static UiText raw(@NonNull String text) {
        return new UiText(0, new Object[0], text);
    }

    @NonNull
    public String resolve(@NonNull Context context) {
        return text != null ? text : context.getString(resId, args);
    }

    /** The string resource, or 0 for backend text. */
    @StringRes
    public int getResId() {
        return resId;
    }

    @Nullable
    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UiText)) {
            return false;
        }
        UiText that = (UiText) other;
        return resId == that.resId
                && Arrays.equals(args, that.args)
                && (text == null ? that.text == null : text.equals(that.text));
    }

    @Override
    public int hashCode() {
        return 31 * (31 * resId + Arrays.hashCode(args)) + (text == null ? 0 : text.hashCode());
    }
}

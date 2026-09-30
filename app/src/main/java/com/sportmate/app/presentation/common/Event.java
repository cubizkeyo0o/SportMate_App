package com.sportmate.app.presentation.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * LiveData payload for one-shot actions such as navigation, so they are not replayed when a
 * Fragment's view is recreated.
 */
public final class Event<T> {
    private final T content;
    private boolean handled;

    public Event(@NonNull T content) {
        this.content = content;
    }

    /** The content the first time it is requested, then {@code null}. */
    @Nullable
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }
}

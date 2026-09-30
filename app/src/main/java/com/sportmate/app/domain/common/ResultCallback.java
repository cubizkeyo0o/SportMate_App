package com.sportmate.app.domain.common;

import androidx.annotation.NonNull;

/**
 * Receives the outcome of an asynchronous repository operation. Exactly one method is called,
 * on the main thread, unless the operation was cancelled first.
 */
public interface ResultCallback<T> {
    void onSuccess(T result);

    void onError(@NonNull AppError error);
}

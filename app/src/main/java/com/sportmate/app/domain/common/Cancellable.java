package com.sportmate.app.domain.common;

/**
 * Handle to an in-flight operation. ViewModels cancel outstanding work in {@code onCleared()}.
 */
public interface Cancellable {
    void cancel();
}

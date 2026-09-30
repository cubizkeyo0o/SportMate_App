package com.sportmate.app.testutil;

import androidx.annotation.NonNull;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.ResultCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Waits for an asynchronous repository result in JVM tests. */
public final class BlockingCallback<T> implements ResultCallback<T> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile T result;
    private volatile AppError error;

    @Override
    public void onSuccess(T result) {
        this.result = result;
        latch.countDown();
    }

    @Override
    public void onError(@NonNull AppError error) {
        this.error = error;
        latch.countDown();
    }

    public BlockingCallback<T> await() throws InterruptedException {
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("callback was not called");
        }
        return this;
    }

    public T result() {
        if (error != null) {
            throw new AssertionError("expected success but got " + error);
        }
        return result;
    }

    public AppError error() {
        if (error == null) {
            throw new AssertionError("expected an error but the call succeeded");
        }
        return error;
    }
}

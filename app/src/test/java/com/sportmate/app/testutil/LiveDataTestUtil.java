package com.sportmate.app.testutil;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Waits for LiveData updated from OkHttp threads (requires InstantTaskExecutorRule). */
public final class LiveDataTestUtil {
    public interface Condition<T> {
        boolean test(T value);
    }

    private LiveDataTestUtil() {
    }

    public static <T> T await(LiveData<T> liveData, Condition<T> condition) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = value -> {
            if (value != null && condition.test(value)) {
                latch.countDown();
            }
        };
        liveData.observeForever(observer);
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("condition not reached; last value " + liveData.getValue());
            }
            return liveData.getValue();
        } finally {
            liveData.removeObserver(observer);
        }
    }
}

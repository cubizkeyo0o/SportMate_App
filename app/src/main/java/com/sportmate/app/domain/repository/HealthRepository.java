package com.sportmate.app.domain.repository;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;

public interface HealthRepository {
    /** Delivers {@code true} when the backend reports itself healthy. */
    Cancellable checkHealth(ResultCallback<Boolean> callback);
}

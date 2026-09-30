package com.sportmate.app.domain.repository;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Sport;

import java.util.List;

public interface SportRepository {
    /** Active sports a user can pick. */
    Cancellable getSports(ResultCallback<List<Sport>> callback);
}

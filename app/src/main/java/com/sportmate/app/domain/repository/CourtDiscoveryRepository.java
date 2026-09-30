package com.sportmate.app.domain.repository;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.OperatingHour;

import java.util.List;

/**
 * Courts any signed-in user can browse. Only active courts exist here; a court that is inactive or
 * unknown is reported as NOT_FOUND.
 */
public interface CourtDiscoveryRepository {
    /** Ordered by name. */
    Cancellable getCourts(ResultCallback<List<Court>> callback);

    Cancellable getCourt(String courtId, ResultCallback<Court> callback);

    /** Sports the court offers now, ordered by sport name. */
    Cancellable getCourtSports(String courtId, ResultCallback<List<CourtSport>> callback);

    /** Weekly periods, ordered by day (Sunday first) then opening time. */
    Cancellable getOperatingHours(String courtId, ResultCallback<List<OperatingHour>> callback);
}

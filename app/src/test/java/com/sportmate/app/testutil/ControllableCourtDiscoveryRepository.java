package com.sportmate.app.testutil;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.CourtStatus;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.repository.CourtDiscoveryRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Records calls and keeps each callback so a test can complete it. Used for ViewModel state tests;
 * HTTP behaviour is covered against MockWebServer.
 */
public final class ControllableCourtDiscoveryRepository implements CourtDiscoveryRepository {
    public int courtsCalls;
    public int courtCalls;
    public int sportsCalls;
    public int hoursCalls;
    public int cancelled;
    public String lastCourtId;

    public ResultCallback<List<Court>> courtsCallback;
    public ResultCallback<Court> courtCallback;
    public ResultCallback<List<CourtSport>> sportsCallback;
    public ResultCallback<List<OperatingHour>> hoursCallback;

    public static Court court(String id, String name) {
        return new Court(id, name, null, "12 Tran Hung Dao", "Hanoi",
                new BigDecimal("21.028511"), new BigDecimal("105.804817"), "Asia/Ho_Chi_Minh", CourtStatus.ACTIVE);
    }

    @Override
    public Cancellable getCourts(ResultCallback<List<Court>> callback) {
        courtsCalls++;
        courtsCallback = callback;
        return () -> cancelled++;
    }

    @Override
    public Cancellable getCourt(String courtId, ResultCallback<Court> callback) {
        courtCalls++;
        lastCourtId = courtId;
        courtCallback = callback;
        return () -> cancelled++;
    }

    @Override
    public Cancellable getCourtSports(String courtId, ResultCallback<List<CourtSport>> callback) {
        sportsCalls++;
        lastCourtId = courtId;
        sportsCallback = callback;
        return () -> cancelled++;
    }

    @Override
    public Cancellable getOperatingHours(String courtId, ResultCallback<List<OperatingHour>> callback) {
        hoursCalls++;
        lastCourtId = courtId;
        hoursCallback = callback;
        return () -> cancelled++;
    }
}

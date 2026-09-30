package com.sportmate.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.data.remote.api.CourtDiscoveryApi;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.CourtStatus;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.Weekday;
import com.sportmate.app.testutil.BlockingCallback;
import com.sportmate.app.testutil.FakeTokenStorage;
import com.sportmate.app.testutil.TestNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/** api/v1/discovery/courts against the backend's JSON shapes and status codes. */
public class CourtDiscoveryRepositoryImplTest {
    private static final String COURT_ID = "7d4e2a10-5b3c-4f1e-9a8b-0c1d2e3f4a5b";

    private MockWebServer server;
    private CourtDiscoveryRepositoryImpl repository;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        CourtDiscoveryApi api = TestNetwork.retrofit(server, new FakeTokenStorage("access-1", "refresh-1"))
                .create(CourtDiscoveryApi.class);
        repository = new CourtDiscoveryRepositoryImpl(api, TestNetwork.executor());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    // --- List ---

    @Test
    public void listMapsSnakeCaseCourtsWithBearerToken() throws Exception {
        server.enqueue(json("[" + court(COURT_ID, "Court A", "\"Indoor, lit\"") + ","
                + court("8e5f3b21-6c4d-4a2f-8b9c-1d2e3f4a5b6c", "Court B", "null") + "]"));

        List<Court> courts = courts().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/discovery/courts", request.getPath());
        assertEquals("Bearer access-1", request.getHeader("Authorization"));
        assertEquals(2, courts.size());
        Court first = courts.get(0);
        assertEquals(COURT_ID, first.getId());
        assertEquals("Court A", first.getName());
        assertEquals("Indoor, lit", first.getDescription());
        assertEquals("12 Tran Hung Dao", first.getAddress());
        assertEquals("Hanoi", first.getCity());
        assertEquals(new BigDecimal("21.028511"), first.getLatitude());
        assertEquals(new BigDecimal("105.804817"), first.getLongitude());
        assertEquals("Asia/Ho_Chi_Minh", first.getTimeZoneId());
        assertEquals(CourtStatus.ACTIVE, first.getStatus());
        assertNull(courts.get(1).getDescription());
    }

    @Test
    public void emptyListIsSuccess() throws Exception {
        server.enqueue(json("[]"));

        assertTrue(courts().result().isEmpty());
    }

    @Test
    public void listFailures() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401).addHeader("WWW-Authenticate", "Bearer"));
        server.enqueue(problem(400, "{\"title\":\"One or more validation errors occurred.\",\"status\":400,"
                + "\"errors\":{\"x\":[\"Bad.\"]}}"));
        server.enqueue(problem(403, "{\"title\":\"This account is not active.\",\"status\":403}"));
        server.enqueue(new MockResponse().setResponseCode(503));

        assertEquals(AppError.Kind.UNAUTHORIZED, courts().error().getKind());
        assertEquals(AppError.Kind.VALIDATION, courts().error().getKind());
        assertEquals(AppError.Kind.FORBIDDEN, courts().error().getKind());
        assertEquals(AppError.Kind.SERVER, courts().error().getKind());

        server.shutdown();
        assertEquals(AppError.Kind.NETWORK, courts().error().getKind());
    }

    // --- Detail ---

    @Test
    public void detailLoadsCourtById() throws Exception {
        server.enqueue(json(court(COURT_ID, "Court A", "null")));

        BlockingCallback<Court> callback = new BlockingCallback<>();
        repository.getCourt(COURT_ID, callback);
        Court court = callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/discovery/courts/" + COURT_ID, request.getPath());
        assertEquals("Court A", court.getName());
    }

    @Test
    public void inactiveOrUnknownCourtIsNotFound() throws Exception {
        server.enqueue(problem(404, "{\"title\":\"Court not found.\",\"status\":404}"));

        BlockingCallback<Court> callback = new BlockingCallback<>();
        repository.getCourt(COURT_ID, callback);
        AppError error = callback.await().error();

        assertEquals(AppError.Kind.NOT_FOUND, error.getKind());
        assertEquals("Court not found.", error.getMessage());
    }

    @Test
    public void incompleteCourtIsAnUnreadableResponse() throws Exception {
        server.enqueue(json("{\"id\":\"" + COURT_ID + "\",\"name\":\"Court A\"}"));

        BlockingCallback<Court> callback = new BlockingCallback<>();
        repository.getCourt(COURT_ID, callback);

        assertEquals(AppError.Kind.UNKNOWN, callback.await().error().getKind());
    }

    // --- Sports ---

    @Test
    public void sportsMapExactPriceAndCurrency() throws Exception {
        server.enqueue(json("[{\"court_sport_id\":\"cs-1\",\"sport\":{\"id\":\"s-1\",\"name\":\"Badminton\",\"slug\":\"badminton\"},"
                + "\"price_per_hour\":150000.00,\"currency\":\"VND\",\"is_active\":true},"
                + "{\"court_sport_id\":\"cs-2\",\"sport\":{\"id\":\"s-2\",\"name\":\"Tennis\",\"slug\":\"tennis\"},"
                + "\"price_per_hour\":12.75,\"currency\":\"USD\",\"is_active\":true}]"));

        BlockingCallback<List<CourtSport>> callback = new BlockingCallback<>();
        repository.getCourtSports(COURT_ID, callback);
        List<CourtSport> sports = callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/discovery/courts/" + COURT_ID + "/sports", request.getPath());
        assertEquals(2, sports.size());
        assertEquals("cs-1", sports.get(0).getId());
        assertEquals("s-1", sports.get(0).getSport().getId());
        assertEquals("Badminton", sports.get(0).getSport().getName());
        assertEquals("badminton", sports.get(0).getSport().getSlug());
        assertEquals(new BigDecimal("150000.00"), sports.get(0).getPricePerHour());
        assertEquals("VND", sports.get(0).getCurrency());
        assertEquals(new BigDecimal("12.75"), sports.get(1).getPricePerHour());
        assertEquals("USD", sports.get(1).getCurrency());
    }

    @Test
    public void sportsOfUnknownCourtAreNotFound() throws Exception {
        server.enqueue(problem(404, "{\"title\":\"Court not found.\",\"status\":404}"));

        BlockingCallback<List<CourtSport>> callback = new BlockingCallback<>();
        repository.getCourtSports(COURT_ID, callback);

        assertEquals(AppError.Kind.NOT_FOUND, callback.await().error().getKind());
    }

    // --- Operating hours ---

    @Test
    public void operatingHoursKeepLocalTimesAndMapEveryDay() throws Exception {
        StringBuilder body = new StringBuilder("[");
        for (int day = 0; day <= 6; day++) {
            body.append(day == 0 ? "" : ",").append(hour(day, "08:00", "22:00"));
        }
        body.append(",").append(hour(6, "23:00", "23:59")).append("]");
        server.enqueue(json(body.toString()));

        List<OperatingHour> hours = operatingHours().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/discovery/courts/" + COURT_ID + "/operating-hours", request.getPath());
        assertEquals(8, hours.size());
        Weekday[] expected = {Weekday.SUNDAY, Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY,
                Weekday.THURSDAY, Weekday.FRIDAY, Weekday.SATURDAY};
        for (int day = 0; day <= 6; day++) {
            assertEquals(expected[day], hours.get(day).getDay());
            assertEquals("08:00", hours.get(day).getOpensAt());
            assertEquals("22:00", hours.get(day).getClosesAt());
        }
        assertEquals(Weekday.SATURDAY, hours.get(7).getDay());
        assertEquals("23:00", hours.get(7).getOpensAt());
        assertEquals("23:59", hours.get(7).getClosesAt());
    }

    @Test
    public void emptyOperatingHoursIsSuccess() throws Exception {
        server.enqueue(json("[]"));

        assertTrue(operatingHours().result().isEmpty());
    }

    @Test
    public void unknownDayOrMalformedTimeIsAnUnreadableResponse() throws Exception {
        server.enqueue(json("[" + hour(7, "08:00", "22:00") + "]"));
        server.enqueue(json("[" + hour(1, "8:00", "22:00") + "]"));
        server.enqueue(json("[" + hour(1, "08:00", "24:00") + "]"));

        assertEquals(AppError.Kind.UNKNOWN, operatingHours().error().getKind());
        assertEquals(AppError.Kind.UNKNOWN, operatingHours().error().getKind());
        assertEquals(AppError.Kind.UNKNOWN, operatingHours().error().getKind());
    }

    private BlockingCallback<List<Court>> courts() throws InterruptedException {
        BlockingCallback<List<Court>> callback = new BlockingCallback<>();
        repository.getCourts(callback);
        return callback.await();
    }

    private BlockingCallback<List<OperatingHour>> operatingHours() throws InterruptedException {
        BlockingCallback<List<OperatingHour>> callback = new BlockingCallback<>();
        repository.getOperatingHours(COURT_ID, callback);
        return callback.await();
    }

    private static String court(String id, String name, String descriptionJson) {
        return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"description\":" + descriptionJson + ","
                + "\"address\":\"12 Tran Hung Dao\",\"city\":\"Hanoi\",\"latitude\":21.028511,"
                + "\"longitude\":105.804817,\"time_zone_id\":\"Asia/Ho_Chi_Minh\",\"status\":\"active\"}";
    }

    private static String hour(int day, String opensAt, String closesAt) {
        return "{\"day_of_week\":" + day + ",\"opens_at\":\"" + opensAt + "\",\"closes_at\":\"" + closesAt + "\"}";
    }

    private static MockResponse json(String body) {
        return new MockResponse().addHeader("Content-Type", "application/json").setBody(body);
    }

    private static MockResponse problem(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/problem+json")
                .setBody(body);
    }
}

package com.sportmate.app.presentation.courts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.sportmate.app.data.remote.api.CourtDiscoveryApi;
import com.sportmate.app.data.repository.CourtDiscoveryRepositoryImpl;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.Weekday;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.testutil.FakeTokenStorage;
import com.sportmate.app.testutil.LiveDataTestUtil;
import com.sportmate.app.testutil.TestNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Courts list and court detail through the real repository, Retrofit and OkHttp, down to UI state.
 * Requests are answered by path because the detail screen loads three resources in parallel.
 */
public class CourtsFlowIntegrationTest {
    private static final String COURT_ID = "7d4e2a10-5b3c-4f1e-9a8b-0c1d2e3f4a5b";
    private static final String INACTIVE_ID = "9f6a4c32-7d5e-4b3a-9c0d-2e3f4a5b6c7d";

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MockWebServer server;
    private CourtDiscoveryRepositoryImpl repository;
    private BackendStub backend;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        backend = new BackendStub();
        server.setDispatcher(backend);
        server.start();
        CourtDiscoveryApi api = TestNetwork.retrofit(server, new FakeTokenStorage("access-1", "refresh-1"))
                .create(CourtDiscoveryApi.class);
        repository = new CourtDiscoveryRepositoryImpl(api, TestNetwork.executor());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void listThenDetail() throws Exception {
        CourtsViewModel list = new CourtsViewModel(repository);
        List<Court> courts = LiveDataTestUtil.await(list.getCourts(), s -> s.getStatus() == UiState.Status.SUCCESS).getData();
        assertEquals(1, courts.size());
        assertEquals("Court A", courts.get(0).getName());

        CourtDetailViewModel detail = detailFor(courts.get(0).getId());
        Court court = LiveDataTestUtil.await(detail.getCourt(), s -> s.getStatus() == UiState.Status.SUCCESS).getData();
        List<CourtSport> sports = LiveDataTestUtil.await(detail.getSports(), s -> s.getStatus() == UiState.Status.SUCCESS).getData();
        List<OperatingHour> hours = LiveDataTestUtil.await(detail.getOperatingHours(),
                s -> s.getStatus() == UiState.Status.SUCCESS).getData();

        assertEquals("Asia/Ho_Chi_Minh", court.getTimeZoneId());
        assertEquals(new BigDecimal("150000.00"), sports.get(0).getPricePerHour());
        assertEquals("VND", sports.get(0).getCurrency());
        assertEquals(2, hours.size());
        assertEquals(Weekday.SUNDAY, hours.get(0).getDay());
        assertEquals("08:00", hours.get(0).getOpensAt());
        assertEquals(Weekday.MONDAY, hours.get(1).getDay());
        assertEquals("22:00", hours.get(1).getClosesAt());
    }

    @Test
    public void emptyCatalogueIsTheEmptyState() throws Exception {
        backend.courtsJson = "[]";

        CourtsViewModel list = new CourtsViewModel(repository);

        assertTrue(LiveDataTestUtil.await(list.getCourts(), s -> s.getStatus() == UiState.Status.SUCCESS).getData().isEmpty());
    }

    @Test
    public void inactiveCourtDetailIsNotFound() throws Exception {
        CourtDetailViewModel detail = detailFor(INACTIVE_ID);

        UiState<Court> state = LiveDataTestUtil.await(detail.getCourt(), s -> s.getStatus() == UiState.Status.ERROR);

        assertEquals(AppError.Kind.NOT_FOUND, state.getError().getKind());
    }

    @Test
    public void failedSectionDoesNotAffectTheCourt() throws Exception {
        backend.sportsFail = true;
        CourtDetailViewModel detail = detailFor(COURT_ID);

        LiveDataTestUtil.await(detail.getSports(), s -> s.getStatus() == UiState.Status.ERROR);
        LiveDataTestUtil.await(detail.getCourt(), s -> s.getStatus() == UiState.Status.SUCCESS);
        LiveDataTestUtil.await(detail.getOperatingHours(), s -> s.getStatus() == UiState.Status.SUCCESS);

        backend.sportsFail = false;
        detail.retrySports();
        LiveDataTestUtil.await(detail.getSports(), s -> s.getStatus() == UiState.Status.SUCCESS);
    }

    private CourtDetailViewModel detailFor(String courtId) {
        return new CourtDetailViewModel(repository,
                new SavedStateHandle(Collections.singletonMap(CourtDetailViewModel.ARG_COURT_ID, courtId)));
    }

    /** Answers like the backend discovery endpoints for one active court. */
    private static final class BackendStub extends Dispatcher {
        volatile String courtsJson = "[" + court() + "]";
        volatile boolean sportsFail;

        @Override
        public MockResponse dispatch(RecordedRequest request) {
            if (!"Bearer access-1".equals(request.getHeader("Authorization"))) {
                return new MockResponse().setResponseCode(401);
            }
            if (!"GET".equals(request.getMethod())) {
                return new MockResponse().setResponseCode(405);
            }
            String base = "/api/v1/discovery/courts";
            String path = request.getPath();
            if (base.equals(path)) {
                return json(courtsJson);
            }
            if ((base + "/" + COURT_ID).equals(path)) {
                return json(court());
            }
            if ((base + "/" + COURT_ID + "/sports").equals(path)) {
                return sportsFail
                        ? new MockResponse().setResponseCode(500)
                        : json("[{\"court_sport_id\":\"cs-1\",\"sport\":{\"id\":\"s-1\",\"name\":\"Badminton\",\"slug\":\"badminton\"},"
                                + "\"price_per_hour\":150000.00,\"currency\":\"VND\",\"is_active\":true}]");
            }
            if ((base + "/" + COURT_ID + "/operating-hours").equals(path)) {
                return json("[{\"day_of_week\":0,\"opens_at\":\"08:00\",\"closes_at\":\"20:00\"},"
                        + "{\"day_of_week\":1,\"opens_at\":\"06:00\",\"closes_at\":\"22:00\"}]");
            }
            return new MockResponse().setResponseCode(404)
                    .addHeader("Content-Type", "application/problem+json")
                    .setBody("{\"title\":\"Court not found.\",\"status\":404}");
        }

        private static String court() {
            return "{\"id\":\"" + COURT_ID + "\",\"name\":\"Court A\",\"description\":null,"
                    + "\"address\":\"12 Tran Hung Dao\",\"city\":\"Hanoi\",\"latitude\":21.028511,"
                    + "\"longitude\":105.804817,\"time_zone_id\":\"Asia/Ho_Chi_Minh\",\"status\":\"active\"}";
        }

        private static MockResponse json(String body) {
            return new MockResponse().addHeader("Content-Type", "application/json").setBody(body);
        }
    }
}

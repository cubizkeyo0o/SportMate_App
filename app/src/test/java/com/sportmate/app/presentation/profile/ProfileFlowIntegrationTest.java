package com.sportmate.app.presentation.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.data.remote.api.AuthApi;
import com.sportmate.app.data.remote.api.SportApi;
import com.sportmate.app.data.remote.api.UserApi;
import com.sportmate.app.data.repository.AuthRepositoryImpl;
import com.sportmate.app.data.repository.SportRepositoryImpl;
import com.sportmate.app.data.repository.UserRepositoryImpl;
import com.sportmate.app.data.repository.UserSportProfileRepositoryImpl;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.model.UserSportProfile;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.session.AuthState;
import com.sportmate.app.presentation.session.SessionManager;
import com.sportmate.app.testutil.FakeTokenStorage;
import com.sportmate.app.testutil.LiveDataTestUtil;
import com.sportmate.app.testutil.TestNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;

/**
 * Signed-in user opens Profile, edits it and a sport profile, then logs out, through the real
 * repositories, Retrofit and OkHttp. Requests are answered by path because the two profile
 * sections load in parallel.
 */
public class ProfileFlowIntegrationTest {
    private static final String TENNIS_ID = "3b0c8f2e-1d2a-4f3b-9c4d-5e6f7a8b9c01";

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MockWebServer server;
    private FakeTokenStorage tokenStorage;
    private Retrofit retrofit;
    private SessionManager sessionManager;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new BackendStub());
        server.start();
        tokenStorage = new FakeTokenStorage("access-1", "refresh-1");
        retrofit = TestNetwork.retrofit(server, tokenStorage);
        sessionManager = new SessionManager(new AuthRepositoryImpl(
                retrofit.create(AuthApi.class), retrofit.create(UserApi.class), tokenStorage, TestNetwork.executor()));
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void editProfileAndSportThenLogOut() throws Exception {
        sessionManager.restoreIfNeeded();
        LiveDataTestUtil.await(sessionManager.getAuthState(), s -> s.getStatus() == AuthState.Status.AUTHENTICATED);

        ProfileViewModel profile = new ProfileViewModel(
                new UserRepositoryImpl(retrofit.create(UserApi.class), TestNetwork.executor()), sessionManager);
        SportProfilesViewModel sports = new SportProfilesViewModel(
                new UserSportProfileRepositoryImpl(retrofit.create(UserApi.class), TestNetwork.executor()),
                new SportRepositoryImpl(retrofit.create(SportApi.class), TestNetwork.executor()));

        User loaded = LiveDataTestUtil.await(profile.getProfile(), s -> s.getStatus() == UiState.Status.SUCCESS).getData();
        assertEquals("Alex", loaded.getDisplayName());
        List<UserSportProfile> list = LiveDataTestUtil.await(sports.getProfiles(),
                s -> s.getStatus() == UiState.Status.SUCCESS).getData();
        assertTrue(list.isEmpty());
        assertEquals(1, sports.getAddableSports().getValue().size());

        profile.save("Alex Nguyen", "");
        LiveDataTestUtil.await(profile.getMessage(), event -> true);
        assertEquals("Alex Nguyen", profile.getProfile().getValue().getData().getDisplayName());
        assertEquals("Alex Nguyen", sessionManager.getAuthState().getValue().getUser().getDisplayName());

        sports.setSkillLevel(TENNIS_ID, SkillLevel.ADVANCED);
        LiveDataTestUtil.await(sports.getMessage(), event -> true);
        UserSportProfile saved = sports.getProfiles().getValue().getData().get(0);
        assertEquals("Tennis", saved.getSport().getName());
        assertEquals(SkillLevel.ADVANCED, saved.getSkillLevel());
        assertTrue(sports.getAddableSports().getValue().isEmpty());

        sessionManager.logout();
        assertEquals(AuthState.Status.UNAUTHENTICATED, sessionManager.getAuthState().getValue().getStatus());
        assertNull(tokenStorage.getAccessToken());
        assertNull(tokenStorage.getRefreshToken());
    }

    /** Answers like the backend for the endpoints this flow uses. */
    private static final class BackendStub extends Dispatcher {
        private String displayName = "Alex";

        @Override
        public MockResponse dispatch(RecordedRequest request) {
            if (!"Bearer access-1".equals(request.getHeader("Authorization"))) {
                return new MockResponse().setResponseCode(401);
            }
            String route = request.getMethod() + " " + request.getPath();
            switch (route) {
                case "GET /api/v1/users/me":
                    return json(user());
                case "PUT /api/v1/users/me":
                    String body = request.getBody().readUtf8();
                    if (!body.equals("{\"display_name\":\"Alex Nguyen\"}")) {
                        return new MockResponse().setResponseCode(400);
                    }
                    displayName = "Alex Nguyen";
                    return json(user());
                case "GET /api/v1/users/me/sports":
                    return json("[]");
                case "GET /api/v1/sports":
                    return json("[{\"id\":\"" + TENNIS_ID + "\",\"name\":\"Tennis\",\"slug\":\"tennis\"}]");
                case "PUT /api/v1/users/me/sports/" + TENNIS_ID:
                    if (!request.getBody().readUtf8().equals("{\"skill_level\":\"advanced\"}")) {
                        return new MockResponse().setResponseCode(400);
                    }
                    return json("{\"sport\":{\"id\":\"" + TENNIS_ID + "\",\"name\":\"Tennis\",\"slug\":\"tennis\","
                            + "\"is_active\":true},\"skill_level\":\"advanced\"}");
                case "POST /api/v1/auth/logout":
                    return new MockResponse().setResponseCode(204);
                default:
                    return new MockResponse().setResponseCode(404);
            }
        }

        private String user() {
            return "{\"id\":\"4f1c2d7e-8a8b-4d8e-9d52-2b1f0c1a7e11\",\"email\":\"a@example.com\","
                    + "\"display_name\":\"" + displayName + "\",\"phone\":null,\"status\":\"active\",\"role\":\"User\"}";
        }

        private static MockResponse json(String body) {
            return new MockResponse().addHeader("Content-Type", "application/json").setBody(body);
        }
    }
}

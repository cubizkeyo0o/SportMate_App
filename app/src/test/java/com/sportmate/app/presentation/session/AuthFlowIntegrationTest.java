package com.sportmate.app.presentation.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.data.remote.api.AuthApi;
import com.sportmate.app.data.remote.api.UserApi;
import com.sportmate.app.data.repository.AuthRepositoryImpl;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.presentation.auth.LoginViewModel;
import com.sportmate.app.presentation.common.FormState;
import com.sportmate.app.testutil.FakeTokenStorage;
import com.sportmate.app.testutil.LiveDataTestUtil;
import com.sportmate.app.testutil.TestNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Retrofit;

/** Session state driven through the real repository, Retrofit and OkHttp stack. */
public class AuthFlowIntegrationTest {
    private static final String USER_JSON = "{\"id\":\"4f1c2d7e-8a8b-4d8e-9d52-2b1f0c1a7e11\","
            + "\"email\":\"a@example.com\",\"display_name\":\"Alex\",\"phone\":null,"
            + "\"status\":\"active\",\"role\":\"User\"}";

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MockWebServer server;
    private FakeTokenStorage tokenStorage;
    private SessionManager sessionManager;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        tokenStorage = new FakeTokenStorage();
        Retrofit retrofit = TestNetwork.retrofit(server, tokenStorage);
        sessionManager = new SessionManager(new AuthRepositoryImpl(
                retrofit.create(AuthApi.class),
                retrofit.create(UserApi.class),
                tokenStorage,
                TestNetwork.executor()));
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void loginThenLogout() throws Exception {
        sessionManager.restoreIfNeeded();
        assertEquals(AuthState.Status.UNAUTHENTICATED, status());
        server.enqueue(new MockResponse().setBody("{\"access_token\":\"access-1\",\"refresh_token\":\"refresh-1\","
                + "\"token_type\":\"Bearer\",\"expires_in\":900,\"user\":" + USER_JSON + "}"));
        server.enqueue(new MockResponse().setResponseCode(204));

        LoginViewModel viewModel = new LoginViewModel(sessionManager);
        viewModel.login("a@example.com", "secret-pass1");
        awaitStatus(AuthState.Status.AUTHENTICATED);

        assertEquals("access-1", tokenStorage.getAccessToken());
        assertEquals("Alex", sessionManager.getAuthState().getValue().getUser().getDisplayName());

        sessionManager.logout();
        assertEquals(AuthState.Status.UNAUTHENTICATED, status());
        assertNull(tokenStorage.getAccessToken());
        assertNull(tokenStorage.getRefreshToken());
    }

    @Test
    public void failedLoginShowsErrorAndStoresNothing() throws Exception {
        sessionManager.restoreIfNeeded();
        server.enqueue(new MockResponse().setResponseCode(401)
                .setBody("{\"title\":\"Invalid email or password.\",\"status\":401}"));

        LoginViewModel viewModel = new LoginViewModel(sessionManager);
        viewModel.login("a@example.com", "wrong-pass1");
        FormState state = LiveDataTestUtil.await(viewModel.getState(), s -> !s.isLoading());

        assertEquals(AppError.Kind.UNAUTHORIZED, state.getError().getKind());
        assertEquals("Invalid email or password.", state.getError().getMessage());
        assertEquals(AuthState.Status.UNAUTHENTICATED, status());
        assertNull(tokenStorage.getAccessToken());
    }

    @Test
    public void startupWithAcceptedTokenIsAuthenticated() throws Exception {
        tokenStorage.saveTokens("access-1", "refresh-1");
        server.enqueue(new MockResponse().setBody(USER_JSON));

        sessionManager.restoreIfNeeded();
        awaitStatus(AuthState.Status.AUTHENTICATED);

        assertEquals("Bearer access-1", server.takeRequest().getHeader("Authorization"));
    }

    @Test
    public void startupWithExpiredTokenClearsItAndIsUnauthenticated() throws Exception {
        tokenStorage.saveTokens("expired", "refresh-1");
        server.enqueue(new MockResponse().setResponseCode(401));

        sessionManager.restoreIfNeeded();
        awaitStatus(AuthState.Status.UNAUTHENTICATED);

        assertNull(tokenStorage.getAccessToken());
        assertNull(tokenStorage.getRefreshToken());
    }

    @Test
    public void startupServerErrorKeepsTokensAndIsRetryable() throws Exception {
        tokenStorage.saveTokens("access-1", "refresh-1");
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setBody(USER_JSON));

        sessionManager.restoreIfNeeded();
        awaitStatus(AuthState.Status.ERROR);
        assertEquals("access-1", tokenStorage.getAccessToken());

        sessionManager.retryRestore();
        awaitStatus(AuthState.Status.AUTHENTICATED);
    }

    private AuthState.Status status() {
        return sessionManager.getAuthState().getValue().getStatus();
    }

    private void awaitStatus(AuthState.Status expected) throws InterruptedException {
        LiveDataTestUtil.await(sessionManager.getAuthState(), state -> state.getStatus() == expected);
    }
}

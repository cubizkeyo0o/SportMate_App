package com.sportmate.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.data.remote.api.AuthApi;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.model.UserRole;
import com.sportmate.app.domain.model.UserStatus;
import com.sportmate.app.testutil.BlockingCallback;
import com.sportmate.app.testutil.FakeTokenStorage;
import com.sportmate.app.testutil.TestNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class AuthRepositoryImplTest {
    // Shape produced by the backend: snake_case properties and snake_case enum values.
    private static final String AUTH_RESPONSE = "{"
            + "\"access_token\":\"access-1\",\"refresh_token\":\"refresh-1\","
            + "\"token_type\":\"Bearer\",\"expires_in\":900,"
            + "\"user\":{\"id\":\"4f1c2d7e-8a8b-4d8e-9d52-2b1f0c1a7e11\",\"email\":\"a@example.com\","
            + "\"display_name\":\"Alex\",\"phone\":null,\"status\":\"active\",\"role\":\"CourtOwner\"}}";

    private MockWebServer server;
    private FakeTokenStorage tokenStorage;
    private AuthRepositoryImpl repository;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        tokenStorage = new FakeTokenStorage();
        AuthApi api = TestNetwork.retrofit(server, tokenStorage).create(AuthApi.class);
        repository = new AuthRepositoryImpl(api, tokenStorage, TestNetwork.executor());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void loginSendsSnakeCaseRequestAndStoresTokens() throws Exception {
        server.enqueue(new MockResponse().setBody(AUTH_RESPONSE));

        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.login("a@example.com", "secret-pass", callback);
        User user = callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("/api/v1/auth/login", request.getPath());
        assertEquals("{\"email\":\"a@example.com\",\"password\":\"secret-pass\"}", request.getBody().readUtf8());
        assertEquals("Alex", user.getDisplayName());
        assertEquals(UserRole.COURT_OWNER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getPhone());
        assertEquals("access-1", tokenStorage.getAccessToken());
        assertEquals("refresh-1", tokenStorage.getRefreshToken());
        assertTrue(repository.hasSession());
    }

    @Test
    public void failedLoginStoresNothing() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401)
                .setBody("{\"title\":\"Invalid email or password.\",\"status\":401}"));

        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.login("a@example.com", "wrong", callback);
        AppError error = callback.await().error();

        assertEquals(AppError.Kind.UNAUTHORIZED, error.getKind());
        assertEquals("Invalid email or password.", error.getMessage());
        assertFalse(repository.hasSession());
    }

    @Test
    public void incompleteLoginResponseStoresNothing() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"access_token\":\"access-1\"}"));

        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.login("a@example.com", "secret-pass", callback);

        assertEquals(AppError.Kind.UNKNOWN, callback.await().error().getKind());
        assertNull(tokenStorage.getAccessToken());
    }

    @Test
    public void logoutSendsRefreshTokenWithBearerAndClearsTokens() throws Exception {
        tokenStorage.saveTokens("access-1", "refresh-1");
        server.enqueue(new MockResponse().setResponseCode(204));

        BlockingCallback<Void> callback = new BlockingCallback<>();
        repository.logout(callback);
        callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("/api/v1/auth/logout", request.getPath());
        assertEquals("Bearer access-1", request.getHeader("Authorization"));
        assertEquals("{\"refresh_token\":\"refresh-1\"}", request.getBody().readUtf8());
        assertFalse(repository.hasSession());
    }

    @Test
    public void logoutClearsTokensEvenWhenBackendFails() throws Exception {
        tokenStorage.saveTokens("access-1", "refresh-1");
        server.enqueue(new MockResponse().setResponseCode(500));

        BlockingCallback<Void> callback = new BlockingCallback<>();
        repository.logout(callback);

        assertEquals(AppError.Kind.SERVER, callback.await().error().getKind());
        assertFalse(repository.hasSession());
        assertNull(tokenStorage.getAccessToken());
    }
}

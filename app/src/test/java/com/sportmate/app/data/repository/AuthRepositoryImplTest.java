package com.sportmate.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.data.remote.api.AuthApi;
import com.sportmate.app.data.remote.api.UserApi;
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

import java.util.Collections;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import retrofit2.Retrofit;

/** Auth flows against the backend's actual JSON shapes and status codes. */
public class AuthRepositoryImplTest {
    // Backend UserResponse: snake_case properties, snake_case enum values, role as its name.
    private static final String USER_JSON = "{\"id\":\"4f1c2d7e-8a8b-4d8e-9d52-2b1f0c1a7e11\","
            + "\"email\":\"a@example.com\",\"display_name\":\"Alex\",\"phone\":null,"
            + "\"status\":\"active\",\"role\":\"User\"}";
    private static final String AUTH_RESPONSE = "{"
            + "\"access_token\":\"access-1\",\"refresh_token\":\"refresh-1\","
            + "\"token_type\":\"Bearer\",\"expires_in\":900,\"user\":" + USER_JSON + "}";

    private MockWebServer server;
    private FakeTokenStorage tokenStorage;
    private AuthRepositoryImpl repository;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        tokenStorage = new FakeTokenStorage();
        Retrofit retrofit = TestNetwork.retrofit(server, tokenStorage);
        repository = new AuthRepositoryImpl(
                retrofit.create(AuthApi.class),
                retrofit.create(UserApi.class),
                tokenStorage,
                TestNetwork.executor());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    // --- Login ---

    @Test
    public void loginSendsSnakeCaseRequestAndStoresTokens() throws Exception {
        server.enqueue(new MockResponse().setBody(AUTH_RESPONSE));

        User user = login("a@example.com", "secret-pass1").result();

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/auth/login", request.getPath());
        assertEquals("{\"email\":\"a@example.com\",\"password\":\"secret-pass1\"}", request.getBody().readUtf8());
        assertEquals("Alex", user.getDisplayName());
        assertEquals(UserRole.USER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getPhone());
        assertEquals("access-1", tokenStorage.getAccessToken());
        assertEquals("refresh-1", tokenStorage.getRefreshToken());
        assertTrue(repository.hasStoredSession());
    }

    @Test
    public void loginWithInvalidCredentialsStoresNothing() throws Exception {
        server.enqueue(problem(401, "{\"title\":\"Invalid email or password.\",\"status\":401}"));

        AppError error = login("a@example.com", "wrong-pass1").error();

        assertEquals(AppError.Kind.UNAUTHORIZED, error.getKind());
        assertEquals("Invalid email or password.", error.getMessage());
        assertNoTokens();
    }

    @Test
    public void loginValidationErrorCarriesFieldErrors() throws Exception {
        server.enqueue(problem(400, "{\"title\":\"One or more validation errors occurred.\",\"status\":400,"
                + "\"errors\":{\"email\":[\"Email is required.\"],\"password\":[\"Password is required.\"]}}"));

        AppError error = login("", "").error();

        assertEquals(AppError.Kind.VALIDATION, error.getKind());
        assertEquals(Collections.singletonList("Email is required."), error.getFieldErrors().get("email"));
        assertEquals(Collections.singletonList("Password is required."), error.getFieldErrors().get("password"));
        assertNoTokens();
    }

    @Test
    public void loginOfInactiveAccountIsForbidden() throws Exception {
        server.enqueue(problem(403, "{\"title\":\"This account is not active.\",\"status\":403}"));

        AppError error = login("a@example.com", "secret-pass1").error();

        assertEquals(AppError.Kind.FORBIDDEN, error.getKind());
        assertEquals("This account is not active.", error.getMessage());
        assertNoTokens();
    }

    @Test
    public void loginServerErrorStoresNothing() throws Exception {
        server.enqueue(problem(500, "{\"title\":\"An unexpected error occurred.\",\"status\":500}"));

        assertEquals(AppError.Kind.SERVER, login("a@example.com", "secret-pass1").error().getKind());
        assertNoTokens();
    }

    @Test
    public void loginNetworkFailureStoresNothing() throws Exception {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        assertEquals(AppError.Kind.NETWORK, login("a@example.com", "secret-pass1").error().getKind());
        assertNoTokens();
    }

    @Test
    public void incompleteLoginResponseStoresNothing() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"access_token\":\"access-1\"}"));

        assertEquals(AppError.Kind.UNKNOWN, login("a@example.com", "secret-pass1").error().getKind());
        assertNoTokens();
    }

    // --- Register ---

    @Test
    public void registerSendsOnlyContractFieldsAndDoesNotSignIn() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201)
                .addHeader("Location", "/api/v1/users/me")
                .setBody(USER_JSON));

        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.register("a@example.com", "secret-pass1", "Alex", "+84 912 345 678", callback);
        User user = callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("/api/v1/auth/register", request.getPath());
        assertEquals("{\"email\":\"a@example.com\",\"password\":\"secret-pass1\","
                + "\"display_name\":\"Alex\",\"phone\":\"+84 912 345 678\"}", request.getBody().readUtf8());
        assertEquals(UserRole.USER, user.getRole());
        assertNoTokens();
    }

    @Test
    public void registerOmitsMissingPhone() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201).setBody(USER_JSON));

        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.register("a@example.com", "secret-pass1", "Alex", null, callback);
        callback.await().result();

        assertEquals("{\"email\":\"a@example.com\",\"password\":\"secret-pass1\",\"display_name\":\"Alex\"}",
                server.takeRequest().getBody().readUtf8());
    }

    @Test
    public void registerValidationErrorsAndConflictsAreReported() throws Exception {
        server.enqueue(problem(400, "{\"title\":\"One or more validation errors occurred.\",\"status\":400,"
                + "\"errors\":{\"password\":[\"Password must be between 8 and 128 characters.\","
                + "\"Password must contain at least one letter and one digit.\"]}}"));
        server.enqueue(problem(409, "{\"title\":\"An account with this email already exists.\",\"status\":409}"));

        AppError validation = register().error();
        AppError conflict = register().error();

        assertEquals(AppError.Kind.VALIDATION, validation.getKind());
        assertEquals(2, validation.getFieldErrors().get("password").size());
        assertEquals(AppError.Kind.CONFLICT, conflict.getKind());
        assertEquals("An account with this email already exists.", conflict.getMessage());
    }

    @Test
    public void registerServerAndNetworkFailuresAreReported() throws Exception {
        server.enqueue(problem(500, "{\"title\":\"An unexpected error occurred.\",\"status\":500}"));
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        assertEquals(AppError.Kind.SERVER, register().error().getKind());
        assertEquals(AppError.Kind.NETWORK, register().error().getKind());
    }

    // --- Session restore ---

    @Test
    public void noStoredTokenMeansNoStoredSession() {
        assertFalse(repository.hasStoredSession());
    }

    @Test
    public void restoreLoadsCurrentUserWithStoredToken() throws Exception {
        tokenStorage.saveTokens("access-1", "refresh-1");
        server.enqueue(new MockResponse().setBody(USER_JSON));

        User user = restore().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/users/me", request.getPath());
        assertEquals("Bearer access-1", request.getHeader("Authorization"));
        assertEquals("Alex", user.getDisplayName());
        assertTrue(repository.hasStoredSession());
    }

    @Test
    public void restoreRejectedWith401ClearsTokens() throws Exception {
        tokenStorage.saveTokens("expired", "refresh-1");
        server.enqueue(new MockResponse().setResponseCode(401).addHeader("WWW-Authenticate", "Bearer"));

        assertEquals(AppError.Kind.UNAUTHORIZED, restore().error().getKind());
        assertNoTokens();
        assertFalse(repository.hasStoredSession());
    }

    @Test
    public void restoreServerOrNetworkErrorKeepsTokens() throws Exception {
        tokenStorage.saveTokens("access-1", "refresh-1");
        server.enqueue(problem(500, "{\"title\":\"An unexpected error occurred.\",\"status\":500}"));
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        assertEquals(AppError.Kind.SERVER, restore().error().getKind());
        assertEquals(AppError.Kind.NETWORK, restore().error().getKind());
        assertEquals("access-1", tokenStorage.getAccessToken());
        assertEquals("refresh-1", tokenStorage.getRefreshToken());
    }

    // --- Logout ---

    @Test
    public void logoutClearsTokensAtOnceAndRevokesWithCapturedToken() throws Exception {
        tokenStorage.saveTokens("access-1", "refresh-1");
        server.enqueue(new MockResponse().setResponseCode(204));

        BlockingCallback<Void> callback = new BlockingCallback<>();
        repository.logout(callback);
        assertNoTokens();
        callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("/api/v1/auth/logout", request.getPath());
        assertEquals("Bearer access-1", request.getHeader("Authorization"));
        assertEquals("{\"refresh_token\":\"refresh-1\"}", request.getBody().readUtf8());
    }

    @Test
    public void logoutKeepsTokensClearedWhenBackendFails() throws Exception {
        tokenStorage.saveTokens("access-1", "refresh-1");
        server.enqueue(new MockResponse().setResponseCode(500));

        BlockingCallback<Void> callback = new BlockingCallback<>();
        repository.logout(callback);

        assertEquals(AppError.Kind.SERVER, callback.await().error().getKind());
        assertNoTokens();
        assertFalse(repository.hasStoredSession());
    }

    @Test
    public void logoutWithoutTokensSkipsBackend() throws Exception {
        BlockingCallback<Void> callback = new BlockingCallback<>();
        repository.logout(callback);

        callback.await().result();
        assertEquals(0, server.getRequestCount());
    }

    private BlockingCallback<User> login(String email, String password) throws InterruptedException {
        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.login(email, password, callback);
        return callback.await();
    }

    private BlockingCallback<User> register() throws InterruptedException {
        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.register("a@example.com", "short", "Alex", null, callback);
        return callback.await();
    }

    private BlockingCallback<User> restore() throws InterruptedException {
        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.restoreSession(callback);
        return callback.await();
    }

    private void assertNoTokens() {
        assertNull(tokenStorage.getAccessToken());
        assertNull(tokenStorage.getRefreshToken());
    }

    private static MockResponse problem(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/problem+json")
                .setBody(body);
    }
}

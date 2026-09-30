package com.sportmate.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

/** GET/PUT api/v1/users/me against the backend's JSON shapes. */
public class UserRepositoryImplTest {
    private MockWebServer server;
    private UserRepositoryImpl repository;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        UserApi api = TestNetwork.retrofit(server, new FakeTokenStorage("access-1", "refresh-1")).create(UserApi.class);
        repository = new UserRepositoryImpl(api, TestNetwork.executor());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void getCurrentUserMapsSnakeCaseResponse() throws Exception {
        server.enqueue(new MockResponse().setBody(user("Alex", "\"+84 912 345 678\"", "suspended", "CourtOwner")));

        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.getCurrentUser(callback);
        User user = callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/users/me", request.getPath());
        assertEquals("Bearer access-1", request.getHeader("Authorization"));
        assertEquals("4f1c2d7e-8a8b-4d8e-9d52-2b1f0c1a7e11", user.getId());
        assertEquals("a@example.com", user.getEmail());
        assertEquals("Alex", user.getDisplayName());
        assertEquals("+84 912 345 678", user.getPhone());
        assertEquals(UserStatus.SUSPENDED, user.getStatus());
        assertEquals(UserRole.COURT_OWNER, user.getRole());
    }

    @Test
    public void updateProfileSendsOnlyEditableFields() throws Exception {
        server.enqueue(new MockResponse().setBody(user("Alex N", "\"0912345678\"", "active", "User")));

        User user = update("Alex N", "0912345678").result();

        RecordedRequest request = server.takeRequest();
        assertEquals("PUT", request.getMethod());
        assertEquals("/api/v1/users/me", request.getPath());
        assertEquals("{\"display_name\":\"Alex N\",\"phone\":\"0912345678\"}", request.getBody().readUtf8());
        assertEquals("Alex N", user.getDisplayName());
        assertEquals("0912345678", user.getPhone());
    }

    @Test
    public void updateWithoutPhoneOmitsItSoTheBackendClearsIt() throws Exception {
        server.enqueue(new MockResponse().setBody(user("Alex", "null", "active", "User")));

        User user = update("Alex", null).result();

        assertEquals("{\"display_name\":\"Alex\"}", server.takeRequest().getBody().readUtf8());
        assertNull(user.getPhone());
    }

    @Test
    public void updateValidationErrorsAreKeyedByField() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(400).setBody(
                "{\"title\":\"One or more validation errors occurred.\",\"status\":400,"
                        + "\"errors\":{\"display_name\":[\"Display name is required.\"]}}"));

        AppError error = update(" ", null).error();

        assertEquals(AppError.Kind.VALIDATION, error.getKind());
        assertEquals(Collections.singletonList("Display name is required."), error.getFieldErrors().get("display_name"));
    }

    @Test
    public void updateOfInactiveAccountIsForbidden() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(403)
                .setBody("{\"title\":\"This account is not active.\",\"status\":403}"));

        AppError error = update("Alex", null).error();

        assertEquals(AppError.Kind.FORBIDDEN, error.getKind());
        assertEquals("This account is not active.", error.getMessage());
    }

    @Test
    public void unauthorizedServerAndNetworkErrors() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        assertEquals(AppError.Kind.UNAUTHORIZED, get().error().getKind());
        assertEquals(AppError.Kind.SERVER, get().error().getKind());
        assertEquals(AppError.Kind.NETWORK, get().error().getKind());
    }

    private BlockingCallback<User> get() throws InterruptedException {
        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.getCurrentUser(callback);
        return callback.await();
    }

    private BlockingCallback<User> update(String displayName, String phone) throws InterruptedException {
        BlockingCallback<User> callback = new BlockingCallback<>();
        repository.updateProfile(displayName, phone, callback);
        return callback.await();
    }

    private static String user(String displayName, String phoneJson, String status, String role) {
        return "{\"id\":\"4f1c2d7e-8a8b-4d8e-9d52-2b1f0c1a7e11\",\"email\":\"a@example.com\","
                + "\"display_name\":\"" + displayName + "\",\"phone\":" + phoneJson + ","
                + "\"status\":\"" + status + "\",\"role\":\"" + role + "\"}";
    }
}

package com.sportmate.app.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.data.remote.api.HealthApi;
import com.sportmate.app.data.repository.HealthRepositoryImpl;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.testutil.BlockingCallback;
import com.sportmate.app.testutil.FakeTokenStorage;
import com.sportmate.app.testutil.TestNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/** Error translation for the ProblemDetails bodies the backend returns. */
public class ApiCallExecutorTest {
    private MockWebServer server;
    private HealthRepositoryImpl repository;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        HealthApi api = TestNetwork.retrofit(server, new FakeTokenStorage()).create(HealthApi.class);
        repository = new HealthRepositoryImpl(api, TestNetwork.executor());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void mapsSuccessfulBody() throws Exception {
        server.enqueue(json(200, "{\"status\":\"ok\"}"));

        assertTrue(check().result());
    }

    @Test
    public void mapsValidationProblemWithFieldErrors() throws Exception {
        server.enqueue(json(400, "{\"type\":\"https://tools.ietf.org/html/rfc9110#section-15.5.1\","
                + "\"title\":\"One or more validation errors occurred.\",\"status\":400,"
                + "\"errors\":{\"email\":[\"Email is required.\"]},\"instance\":\"/api/v1/health\"}"));

        AppError error = check().error();

        assertEquals(AppError.Kind.VALIDATION, error.getKind());
        assertEquals(400, error.getHttpStatus());
        assertEquals("One or more validation errors occurred.", error.getMessage());
        assertEquals(Collections.singletonList("Email is required."), error.getFieldErrors().get("email"));
    }

    @Test
    public void mapsConflictTitle() throws Exception {
        server.enqueue(json(409, "{\"title\":\"The court is already booked for this time.\",\"status\":409}"));

        AppError error = check().error();

        assertEquals(AppError.Kind.CONFLICT, error.getKind());
        assertEquals("The court is already booked for this time.", error.getMessage());
    }

    @Test
    public void mapsEmptyUnauthorizedChallenge() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401).addHeader("WWW-Authenticate", "Bearer"));

        AppError error = check().error();

        assertEquals(AppError.Kind.UNAUTHORIZED, error.getKind());
        assertNull(error.getMessage());
        assertTrue(error.getFieldErrors().isEmpty());
    }

    @Test
    public void mapsDomainRuleAndServerErrors() throws Exception {
        server.enqueue(json(422, "{\"title\":\"Not allowed.\",\"status\":422}"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("not json"));

        assertEquals(AppError.Kind.BUSINESS_RULE, check().error().getKind());
        AppError serverError = check().error();
        assertEquals(AppError.Kind.SERVER, serverError.getKind());
        assertNull(serverError.getMessage());
    }

    @Test
    public void mapsUnreachableServerToNetworkError() throws Exception {
        server.shutdown();

        AppError error = check().error();

        assertEquals(AppError.Kind.NETWORK, error.getKind());
        assertEquals(0, error.getHttpStatus());
    }

    private BlockingCallback<Boolean> check() throws InterruptedException {
        BlockingCallback<Boolean> callback = new BlockingCallback<>();
        repository.checkHealth(callback);
        return callback.await();
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/problem+json")
                .setBody(body);
    }
}

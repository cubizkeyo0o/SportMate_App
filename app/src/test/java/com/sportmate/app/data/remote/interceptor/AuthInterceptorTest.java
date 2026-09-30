package com.sportmate.app.data.remote.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.sportmate.app.testutil.FakeTokenStorage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class AuthInterceptorTest {
    private MockWebServer server;
    private FakeTokenStorage tokenStorage;
    private OkHttpClient client;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        tokenStorage = new FakeTokenStorage();
        client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenStorage, server.url("/")))
                .build();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void attachesBearerTokenWhenStored() throws Exception {
        tokenStorage.saveTokens("access-123", "refresh-456");

        RecordedRequest request = send(new Request.Builder().url(server.url("/api/v1/users/me")).build());

        assertEquals("Bearer access-123", request.getHeader("Authorization"));
    }

    @Test
    public void sendsNoAuthorizationWithoutToken() throws Exception {
        RecordedRequest request = send(new Request.Builder().url(server.url("/api/v1/health")).build());

        assertNull(request.getHeader("Authorization"));
    }

    @Test
    public void keepsExplicitAuthorizationHeader() throws Exception {
        tokenStorage.saveTokens("access-123", "refresh-456");

        RecordedRequest request = send(new Request.Builder()
                .url(server.url("/api/v1/users/me"))
                .header("Authorization", "Bearer other")
                .build());

        assertEquals("Bearer other", request.getHeader("Authorization"));
    }

    @Test
    public void doesNotSendTokenToAnotherOrigin() throws Exception {
        tokenStorage.saveTokens("access-123", "refresh-456");
        OkHttpClient otherOriginClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenStorage, HttpUrl.get("https://api.example.invalid/")))
                .build();
        server.enqueue(new MockResponse());

        otherOriginClient.newCall(new Request.Builder().url(server.url("/")).build()).execute().close();

        assertNull(server.takeRequest().getHeader("Authorization"));
    }

    private RecordedRequest send(Request request) throws Exception {
        server.enqueue(new MockResponse());
        client.newCall(request).execute().close();
        return server.takeRequest();
    }
}

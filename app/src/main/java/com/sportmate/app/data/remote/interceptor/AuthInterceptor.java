package com.sportmate.app.data.remote.interceptor;

import androidx.annotation.NonNull;

import com.sportmate.app.data.local.TokenStorage;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adds {@code Authorization: Bearer <access token>} when a token is stored. The token is only sent
 * to the configured API origin, and an explicitly set Authorization header is left untouched.
 * Expired tokens are not refreshed here yet; the backend answers 401.
 */
@Singleton
public final class AuthInterceptor implements Interceptor {
    public static final String HEADER_AUTHORIZATION = "Authorization";

    private final TokenStorage tokenStorage;
    private final HttpUrl apiBaseUrl;

    @Inject
    public AuthInterceptor(TokenStorage tokenStorage, @Named("apiBaseUrl") HttpUrl apiBaseUrl) {
        this.tokenStorage = tokenStorage;
        this.apiBaseUrl = apiBaseUrl;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        if (request.header(HEADER_AUTHORIZATION) != null || !isApiOrigin(request.url())) {
            return chain.proceed(request);
        }

        String accessToken = tokenStorage.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return chain.proceed(request);
        }

        return chain.proceed(request.newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build());
    }

    private boolean isApiOrigin(HttpUrl url) {
        return url.scheme().equals(apiBaseUrl.scheme())
                && url.host().equals(apiBaseUrl.host())
                && url.port() == apiBaseUrl.port();
    }
}

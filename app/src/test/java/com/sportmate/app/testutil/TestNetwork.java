package com.sportmate.app.testutil;

import com.sportmate.app.data.local.TokenStorage;
import com.sportmate.app.data.remote.ApiCallExecutor;
import com.sportmate.app.data.remote.ApiErrorParser;
import com.sportmate.app.data.remote.interceptor.AuthInterceptor;
import com.sportmate.app.di.NetworkModule;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Builds the production network stack against a MockWebServer. */
public final class TestNetwork {
    private TestNetwork() {
    }

    public static Retrofit retrofit(MockWebServer server, TokenStorage tokenStorage) {
        HttpUrl baseUrl = server.url("/");
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenStorage, baseUrl))
                .build();
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(NetworkModule.provideGson()))
                .build();
    }

    public static ApiCallExecutor executor() {
        return new ApiCallExecutor(new ApiErrorParser(NetworkModule.provideGson()));
    }
}

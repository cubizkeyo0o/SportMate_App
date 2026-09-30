package com.sportmate.app.di;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sportmate.app.BuildConfig;
import com.sportmate.app.data.remote.api.AuthApi;
import com.sportmate.app.data.remote.api.HealthApi;
import com.sportmate.app.data.remote.api.SportApi;
import com.sportmate.app.data.remote.api.UserApi;
import com.sportmate.app.data.remote.interceptor.AuthInterceptor;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public final class NetworkModule {
    private NetworkModule() {
    }

    // DTOs name their JSON fields explicitly with @SerializedName.
    @Provides
    @Singleton
    public static Gson provideGson() {
        return new GsonBuilder().create();
    }

    /** From BuildConfig.API_BASE_URL; see app/build.gradle for how to override it. */
    @Provides
    @Singleton
    @Named("apiBaseUrl")
    public static HttpUrl provideApiBaseUrl() {
        return HttpUrl.get(BuildConfig.API_BASE_URL);
    }

    @Provides
    @Singleton
    public static OkHttpClient provideOkHttpClient(AuthInterceptor authInterceptor) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor);

        if (BuildConfig.DEBUG) {
            // BASIC logs only the request line and status: no headers, no bodies (which carry
            // tokens and passwords). Redaction guards against the level being raised to HEADERS.
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.redactHeader(AuthInterceptor.HEADER_AUTHORIZATION);
            logging.redactHeader("Cookie");
            logging.redactHeader("Set-Cookie");
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            builder.addInterceptor(logging);
        }
        return builder.build();
    }

    @Provides
    @Singleton
    public static Retrofit provideRetrofit(
            @Named("apiBaseUrl") HttpUrl apiBaseUrl,
            OkHttpClient client,
            Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(apiBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    public static HealthApi provideHealthApi(Retrofit retrofit) {
        return retrofit.create(HealthApi.class);
    }

    @Provides
    @Singleton
    public static AuthApi provideAuthApi(Retrofit retrofit) {
        return retrofit.create(AuthApi.class);
    }

    @Provides
    @Singleton
    public static UserApi provideUserApi(Retrofit retrofit) {
        return retrofit.create(UserApi.class);
    }

    @Provides
    @Singleton
    public static SportApi provideSportApi(Retrofit retrofit) {
        return retrofit.create(SportApi.class);
    }
}

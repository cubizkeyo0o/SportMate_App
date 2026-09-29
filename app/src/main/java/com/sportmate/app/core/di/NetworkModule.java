package com.sportmate.app.core.di;

import com.sportmate.app.BuildConfig;
import com.sportmate.app.data.api.ApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public final class NetworkModule {
    private NetworkModule() {
    }

    @Provides
    @Singleton
    static Gson provideGson() {
        return new GsonBuilder().create();
    }

    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BASIC
                : HttpLoggingInterceptor.Level.NONE);

        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
    }

    @Provides
    @Singleton
    static Retrofit provideRetrofit(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    static ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
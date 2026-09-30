package com.sportmate.app.di;

import com.sportmate.app.data.local.SharedPreferencesTokenStorage;
import com.sportmate.app.data.local.TokenStorage;
import com.sportmate.app.data.repository.AuthRepositoryImpl;
import com.sportmate.app.data.repository.HealthRepositoryImpl;
import com.sportmate.app.data.repository.SportRepositoryImpl;
import com.sportmate.app.data.repository.UserRepositoryImpl;
import com.sportmate.app.data.repository.UserSportProfileRepositoryImpl;
import com.sportmate.app.domain.repository.AuthRepository;
import com.sportmate.app.domain.repository.HealthRepository;
import com.sportmate.app.domain.repository.SportRepository;
import com.sportmate.app.domain.repository.UserRepository;
import com.sportmate.app.domain.repository.UserSportProfileRepository;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/** Binds data-layer implementations to the interfaces the rest of the app depends on. */
@Module
@InstallIn(SingletonComponent.class)
public abstract class DataModule {
    @Binds
    abstract TokenStorage bindTokenStorage(SharedPreferencesTokenStorage impl);

    @Binds
    abstract HealthRepository bindHealthRepository(HealthRepositoryImpl impl);

    @Binds
    abstract AuthRepository bindAuthRepository(AuthRepositoryImpl impl);

    @Binds
    abstract UserRepository bindUserRepository(UserRepositoryImpl impl);

    @Binds
    abstract SportRepository bindSportRepository(SportRepositoryImpl impl);

    @Binds
    abstract UserSportProfileRepository bindUserSportProfileRepository(UserSportProfileRepositoryImpl impl);
}

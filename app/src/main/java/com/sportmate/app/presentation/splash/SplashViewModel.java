package com.sportmate.app.presentation.splash;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.domain.repository.AuthRepository;
import com.sportmate.app.presentation.common.Event;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Decides where the app starts: the main flow with a stored session, otherwise the auth flow. */
@HiltViewModel
public final class SplashViewModel extends ViewModel {
    public enum Destination {
        AUTH,
        MAIN
    }

    private final MutableLiveData<Event<Destination>> destination = new MutableLiveData<>();

    @Inject
    public SplashViewModel(AuthRepository authRepository) {
        destination.setValue(new Event<>(authRepository.hasSession() ? Destination.MAIN : Destination.AUTH));
    }

    public LiveData<Event<Destination>> getDestination() {
        return destination;
    }
}

package com.sportmate.app.presentation.session;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Activity-scoped access to {@link SessionManager} for MainActivity, Splash and signed-in screens.
 * Creating it starts the stored-session check.
 */
@HiltViewModel
public final class SessionViewModel extends ViewModel {
    private final SessionManager sessionManager;

    @Inject
    public SessionViewModel(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        sessionManager.restoreIfNeeded();
    }

    public LiveData<AuthState> getAuthState() {
        return sessionManager.getAuthState();
    }

    public void retry() {
        sessionManager.retryRestore();
    }

    public void logout() {
        sessionManager.logout();
    }
}

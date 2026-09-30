package com.sportmate.app.presentation.session;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.repository.AuthRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Single source of the app's {@link AuthState}. The state only changes as a result of backend
 * calls made through {@link AuthRepository}; nothing can mark the app authenticated otherwise.
 * Must be used from the main thread.
 */
@Singleton
public final class SessionManager {
    private final AuthRepository authRepository;
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>(AuthState.loading());
    private boolean restoreStarted;

    @Inject
    public SessionManager(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<AuthState> getAuthState() {
        return authState;
    }

    /** Checks the stored session once per process. */
    public void restoreIfNeeded() {
        if (!restoreStarted) {
            restore();
        }
    }

    /** Checks the stored session again after a network or server error. */
    public void retryRestore() {
        if (currentStatus() == AuthState.Status.ERROR) {
            restore();
        }
    }

    public Cancellable login(String email, String password, ResultCallback<User> callback) {
        return authRepository.login(email, password, new ResultCallback<User>() {
            @Override
            public void onSuccess(User user) {
                authState.setValue(AuthState.authenticated(user));
                callback.onSuccess(user);
            }

            @Override
            public void onError(@NonNull AppError error) {
                callback.onError(error);
            }
        });
    }

    /** Signs out locally at once; the backend revocation is best-effort. */
    public void logout() {
        authRepository.logout(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(@NonNull AppError error) {
            }
        });
        authState.setValue(AuthState.unauthenticated());
    }

    private void restore() {
        restoreStarted = true;
        if (!authRepository.hasStoredSession()) {
            authState.setValue(AuthState.unauthenticated());
            return;
        }

        authState.setValue(AuthState.loading());
        authRepository.restoreSession(new ResultCallback<User>() {
            @Override
            public void onSuccess(User user) {
                authState.setValue(AuthState.authenticated(user));
            }

            @Override
            public void onError(@NonNull AppError error) {
                // The repository has already cleared rejected tokens.
                authState.setValue(error.getKind() == AppError.Kind.UNAUTHORIZED
                        ? AuthState.unauthenticated()
                        : AuthState.error(error));
            }
        });
    }

    private AuthState.Status currentStatus() {
        AuthState state = authState.getValue();
        return state == null ? AuthState.Status.LOADING : state.getStatus();
    }
}

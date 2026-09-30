package com.sportmate.app.presentation.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.testutil.ControllableAuthRepository;

import org.junit.Rule;
import org.junit.Test;

public class SessionManagerTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableAuthRepository repository = new ControllableAuthRepository();
    private final SessionManager sessionManager = new SessionManager(repository);

    @Test
    public void startsLoadingBeforeRestore() {
        assertEquals(AuthState.Status.LOADING, status());
    }

    @Test
    public void noStoredTokenIsUnauthenticatedWithoutCallingBackend() {
        sessionManager.restoreIfNeeded();

        assertEquals(AuthState.Status.UNAUTHENTICATED, status());
        assertEquals(0, repository.restoreCalls);
    }

    @Test
    public void storedTokenAcceptedByBackendIsAuthenticated() {
        repository.storedSession = true;
        sessionManager.restoreIfNeeded();
        assertEquals(AuthState.Status.LOADING, status());

        User user = ControllableAuthRepository.user();
        repository.restoreCallback.onSuccess(user);

        assertEquals(AuthState.Status.AUTHENTICATED, status());
        assertSame(user, sessionManager.getAuthState().getValue().getUser());
    }

    @Test
    public void storedTokenRejectedWith401IsUnauthenticated() {
        repository.storedSession = true;
        sessionManager.restoreIfNeeded();

        repository.restoreCallback.onError(new AppError(AppError.Kind.UNAUTHORIZED, 401, null, null));

        assertEquals(AuthState.Status.UNAUTHENTICATED, status());
    }

    @Test
    public void otherRestoreErrorsAreRetryable() {
        repository.storedSession = true;
        sessionManager.restoreIfNeeded();
        AppError serverError = new AppError(AppError.Kind.SERVER, 500, null, null);

        repository.restoreCallback.onError(serverError);
        assertEquals(AuthState.Status.ERROR, status());
        assertSame(serverError, sessionManager.getAuthState().getValue().getError());

        sessionManager.retryRestore();
        assertEquals(AuthState.Status.LOADING, status());
        assertEquals(2, repository.restoreCalls);

        repository.restoreCallback.onError(AppError.of(AppError.Kind.NETWORK));
        assertEquals(AuthState.Status.ERROR, status());
    }

    @Test
    public void restoreRunsOncePerProcessAndRetryOnlyAfterError() {
        repository.storedSession = true;
        sessionManager.restoreIfNeeded();
        sessionManager.restoreIfNeeded();
        sessionManager.retryRestore();

        assertEquals(1, repository.restoreCalls);
    }

    @Test
    public void successfulLoginAuthenticates() {
        sessionManager.restoreIfNeeded();
        RecordingCallback callback = new RecordingCallback();

        sessionManager.login("a@example.com", "secret-pass1", callback);
        repository.loginCallback.onSuccess(ControllableAuthRepository.user());

        assertEquals(AuthState.Status.AUTHENTICATED, status());
        assertEquals(1, callback.successes);
    }

    @Test
    public void failedLoginStaysUnauthenticated() {
        sessionManager.restoreIfNeeded();
        RecordingCallback callback = new RecordingCallback();

        sessionManager.login("a@example.com", "wrong-pass1", callback);
        repository.loginCallback.onError(new AppError(AppError.Kind.UNAUTHORIZED, 401, "Invalid email or password.", null));

        assertEquals(AuthState.Status.UNAUTHENTICATED, status());
        assertEquals(AppError.Kind.UNAUTHORIZED, callback.error.getKind());
    }

    @Test
    public void logoutIsUnauthenticated() {
        repository.storedSession = true;
        sessionManager.restoreIfNeeded();
        repository.restoreCallback.onSuccess(ControllableAuthRepository.user());

        sessionManager.logout();

        assertEquals(AuthState.Status.UNAUTHENTICATED, status());
        assertEquals(1, repository.logoutCalls);
    }

    private AuthState.Status status() {
        return sessionManager.getAuthState().getValue().getStatus();
    }

    private static final class RecordingCallback implements ResultCallback<User> {
        int successes;
        AppError error;

        @Override
        public void onSuccess(User result) {
            successes++;
        }

        @Override
        public void onError(@NonNull AppError error) {
            this.error = error;
        }
    }
}

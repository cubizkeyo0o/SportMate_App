package com.sportmate.app.presentation.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.presentation.common.UiText;
import com.sportmate.app.presentation.session.AuthState;
import com.sportmate.app.presentation.session.SessionManager;
import com.sportmate.app.testutil.ControllableAuthRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableAuthRepository repository = new ControllableAuthRepository();
    private final SessionManager sessionManager = new SessionManager(repository);
    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        sessionManager.restoreIfNeeded();
        viewModel = new LoginViewModel(sessionManager);
    }

    @Test
    public void invalidInputIsRejectedWithoutCallingBackend() {
        viewModel.login("  ", "");

        AuthFormState state = state();
        assertFalse(state.isLoading());
        assertEquals(UiText.of(R.string.error_email_required), state.getFieldError(AuthInputRules.FIELD_EMAIL));
        assertEquals(UiText.of(R.string.error_password_required), state.getFieldError(AuthInputRules.FIELD_PASSWORD));
        assertEquals(0, repository.loginCalls);
    }

    @Test
    public void successGoesLoadingThenIdleAndAuthenticatesSession() {
        viewModel.login("  a@example.com ", "secret-pass1");

        assertTrue(state().isLoading());
        assertEquals("a@example.com", repository.lastEmail);
        assertEquals("secret-pass1", repository.lastPassword);

        repository.loginCallback.onSuccess(ControllableAuthRepository.user());

        assertFalse(state().isLoading());
        assertNull(state().getError());
        assertEquals(AuthState.Status.AUTHENTICATED, sessionManager.getAuthState().getValue().getStatus());
    }

    @Test
    public void submitWhileLoadingIsIgnored() {
        viewModel.login("a@example.com", "secret-pass1");
        viewModel.login("a@example.com", "secret-pass1");

        assertEquals(1, repository.loginCalls);
    }

    @Test
    public void invalidCredentialsShowGeneralErrorAndStayUnauthenticated() {
        viewModel.login("a@example.com", "wrong-pass1");
        AppError error = new AppError(AppError.Kind.UNAUTHORIZED, 401, "Invalid email or password.", null);

        repository.loginCallback.onError(error);

        assertFalse(state().isLoading());
        assertSame(error, state().getError());
        assertTrue(state().getFieldErrors().isEmpty());
        assertEquals(AuthState.Status.UNAUTHENTICATED, sessionManager.getAuthState().getValue().getStatus());
    }

    @Test
    public void backendValidationErrorsAreShownOnFields() {
        viewModel.login("a@example.com", "x");
        Map<String, List<String>> fieldErrors = new HashMap<>();
        fieldErrors.put("email", Collections.singletonList("Email is required."));
        fieldErrors.put("password", Arrays.asList("First.", "Second."));

        repository.loginCallback.onError(new AppError(AppError.Kind.VALIDATION, 400, "One or more validation errors occurred.", fieldErrors));

        assertEquals(UiText.raw("Email is required."), state().getFieldError(AuthInputRules.FIELD_EMAIL));
        assertEquals(UiText.raw("First.\nSecond."), state().getFieldError(AuthInputRules.FIELD_PASSWORD));
        assertNull(state().getError());
    }

    @Test
    public void serverAndNetworkErrorsAreGeneralErrors() {
        viewModel.login("a@example.com", "secret-pass1");
        repository.loginCallback.onError(new AppError(AppError.Kind.SERVER, 500, null, null));
        assertEquals(AppError.Kind.SERVER, state().getError().getKind());
        assertFalse(state().isLoading());

        viewModel.login("a@example.com", "secret-pass1");
        repository.loginCallback.onError(AppError.of(AppError.Kind.NETWORK));
        assertEquals(AppError.Kind.NETWORK, state().getError().getKind());
        assertFalse(state().isLoading());
    }

    @Test
    public void editingAFieldClearsOnlyItsError() {
        viewModel.login("", "");

        viewModel.onFieldEdited(AuthInputRules.FIELD_EMAIL);

        assertNull(state().getFieldError(AuthInputRules.FIELD_EMAIL));
        assertEquals(UiText.of(R.string.error_password_required), state().getFieldError(AuthInputRules.FIELD_PASSWORD));
    }

    @Test
    public void clearingCancelsPendingLogin() {
        viewModel.login("a@example.com", "secret-pass1");

        viewModel.onCleared();

        assertEquals(1, repository.cancelled);
    }

    private AuthFormState state() {
        return viewModel.getState().getValue();
    }
}

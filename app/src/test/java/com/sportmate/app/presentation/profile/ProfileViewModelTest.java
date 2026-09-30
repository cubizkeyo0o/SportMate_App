package com.sportmate.app.presentation.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.common.UiText;
import com.sportmate.app.presentation.common.UserInputRules;
import com.sportmate.app.presentation.session.AuthState;
import com.sportmate.app.presentation.session.SessionManager;
import com.sportmate.app.testutil.ControllableAuthRepository;
import com.sportmate.app.testutil.ControllableProfileRepositories;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

public class ProfileViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableProfileRepositories.Users users = new ControllableProfileRepositories.Users();
    private final ControllableAuthRepository authRepository = new ControllableAuthRepository();
    private final SessionManager sessionManager = new SessionManager(authRepository);
    private ProfileViewModel viewModel;

    @Before
    public void setUp() {
        // Signed in as the same user the profile screen loads.
        authRepository.storedSession = true;
        sessionManager.restoreIfNeeded();
        authRepository.restoreCallback.onSuccess(ControllableProfileRepositories.user("Alex", null));
        viewModel = new ProfileViewModel(users, sessionManager);
    }

    @Test
    public void loadingThenContentFillsTheFormOnce() {
        assertEquals(UiState.Status.LOADING, viewModel.getProfile().getValue().getStatus());
        User user = ControllableProfileRepositories.user("Alex", "0912345678");

        users.getCallback.onSuccess(user);

        assertEquals(UiState.Status.SUCCESS, viewModel.getProfile().getValue().getStatus());
        assertSame(user, viewModel.getProfile().getValue().getData());
        Event<User> fill = viewModel.getFillForm().getValue();
        assertSame(user, fill.getContentIfNotHandled());
        assertNull(fill.getContentIfNotHandled());
    }

    @Test
    public void loadingThenErrorThenRetry() {
        AppError error = AppError.of(AppError.Kind.NETWORK);
        users.getCallback.onError(error);

        assertEquals(UiState.Status.ERROR, viewModel.getProfile().getValue().getStatus());
        assertSame(error, viewModel.getProfile().getValue().getError());

        viewModel.retry();
        assertEquals(UiState.Status.LOADING, viewModel.getProfile().getValue().getStatus());
        assertEquals(2, users.getCalls);

        users.getCallback.onSuccess(ControllableProfileRepositories.user("Alex", null));
        assertEquals(UiState.Status.SUCCESS, viewModel.getProfile().getValue().getStatus());
    }

    @Test
    public void retryDoesNothingUnlessFailed() {
        users.getCallback.onSuccess(ControllableProfileRepositories.user("Alex", null));

        viewModel.retry();

        assertEquals(1, users.getCalls);
    }

    @Test
    public void saveBeforeLoadIsIgnored() {
        viewModel.save("Alex", "");

        assertEquals(0, users.updateCalls);
    }

    @Test
    public void saveSendsNormalizedValuesThenShowsSavedAndUpdatesSession() {
        users.getCallback.onSuccess(ControllableProfileRepositories.user("Alex", "0912345678"));
        viewModel.getFillForm().getValue().getContentIfNotHandled();

        viewModel.save("  Alex Nguyen ", "   ");

        assertTrue(viewModel.getForm().getValue().isLoading());
        assertEquals("Alex Nguyen", users.lastDisplayName);
        assertNull(users.lastPhone);

        User saved = ControllableProfileRepositories.user("Alex Nguyen", null);
        users.updateCallback.onSuccess(saved);

        assertFalse(viewModel.getForm().getValue().isLoading());
        assertSame(saved, viewModel.getProfile().getValue().getData());
        assertSame(saved, viewModel.getFillForm().getValue().getContentIfNotHandled());
        Event<UiText> message = viewModel.getMessage().getValue();
        assertEquals(UiText.of(R.string.profile_saved), message.getContentIfNotHandled());
        assertNull(message.getContentIfNotHandled());
        AuthState session = sessionManager.getAuthState().getValue();
        assertEquals(AuthState.Status.AUTHENTICATED, session.getStatus());
        assertEquals("Alex Nguyen", session.getUser().getDisplayName());
    }

    @Test
    public void invalidInputIsRejectedWithoutCallingBackend() {
        users.getCallback.onSuccess(ControllableProfileRepositories.user("Alex", null));

        viewModel.save(" ", "12ab");

        assertEquals(UiText.of(R.string.error_display_name_required),
                viewModel.getForm().getValue().getFieldError(UserInputRules.FIELD_DISPLAY_NAME));
        assertEquals(UiText.of(R.string.error_phone_invalid),
                viewModel.getForm().getValue().getFieldError(UserInputRules.FIELD_PHONE));
        assertEquals(0, users.updateCalls);
    }

    @Test
    public void backendValidationErrorIsShownOnField() {
        users.getCallback.onSuccess(ControllableProfileRepositories.user("Alex", null));
        viewModel.save("Alex", "0912345678");

        users.updateCallback.onError(new AppError(AppError.Kind.VALIDATION, 400, "One or more validation errors occurred.",
                Collections.singletonMap("phone", Collections.singletonList("Phone must contain 7-15 digits."))));

        assertFalse(viewModel.getForm().getValue().isLoading());
        assertEquals(UiText.raw("Phone must contain 7-15 digits."),
                viewModel.getForm().getValue().getFieldError(UserInputRules.FIELD_PHONE));
        assertNull(viewModel.getForm().getValue().getError());
        assertNull(viewModel.getMessage().getValue());
    }

    @Test
    public void forbiddenAndServerErrorsAreGeneralErrors() {
        users.getCallback.onSuccess(ControllableProfileRepositories.user("Alex", null));

        viewModel.save("Alex", "");
        users.updateCallback.onError(new AppError(AppError.Kind.FORBIDDEN, 403, "This account is not active.", null));
        assertEquals(AppError.Kind.FORBIDDEN, viewModel.getForm().getValue().getError().getKind());

        viewModel.save("Alex", "");
        users.updateCallback.onError(new AppError(AppError.Kind.SERVER, 500, null, null));
        assertEquals(AppError.Kind.SERVER, viewModel.getForm().getValue().getError().getKind());
        assertEquals("Alex", sessionManager.getAuthState().getValue().getUser().getDisplayName());
    }

    @Test
    public void saveWhileSavingIsIgnored() {
        users.getCallback.onSuccess(ControllableProfileRepositories.user("Alex", null));

        viewModel.save("Alex", "");
        viewModel.save("Alex", "");

        assertEquals(1, users.updateCalls);
    }

    @Test
    public void clearingCancelsPendingRequests() {
        viewModel.onCleared();

        assertEquals(1, users.cancelled);
    }
}

package com.sportmate.app.presentation.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.UiText;
import com.sportmate.app.testutil.ControllableAuthRepository;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

public class RegisterViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableAuthRepository repository = new ControllableAuthRepository();
    private final RegisterViewModel viewModel = new RegisterViewModel(repository);

    @Test
    public void invalidInputIsRejectedWithoutCallingBackend() {
        viewModel.register("not-an-email", "short", " ", "12");

        AuthFormState state = state();
        assertEquals(UiText.of(R.string.error_email_invalid), state.getFieldError(AuthInputRules.FIELD_EMAIL));
        assertEquals(UiText.of(R.string.error_password_length), state.getFieldError(AuthInputRules.FIELD_PASSWORD));
        assertEquals(UiText.of(R.string.error_display_name_required), state.getFieldError(AuthInputRules.FIELD_DISPLAY_NAME));
        assertEquals(UiText.of(R.string.error_phone_invalid), state.getFieldError(AuthInputRules.FIELD_PHONE));
        assertEquals(0, repository.registerCalls);
    }

    @Test
    public void successSendsNormalizedInputAndLeadsToLoginOnce() {
        viewModel.register(" a@example.com ", "secret-pass1", "  Alex ", "   ");

        assertTrue(state().isLoading());
        assertEquals("a@example.com", repository.lastEmail);
        assertEquals("Alex", repository.lastDisplayName);
        assertNull(repository.lastPhone);

        repository.registerCallback.onSuccess(ControllableAuthRepository.user());

        assertFalse(state().isLoading());
        Event<String> event = viewModel.getRegistered().getValue();
        assertEquals("a@example.com", event.getContentIfNotHandled());
        assertNull(event.getContentIfNotHandled());
    }

    @Test
    public void submitWhileLoadingIsIgnored() {
        viewModel.register("a@example.com", "secret-pass1", "Alex", "");
        viewModel.register("a@example.com", "secret-pass1", "Alex", "");

        assertEquals(1, repository.registerCalls);
    }

    @Test
    public void backendValidationErrorsAreShownOnFields() {
        viewModel.register("a@example.com", "secret-pass1", "Alex", "");

        repository.registerCallback.onError(new AppError(AppError.Kind.VALIDATION, 400, "One or more validation errors occurred.",
                Collections.singletonMap("phone", Collections.singletonList("Phone must contain 7-15 digits."))));

        assertEquals(UiText.raw("Phone must contain 7-15 digits."), state().getFieldError(AuthInputRules.FIELD_PHONE));
        assertNull(state().getError());
        assertNull(viewModel.getRegistered().getValue());
    }

    @Test
    public void validationErrorForUnknownFieldBecomesGeneralError() {
        viewModel.register("a@example.com", "secret-pass1", "Alex", "");
        AppError error = new AppError(AppError.Kind.VALIDATION, 400, "One or more validation errors occurred.",
                Collections.singletonMap("something_else", Collections.singletonList("Bad.")));

        repository.registerCallback.onError(error);

        assertSame(error, state().getError());
    }

    @Test
    public void duplicateEmailIsShownOnEmailField() {
        viewModel.register("a@example.com", "secret-pass1", "Alex", "");

        repository.registerCallback.onError(new AppError(AppError.Kind.CONFLICT, 409, "An account with this email already exists.", null));

        assertEquals(UiText.raw("An account with this email already exists."), state().getFieldError(AuthInputRules.FIELD_EMAIL));
        assertNull(state().getError());
        assertFalse(state().isLoading());
    }

    @Test
    public void serverAndNetworkErrorsAreGeneralErrors() {
        viewModel.register("a@example.com", "secret-pass1", "Alex", "");
        repository.registerCallback.onError(new AppError(AppError.Kind.SERVER, 500, null, null));
        assertEquals(AppError.Kind.SERVER, state().getError().getKind());

        viewModel.register("a@example.com", "secret-pass1", "Alex", "");
        repository.registerCallback.onError(AppError.of(AppError.Kind.NETWORK));
        assertEquals(AppError.Kind.NETWORK, state().getError().getKind());
        assertFalse(state().isLoading());
        assertNull(viewModel.getRegistered().getValue());
    }

    private AuthFormState state() {
        return viewModel.getState().getValue();
    }
}

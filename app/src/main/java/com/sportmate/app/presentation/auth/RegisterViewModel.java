package com.sportmate.app.presentation.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.repository.AuthRepository;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.FormState;
import com.sportmate.app.presentation.common.UiText;
import com.sportmate.app.presentation.common.UserInputRules;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Register form. The backend answers 201 with the new user and no tokens, so success leads to the
 * login screen rather than into the app. Role, status and IDs are never sent: the backend assigns
 * them.
 */
@HiltViewModel
public final class RegisterViewModel extends ViewModel {
    static final Set<String> FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            UserInputRules.FIELD_EMAIL,
            UserInputRules.FIELD_PASSWORD,
            UserInputRules.FIELD_DISPLAY_NAME,
            UserInputRules.FIELD_PHONE)));

    private final AuthRepository authRepository;
    private final MutableLiveData<FormState> state = new MutableLiveData<>(FormState.idle());
    private final MutableLiveData<Event<String>> registered = new MutableLiveData<>();
    private Cancellable pending;

    @Inject
    public RegisterViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<FormState> getState() {
        return state;
    }

    /** Fires once with the registered email after the account is created. */
    public LiveData<Event<String>> getRegistered() {
        return registered;
    }

    public void register(String email, String password, String displayName, String phone) {
        if (current().isLoading()) {
            return;
        }
        Map<String, UiText> errors = UserInputRules.validateRegister(email, password, displayName, phone);
        if (!errors.isEmpty()) {
            state.setValue(FormState.invalid(errors));
            return;
        }

        String normalizedEmail = UserInputRules.normalizeEmail(email);
        state.setValue(FormState.loading());
        pending = authRepository.register(
                normalizedEmail,
                password,
                displayName.trim(),
                UserInputRules.normalizePhone(phone),
                new ResultCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        state.setValue(FormState.idle());
                        registered.setValue(new Event<>(normalizedEmail));
                    }

                    @Override
                    public void onError(@NonNull AppError error) {
                        state.setValue(toFormState(error));
                    }
                });
    }

    public void onFieldEdited(String field) {
        FormState current = current();
        if (!current.isLoading()) {
            state.setValue(current.withoutFieldError(field));
        }
    }

    @Override
    protected void onCleared() {
        if (pending != null) {
            pending.cancel();
        }
    }

    // Registration's only 409 is a duplicate email, so it is shown on the email input.
    private static FormState toFormState(AppError error) {
        String message = error.getMessage();
        if (error.getKind() == AppError.Kind.CONFLICT && message != null && !message.isEmpty()) {
            return FormState.invalid(Collections.singletonMap(UserInputRules.FIELD_EMAIL, UiText.raw(message)));
        }
        return FormState.fromError(error, FIELDS);
    }

    private FormState current() {
        FormState value = state.getValue();
        return value == null ? FormState.idle() : value;
    }
}

package com.sportmate.app.presentation.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.presentation.common.FormState;
import com.sportmate.app.presentation.common.UiText;
import com.sportmate.app.presentation.common.UserInputRules;
import com.sportmate.app.presentation.session.SessionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Login form. On success the session becomes authenticated and MainActivity leaves the auth flow;
 * this ViewModel does not navigate. The password is passed through, never kept.
 */
@HiltViewModel
public final class LoginViewModel extends ViewModel {
    static final Set<String> FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            UserInputRules.FIELD_EMAIL,
            UserInputRules.FIELD_PASSWORD)));

    private final SessionManager sessionManager;
    private final MutableLiveData<FormState> state = new MutableLiveData<>(FormState.idle());
    private Cancellable pending;

    @Inject
    public LoginViewModel(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public LiveData<FormState> getState() {
        return state;
    }

    public void login(String email, String password) {
        if (current().isLoading()) {
            return;
        }
        Map<String, UiText> errors = UserInputRules.validateLogin(email, password);
        if (!errors.isEmpty()) {
            state.setValue(FormState.invalid(errors));
            return;
        }

        state.setValue(FormState.loading());
        pending = sessionManager.login(UserInputRules.normalizeEmail(email), password, new ResultCallback<User>() {
            @Override
            public void onSuccess(User user) {
                state.setValue(FormState.idle());
            }

            @Override
            public void onError(@NonNull AppError error) {
                state.setValue(FormState.fromError(error, FIELDS));
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

    private FormState current() {
        FormState value = state.getValue();
        return value == null ? FormState.idle() : value;
    }
}

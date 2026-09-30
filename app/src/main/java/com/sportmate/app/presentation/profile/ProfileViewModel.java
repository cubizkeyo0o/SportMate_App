package com.sportmate.app.presentation.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.repository.UserRepository;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.FormState;
import com.sportmate.app.presentation.common.UiState;
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
 * The signed-in user's account. Loads the profile from the backend and saves the only editable
 * fields, display name and phone; email, role and status are shown read-only.
 */
@HiltViewModel
public final class ProfileViewModel extends ViewModel {
    static final Set<String> FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            UserInputRules.FIELD_DISPLAY_NAME,
            UserInputRules.FIELD_PHONE)));

    private final UserRepository userRepository;
    private final SessionManager sessionManager;
    private final MutableLiveData<UiState<User>> profile = new MutableLiveData<>();
    private final MutableLiveData<FormState> form = new MutableLiveData<>(FormState.idle());
    private final MutableLiveData<Event<User>> fillForm = new MutableLiveData<>();
    private final MutableLiveData<Event<UiText>> message = new MutableLiveData<>();
    private Cancellable pendingLoad;
    private Cancellable pendingSave;

    @Inject
    public ProfileViewModel(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
        load();
    }

    public LiveData<UiState<User>> getProfile() {
        return profile;
    }

    /** Saving state and errors of the edit form. */
    public LiveData<FormState> getForm() {
        return form;
    }

    /** Fires with the values to put in the edit fields after loading or saving. */
    public LiveData<Event<User>> getFillForm() {
        return fillForm;
    }

    public LiveData<Event<UiText>> getMessage() {
        return message;
    }

    public void retry() {
        UiState<User> current = profile.getValue();
        if (current != null && current.getStatus() == UiState.Status.ERROR) {
            load();
        }
    }

    public void save(String displayName, String phone) {
        if (currentForm().isLoading() || !isLoaded()) {
            return;
        }
        Map<String, UiText> errors = UserInputRules.validateProfile(displayName, phone);
        if (!errors.isEmpty()) {
            form.setValue(FormState.invalid(errors));
            return;
        }

        form.setValue(FormState.loading());
        pendingSave = userRepository.updateProfile(
                displayName.trim(),
                UserInputRules.normalizePhone(phone),
                new ResultCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        profile.setValue(UiState.success(user));
                        form.setValue(FormState.idle());
                        fillForm.setValue(new Event<>(user));
                        message.setValue(new Event<>(UiText.of(R.string.profile_saved)));
                        sessionManager.onCurrentUserUpdated(user);
                    }

                    @Override
                    public void onError(@NonNull AppError error) {
                        form.setValue(FormState.fromError(error, FIELDS));
                    }
                });
    }

    public void onFieldEdited(String field) {
        FormState current = currentForm();
        if (!current.isLoading()) {
            form.setValue(current.withoutFieldError(field));
        }
    }

    @Override
    protected void onCleared() {
        cancel(pendingLoad);
        cancel(pendingSave);
    }

    private void load() {
        profile.setValue(UiState.loading());
        pendingLoad = userRepository.getCurrentUser(new ResultCallback<User>() {
            @Override
            public void onSuccess(User user) {
                profile.setValue(UiState.success(user));
                fillForm.setValue(new Event<>(user));
            }

            @Override
            public void onError(@NonNull AppError error) {
                profile.setValue(UiState.error(error));
            }
        });
    }

    private boolean isLoaded() {
        UiState<User> current = profile.getValue();
        return current != null && current.getStatus() == UiState.Status.SUCCESS;
    }

    private FormState currentForm() {
        FormState value = form.getValue();
        return value == null ? FormState.idle() : value;
    }

    private static void cancel(Cancellable cancellable) {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }
}

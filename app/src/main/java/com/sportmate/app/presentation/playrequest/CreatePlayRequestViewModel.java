package com.sportmate.app.presentation.playrequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.PlayRequest;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.UserSportProfile;
import com.sportmate.app.domain.repository.PlayRequestRepository;
import com.sportmate.app.domain.repository.UserSportProfileRepository;
import com.sportmate.app.presentation.common.UiState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public final class CreatePlayRequestViewModel extends ViewModel {
    private final UserSportProfileRepository profileRepository;
    private final PlayRequestRepository requestRepository;
    private final MutableLiveData<UiState<List<UserSportProfile>>> profiles = new MutableLiveData<>();
    private final MutableLiveData<UiState<PlayRequest>> created = new MutableLiveData<>();
    @Nullable private Cancellable pendingProfiles;
    @Nullable private Cancellable pendingCreate;

    @Inject
    public CreatePlayRequestViewModel(
            UserSportProfileRepository profileRepository,
            PlayRequestRepository requestRepository) {
        this.profileRepository = profileRepository;
        this.requestRepository = requestRepository;
        loadProfiles();
    }

    public LiveData<UiState<List<UserSportProfile>>> getProfiles() {
        return profiles;
    }

    public LiveData<UiState<PlayRequest>> getCreated() {
        return created;
    }

    public void retryProfiles() {
        UiState<List<UserSportProfile>> current = profiles.getValue();
        if (current != null && current.getStatus() == UiState.Status.ERROR) {
            loadProfiles();
        }
    }

    public void create(
            @NonNull Sport sport,
            @NonNull String title,
            @Nullable String description,
            long startsAtUtcMillis,
            long endsAtUtcMillis,
            int spotsNeeded,
            double latitude,
            double longitude,
            @Nullable String minSkill,
            @Nullable String maxSkill,
            @Nullable String locationName) {
        cancel(pendingCreate);
        created.setValue(UiState.loading());
        pendingCreate = requestRepository.create(
                sport.getId(), title, description, startsAtUtcMillis, endsAtUtcMillis,
                spotsNeeded, latitude, longitude, minSkill, maxSkill, locationName,
                new ResultCallback<PlayRequest>() {
                    @Override
                    public void onSuccess(PlayRequest result) {
                        created.setValue(UiState.success(result));
                    }

                    @Override
                    public void onError(@NonNull AppError error) {
                        created.setValue(UiState.error(error));
                    }
                });
    }

    @Override
    protected void onCleared() {
        cancel(pendingProfiles);
        cancel(pendingCreate);
    }

    private void loadProfiles() {
        profiles.setValue(UiState.loading());
        pendingProfiles = profileRepository.getSportProfiles(new ResultCallback<List<UserSportProfile>>() {
            @Override
            public void onSuccess(List<UserSportProfile> result) {
                List<UserSportProfile> active = new ArrayList<>();
                for (UserSportProfile profile : result) {
                    if (profile.isSportActive()) {
                        active.add(profile);
                    }
                }
                profiles.setValue(UiState.success(active));
            }

            @Override
            public void onError(@NonNull AppError error) {
                profiles.setValue(UiState.error(error));
            }
        });
    }

    private static void cancel(@Nullable Cancellable cancellable) {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }
}

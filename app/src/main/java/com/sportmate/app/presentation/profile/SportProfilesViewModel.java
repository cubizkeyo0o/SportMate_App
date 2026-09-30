package com.sportmate.app.presentation.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.UserSportProfile;
import com.sportmate.app.domain.repository.SportRepository;
import com.sportmate.app.domain.repository.UserSportProfileRepository;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.common.UiText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * The signed-in user's sport profiles. Loads them together with the active sport catalog, which
 * provides the sports that can still be added. One change runs at a time; the list is updated
 * from the backend's response rather than reloaded.
 */
@HiltViewModel
public final class SportProfilesViewModel extends ViewModel {
    // Same order as the backend: by sport name.
    private static final Comparator<UserSportProfile> BY_SPORT_NAME = (a, b) ->
            String.CASE_INSENSITIVE_ORDER.compare(a.getSport().getName(), b.getSport().getName());

    private final UserSportProfileRepository profileRepository;
    private final SportRepository sportRepository;
    private final MutableLiveData<UiState<List<UserSportProfile>>> profiles = new MutableLiveData<>();
    private final MutableLiveData<List<Sport>> addableSports = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> updating = new MutableLiveData<>(false);
    private final MutableLiveData<Event<UiText>> message = new MutableLiveData<>();

    private List<Sport> catalog = Collections.emptyList();
    private Cancellable pendingProfiles;
    private Cancellable pendingCatalog;
    private Cancellable pendingChange;
    private int loadGeneration;

    @Inject
    public SportProfilesViewModel(UserSportProfileRepository profileRepository, SportRepository sportRepository) {
        this.profileRepository = profileRepository;
        this.sportRepository = sportRepository;
        load();
    }

    /** Success with an empty list is the empty state. */
    public LiveData<UiState<List<UserSportProfile>>> getProfiles() {
        return profiles;
    }

    /** Active sports the user has no profile for yet. */
    public LiveData<List<Sport>> getAddableSports() {
        return addableSports;
    }

    /** Whether a change (set or remove) is in flight. */
    public LiveData<Boolean> getUpdating() {
        return updating;
    }

    /** One-time confirmation or error text for a change. */
    public LiveData<Event<UiText>> getMessage() {
        return message;
    }

    public void retry() {
        UiState<List<UserSportProfile>> current = profiles.getValue();
        if (current != null && current.getStatus() == UiState.Status.ERROR) {
            load();
        }
    }

    /** Adds a profile for the sport or changes its skill level. */
    public void setSkillLevel(String sportId, SkillLevel skillLevel) {
        if (!canChange()) {
            return;
        }
        updating.setValue(true);
        pendingChange = profileRepository.setSportProfile(sportId, skillLevel, new ResultCallback<UserSportProfile>() {
            @Override
            public void onSuccess(UserSportProfile saved) {
                List<UserSportProfile> updated = withoutSport(currentProfiles(), saved.getSport().getId());
                updated.add(saved);
                Collections.sort(updated, BY_SPORT_NAME);
                showProfiles(updated);
                finishChange(UiText.of(R.string.sport_profile_saved));
            }

            @Override
            public void onError(@NonNull AppError error) {
                finishChange(changeErrorText(error));
            }
        });
    }

    public void remove(String sportId) {
        if (!canChange()) {
            return;
        }
        updating.setValue(true);
        pendingChange = profileRepository.removeSportProfile(sportId, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                showProfiles(withoutSport(currentProfiles(), sportId));
                finishChange(UiText.of(R.string.sport_profile_removed));
            }

            @Override
            public void onError(@NonNull AppError error) {
                finishChange(changeErrorText(error));
            }
        });
    }

    @Override
    protected void onCleared() {
        cancel(pendingProfiles);
        cancel(pendingCatalog);
        cancel(pendingChange);
    }

    private void load() {
        cancel(pendingProfiles);
        cancel(pendingCatalog);
        int generation = ++loadGeneration;
        profiles.setValue(UiState.loading());
        LoadResult result = new LoadResult();

        pendingProfiles = profileRepository.getSportProfiles(new ResultCallback<List<UserSportProfile>>() {
            @Override
            public void onSuccess(List<UserSportProfile> loaded) {
                result.profiles = loaded;
                onLoadPart(generation, result);
            }

            @Override
            public void onError(@NonNull AppError error) {
                onLoadFailed(generation, result, error);
            }
        });
        pendingCatalog = sportRepository.getSports(new ResultCallback<List<Sport>>() {
            @Override
            public void onSuccess(List<Sport> sports) {
                result.catalog = sports;
                onLoadPart(generation, result);
            }

            @Override
            public void onError(@NonNull AppError error) {
                onLoadFailed(generation, result, error);
            }
        });
    }

    private void onLoadPart(int generation, LoadResult result) {
        if (generation != loadGeneration || result.failed || result.profiles == null || result.catalog == null) {
            return;
        }
        catalog = result.catalog;
        showProfiles(new ArrayList<>(result.profiles));
    }

    // The first failure wins; the other request is no longer needed.
    private void onLoadFailed(int generation, LoadResult result, AppError error) {
        if (generation != loadGeneration || result.failed) {
            return;
        }
        result.failed = true;
        cancel(pendingProfiles);
        cancel(pendingCatalog);
        profiles.setValue(UiState.error(error));
    }

    private void showProfiles(List<UserSportProfile> list) {
        List<UserSportProfile> unmodifiable = Collections.unmodifiableList(list);
        profiles.setValue(UiState.success(unmodifiable));

        Set<String> configured = new HashSet<>();
        for (UserSportProfile profile : unmodifiable) {
            configured.add(profile.getSport().getId());
        }
        List<Sport> addable = new ArrayList<>();
        for (Sport sport : catalog) {
            if (!configured.contains(sport.getId())) {
                addable.add(sport);
            }
        }
        addableSports.setValue(Collections.unmodifiableList(addable));
    }

    private boolean canChange() {
        UiState<List<UserSportProfile>> current = profiles.getValue();
        return !Boolean.TRUE.equals(updating.getValue())
                && current != null
                && current.getStatus() == UiState.Status.SUCCESS;
    }

    private void finishChange(UiText text) {
        updating.setValue(false);
        message.setValue(new Event<>(text));
    }

    private List<UserSportProfile> currentProfiles() {
        UiState<List<UserSportProfile>> current = profiles.getValue();
        List<UserSportProfile> data = current == null ? null : current.getData();
        return data == null ? Collections.emptyList() : data;
    }

    private static List<UserSportProfile> withoutSport(List<UserSportProfile> list, String sportId) {
        List<UserSportProfile> result = new ArrayList<>(list.size());
        for (UserSportProfile profile : list) {
            if (!profile.getSport().getId().equals(sportId)) {
                result.add(profile);
            }
        }
        return result;
    }

    // These requests have no input fields, so a validation message (e.g. about sport_id or
    // skill_level) is shown directly instead of the generic validation title.
    private static UiText changeErrorText(AppError error) {
        if (error.getKind() == AppError.Kind.VALIDATION) {
            String first = firstFieldMessage(error.getFieldErrors());
            if (first != null) {
                return UiText.raw(first);
            }
        }
        return ErrorMessages.toUiText(error);
    }

    @Nullable
    private static String firstFieldMessage(Map<String, List<String>> fieldErrors) {
        for (List<String> messages : fieldErrors.values()) {
            if (messages != null && !messages.isEmpty()) {
                return messages.get(0);
            }
        }
        return null;
    }

    private static void cancel(Cancellable cancellable) {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }

    private static final class LoadResult {
        List<UserSportProfile> profiles;
        List<Sport> catalog;
        boolean failed;
    }
}

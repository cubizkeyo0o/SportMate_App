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
import com.sportmate.app.domain.model.PlayRequestPage;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.repository.PlayRequestRepository;
import com.sportmate.app.domain.repository.SportRepository;
import com.sportmate.app.presentation.common.UiState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public final class PlayRequestsViewModel extends ViewModel {
    public static final int MODE_DISCOVER = 0;
    public static final int MODE_HOSTED = 1;
    public static final int MODE_PARTICIPATING = 2;
    private static final int PAGE_SIZE = 20;

    private final PlayRequestRepository requests;
    private final SportRepository sportsRepository;
    private final MutableLiveData<UiState<PlayRequestPage>> results = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<Sport>>> sports = new MutableLiveData<>();
    private Cancellable pendingRequests;
    private Cancellable pendingSports;
    private int mode = MODE_DISCOVER;
    private int page = 1;
    @Nullable private String sportId;
    @Nullable private String startsFromUtc;
    @Nullable private String startsToUtc;
    @Nullable private Double latitude;
    @Nullable private Double longitude;
    @Nullable private Double radiusKm;

    @Inject
    public PlayRequestsViewModel(PlayRequestRepository requests, SportRepository sportsRepository) {
        this.requests = requests;
        this.sportsRepository = sportsRepository;
        loadSports();
    }

    public LiveData<UiState<PlayRequestPage>> getResults() {
        return results;
    }

    public LiveData<UiState<List<Sport>>> getSports() {
        return sports;
    }

    public void setMode(int mode) {
        if (this.mode != mode) {
            this.mode = mode;
            loadFirstPage();
        }
    }

    public void setFilters(
            @Nullable String sportId,
            @Nullable String startsFromUtc,
            @Nullable String startsToUtc,
            @Nullable Double latitude,
            @Nullable Double longitude,
            @Nullable Double radiusKm) {
        this.sportId = sportId;
        this.startsFromUtc = startsFromUtc;
        this.startsToUtc = startsToUtc;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
        if (mode == MODE_DISCOVER) {
            loadFirstPage();
        }
    }

    public void refresh() {
        loadFirstPage();
    }

    public void retry() {
        UiState<PlayRequestPage> current = results.getValue();
        if (current != null && current.getStatus() == UiState.Status.ERROR) {
            loadFirstPage();
        }
    }

    public void loadNextPage() {
        UiState<PlayRequestPage> current = results.getValue();
        if (current == null || current.getStatus() != UiState.Status.SUCCESS
                || current.getData() == null || !current.getData().hasNextPage()) {
            return;
        }
        page = current.getData().getPage() + 1;
        loadPage(page, current.getData().getItems());
    }

    @Override
    protected void onCleared() {
        cancel(pendingRequests);
        cancel(pendingSports);
    }

    private void loadSports() {
        sports.setValue(UiState.loading());
        pendingSports = sportsRepository.getSports(new ResultCallback<List<Sport>>() {
            @Override
            public void onSuccess(List<Sport> result) {
                sports.setValue(UiState.success(result));
            }

            @Override
            public void onError(@NonNull AppError error) {
                sports.setValue(UiState.error(error));
            }
        });
    }

    private void loadFirstPage() {
        page = 1;
        loadPage(page, null);
    }

    private void loadPage(int page, @Nullable List<PlayRequest> previousItems) {
        cancel(pendingRequests);
        if (previousItems == null) {
            results.setValue(UiState.loading());
        }
        ResultCallback<PlayRequestPage> callback = new ResultCallback<PlayRequestPage>() {
            @Override
            public void onSuccess(PlayRequestPage result) {
                if (previousItems == null) {
                    results.setValue(UiState.success(result));
                    return;
                }
                List<PlayRequest> allItems = new ArrayList<>(previousItems);
                allItems.addAll(result.getItems());
                results.setValue(UiState.success(new PlayRequestPage(
                        allItems, result.getPage(), result.getPageSize(), result.getTotalCount())));
            }

            @Override
            public void onError(@NonNull AppError error) {
                results.setValue(UiState.error(error));
            }
        };
        if (mode == MODE_HOSTED) {
            pendingRequests = requests.getMine(page, PAGE_SIZE, callback);
        } else if (mode == MODE_PARTICIPATING) {
            pendingRequests = requests.getMyParticipations(page, PAGE_SIZE, callback);
        } else {
            pendingRequests = requests.discover(
                    sportId, startsFromUtc, startsToUtc, latitude, longitude, radiusKm,
                    page, PAGE_SIZE, callback);
        }
    }

    private static void cancel(@Nullable Cancellable cancellable) {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }
}

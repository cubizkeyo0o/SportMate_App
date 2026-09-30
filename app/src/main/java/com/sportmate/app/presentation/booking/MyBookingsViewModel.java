package com.sportmate.app.presentation.booking;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.repository.BookingRepository;
import com.sportmate.app.domain.repository.CourtDiscoveryRepository;
import com.sportmate.app.presentation.common.UiState;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * The signed-in user's bookings. Court names come from the court discovery list, which only has
 * active courts; bookings of other courts are shown without a name. Failing to load names does not
 * fail the list.
 */
@HiltViewModel
public final class MyBookingsViewModel extends ViewModel {
    private final BookingRepository bookings;
    private final CourtDiscoveryRepository courts;
    private final MutableLiveData<UiState<List<Booking>>> list = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> courtNames = new MutableLiveData<>(Collections.emptyMap());
    private Cancellable pendingList;
    private Cancellable pendingCourts;

    @Inject
    public MyBookingsViewModel(BookingRepository bookings, CourtDiscoveryRepository courts) {
        this.bookings = bookings;
        this.courts = courts;
        fetch(true);
        loadCourtNames();
    }

    /** Success with an empty list is the empty state. */
    public LiveData<UiState<List<Booking>>> getBookings() {
        return list;
    }

    /** Court id to name, for courts that are currently listed. */
    public LiveData<Map<String, String>> getCourtNames() {
        return courtNames;
    }

    public void retry() {
        UiState<List<Booking>> current = list.getValue();
        if (current != null && current.getStatus() == UiState.Status.ERROR) {
            fetch(true);
            if (courtNames.getValue() == null || courtNames.getValue().isEmpty()) {
                loadCourtNames();
            }
        }
    }

    /** Reloads without leaving the content, e.g. when returning from a booking. */
    public void refresh() {
        UiState<List<Booking>> current = list.getValue();
        if (current != null && current.getStatus() == UiState.Status.SUCCESS) {
            fetch(false);
        }
    }

    @Override
    protected void onCleared() {
        cancel(pendingList);
        cancel(pendingCourts);
    }

    private void fetch(boolean showLoading) {
        cancel(pendingList);
        if (showLoading) {
            list.setValue(UiState.loading());
        }
        pendingList = bookings.getMyBookings(new ResultCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> result) {
                list.setValue(UiState.success(result));
            }

            @Override
            public void onError(@NonNull AppError error) {
                if (showLoading) {
                    list.setValue(UiState.error(error));
                }
            }
        });
    }

    private void loadCourtNames() {
        pendingCourts = courts.getCourts(new ResultCallback<List<Court>>() {
            @Override
            public void onSuccess(List<Court> result) {
                Map<String, String> names = new HashMap<>();
                for (Court court : result) {
                    names.put(court.getId(), court.getName());
                }
                courtNames.setValue(Collections.unmodifiableMap(names));
            }

            @Override
            public void onError(@NonNull AppError error) {
                // Names are optional; bookings are still shown.
            }
        });
    }

    private static void cancel(Cancellable cancellable) {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }
}

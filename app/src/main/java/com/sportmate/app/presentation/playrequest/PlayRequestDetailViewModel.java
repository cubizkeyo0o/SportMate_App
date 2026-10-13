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
import com.sportmate.app.domain.model.PlayRequestParticipant;
import com.sportmate.app.domain.repository.PlayRequestRepository;
import com.sportmate.app.presentation.common.UiState;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public final class PlayRequestDetailViewModel extends ViewModel {
    public static final String ARG_REQUEST_ID = "playRequestId";

    private final PlayRequestRepository repository;
    private final MutableLiveData<UiState<PlayRequest>> request = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<PlayRequestParticipant>>> participants = new MutableLiveData<>();
    private final MutableLiveData<UiState<PlayRequest>> action = new MutableLiveData<>();
    @Nullable private String requestId;
    @Nullable private Cancellable pendingRequest;
    @Nullable private Cancellable pendingParticipants;
    @Nullable private Cancellable pendingAction;

    @Inject
    public PlayRequestDetailViewModel(PlayRequestRepository repository) {
        this.repository = repository;
    }

    public LiveData<UiState<PlayRequest>> getRequest() {
        return request;
    }

    public LiveData<UiState<List<PlayRequestParticipant>>> getParticipants() {
        return participants;
    }

    public LiveData<UiState<PlayRequest>> getAction() {
        return action;
    }

    public void load(String id) {
        requestId = id;
        cancel(pendingRequest);
        request.setValue(UiState.loading());
        pendingRequest = repository.get(id, new ResultCallback<PlayRequest>() {
            @Override
            public void onSuccess(PlayRequest result) {
                request.setValue(UiState.success(result));
                if (result.isHost()) {
                    loadParticipants(id);
                } else {
                    participants.setValue(UiState.success(result.getParticipants()));
                }
            }

            @Override
            public void onError(@NonNull AppError error) {
                request.setValue(UiState.error(error));
            }
        });
    }

    public void performAction(int actionCode, @Nullable String participantId) {
        if (requestId == null) {
            return;
        }
        cancel(pendingAction);
        action.setValue(UiState.loading());
        ResultCallback<PlayRequest> callback = new ResultCallback<PlayRequest>() {
            @Override
            public void onSuccess(PlayRequest result) {
                action.setValue(UiState.success(result));
                load(requestId);
            }

            @Override
            public void onError(@NonNull AppError error) {
                action.setValue(UiState.error(error));
            }
        };
        switch (actionCode) {
            case Action.JOIN:
                pendingAction = repository.join(requestId, callback);
                break;
            case Action.WITHDRAW:
                pendingAction = repository.withdraw(requestId, callback);
                break;
            case Action.ACCEPT:
                if (participantId != null) pendingAction = repository.accept(requestId, participantId, callback);
                break;
            case Action.REJECT:
                if (participantId != null) pendingAction = repository.reject(requestId, participantId, callback);
                break;
            case Action.CANCEL:
                pendingAction = repository.cancel(requestId, callback);
                break;
            case Action.COMPLETE:
                pendingAction = repository.complete(requestId, callback);
                break;
            default:
                action.setValue(UiState.error(AppError.of(AppError.Kind.UNKNOWN)));
        }
    }

    @Override
    protected void onCleared() {
        cancel(pendingRequest);
        cancel(pendingParticipants);
        cancel(pendingAction);
    }

    private void loadParticipants(String id) {
        cancel(pendingParticipants);
        participants.setValue(UiState.loading());
        pendingParticipants = repository.getParticipants(id, new ResultCallback<List<PlayRequestParticipant>>() {
            @Override
            public void onSuccess(List<PlayRequestParticipant> result) {
                participants.setValue(UiState.success(result));
            }

            @Override
            public void onError(@NonNull AppError error) {
                participants.setValue(UiState.error(error));
            }
        });
    }

    private static void cancel(@Nullable Cancellable cancellable) {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }

    public static final class Action {
        public static final int JOIN = 1;
        public static final int WITHDRAW = 2;
        public static final int ACCEPT = 3;
        public static final int REJECT = 4;
        public static final int CANCEL = 5;
        public static final int COMPLETE = 6;
        private Action() { }
    }
}

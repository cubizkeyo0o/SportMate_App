package com.sportmate.app.presentation.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public final class HomeViewModel extends ViewModel {
    private final MutableLiveData<String> title = new MutableLiveData<>("SportMate");

    public LiveData<String> getTitle() {
        return title;
    }
}
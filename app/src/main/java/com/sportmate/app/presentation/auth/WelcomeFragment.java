package com.sportmate.app.presentation.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sportmate.app.databinding.FragmentWelcomeBinding;

/** Entry point of the auth flow. Placeholder until the login and register screens exist. */
public final class WelcomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return FragmentWelcomeBinding.inflate(inflater, container, false).getRoot();
    }
}

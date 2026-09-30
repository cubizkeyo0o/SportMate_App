package com.sportmate.app.presentation.splash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.sportmate.app.databinding.FragmentSplashBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.session.AuthState;
import com.sportmate.app.presentation.session.SessionViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Shown while the stored session is checked. MainActivity navigates away once the session is
 * resolved; if the check fails for a non-auth reason this screen offers a retry.
 */
@AndroidEntryPoint
public final class SplashFragment extends Fragment {
    private FragmentSplashBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSplashBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SessionViewModel sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        binding.retryButton.setOnClickListener(v -> sessionViewModel.retry());
        sessionViewModel.getAuthState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(AuthState state) {
        boolean failed = state.getStatus() == AuthState.Status.ERROR;
        binding.progress.setVisibility(failed ? View.GONE : View.VISIBLE);
        binding.retryButton.setVisibility(failed ? View.VISIBLE : View.GONE);
        binding.errorMessage.setVisibility(failed ? View.VISIBLE : View.GONE);
        AppError error = state.getError();
        binding.errorMessage.setText(error == null ? null : ErrorMessages.resolve(requireContext(), error));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

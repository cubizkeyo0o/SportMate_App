package com.sportmate.app.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.sportmate.app.R;
import com.sportmate.app.databinding.FragmentHomeBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.session.SessionViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding.retryButton.setOnClickListener(v -> viewModel.checkBackend());
        viewModel.getBackendStatus().observe(getViewLifecycleOwner(), this::render);

        binding.courtsButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_home_to_courts));
        binding.myBookingsButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_home_to_my_bookings));
        binding.profileButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_home_to_profile));

        // Logging out makes MainActivity return to the auth flow.
        SessionViewModel sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        binding.logoutButton.setOnClickListener(v -> sessionViewModel.logout());
        sessionViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            User user = state.getUser();
            binding.signedInAs.setText(user == null ? null : getString(R.string.home_signed_in_as, user.getDisplayName()));
        });
    }

    private void render(UiState<Boolean> state) {
        boolean loading = state.getStatus() == UiState.Status.LOADING;
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.retryButton.setVisibility(state.getStatus() == UiState.Status.ERROR ? View.VISIBLE : View.GONE);

        switch (state.getStatus()) {
            case LOADING:
                binding.backendStatus.setText(R.string.home_backend_checking);
                break;
            case SUCCESS:
                binding.backendStatus.setText(Boolean.TRUE.equals(state.getData())
                        ? R.string.home_backend_ok
                        : R.string.home_backend_unhealthy);
                break;
            case ERROR:
                AppError error = state.getError();
                binding.backendStatus.setText(error == null
                        ? getString(R.string.error_unknown)
                        : ErrorMessages.resolve(requireContext(), error));
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

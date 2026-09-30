package com.sportmate.app.presentation.courts;

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
import com.sportmate.app.databinding.FragmentCourtsBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.UiState;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Browse active courts. */
@AndroidEntryPoint
public final class CourtsFragment extends Fragment implements CourtsAdapter.Listener {
    private FragmentCourtsBinding binding;
    private CourtsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCourtsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        CourtsViewModel viewModel = new ViewModelProvider(this).get(CourtsViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        adapter = new CourtsAdapter(this);
        binding.courtList.setAdapter(adapter);
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        viewModel.getCourts().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(UiState<List<Court>> state) {
        UiState.Status status = state.getStatus();
        List<Court> courts = state.getData();
        boolean empty = status == UiState.Status.SUCCESS && (courts == null || courts.isEmpty());

        binding.progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.emptyMessage.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.courtList.setVisibility(status == UiState.Status.SUCCESS && !empty ? View.VISIBLE : View.GONE);

        AppError error = state.getError();
        binding.errorMessage.setText(error == null ? null : ErrorMessages.resolve(requireContext(), error));
        if (status == UiState.Status.SUCCESS) {
            adapter.submitList(courts);
        }
    }

    @Override
    public void onCourtSelected(Court court) {
        Bundle args = new Bundle();
        args.putString(CourtDetailViewModel.ARG_COURT_ID, court.getId());
        NavHostFragment.findNavController(this).navigate(R.id.action_courts_to_court_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }
}

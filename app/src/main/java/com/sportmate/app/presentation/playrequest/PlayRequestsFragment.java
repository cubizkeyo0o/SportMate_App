package com.sportmate.app.presentation.playrequest;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ConfigurationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.sportmate.app.R;
import com.sportmate.app.data.remote.UtcTimestamps;
import com.sportmate.app.databinding.FragmentPlayRequestsBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.PlayRequest;
import com.sportmate.app.domain.model.PlayRequestPage;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.UiState;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class PlayRequestsFragment extends Fragment implements PlayRequestsAdapter.Listener {
    private FragmentPlayRequestsBinding binding;
    private PlayRequestsViewModel viewModel;
    private PlayRequestsAdapter adapter;
    private final List<Sport> sports = new ArrayList<>();
    @Nullable private Calendar selectedDate;
    private boolean restoringMode;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayRequestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(PlayRequestsViewModel.class);
        adapter = new PlayRequestsAdapter(
                this,
                ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0));
        binding.requestList.setAdapter(adapter);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.loadMore.setOnClickListener(v -> viewModel.loadNextPage());
        binding.createRequest.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_play_requests_to_create));
        binding.dateFilter.setOnClickListener(v -> chooseFilterDate());
        binding.applyFilters.setOnClickListener(v -> applyFilters());
        binding.clearFilters.setOnClickListener(v -> clearFilters());
        binding.modeGroup.addOnButtonCheckedListener(this::onModeChecked);
        binding.modeGroup.check(R.id.discover_mode);
        List<String> defaultSport = new ArrayList<>();
        defaultSport.add(getString(R.string.play_all_sports));
        binding.sportFilter.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, defaultSport));

        viewModel.getSports().observe(getViewLifecycleOwner(), this::renderSports);
        viewModel.getResults().observe(getViewLifecycleOwner(), this::renderResults);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    @Override
    public void onPlayRequestSelected(PlayRequest request) {
        Bundle args = new Bundle();
        args.putString(PlayRequestDetailViewModel.ARG_REQUEST_ID, request.getId());
        NavHostFragment.findNavController(this).navigate(R.id.action_play_requests_to_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }

    private void onModeChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
        if (!isChecked || restoringMode) {
            return;
        }
        int mode = checkedId == R.id.hosted_mode
                ? PlayRequestsViewModel.MODE_HOSTED
                : checkedId == R.id.participating_mode
                ? PlayRequestsViewModel.MODE_PARTICIPATING
                : PlayRequestsViewModel.MODE_DISCOVER;
        viewModel.setMode(mode);
        binding.filterCard.setVisibility(mode == PlayRequestsViewModel.MODE_DISCOVER ? View.VISIBLE : View.GONE);
        binding.emptyMessage.setText(mode == PlayRequestsViewModel.MODE_HOSTED
                ? R.string.play_hosted_empty
                : mode == PlayRequestsViewModel.MODE_PARTICIPATING
                ? R.string.play_joined_empty
                : R.string.play_requests_empty);
    }

    private void renderSports(UiState<List<Sport>> state) {
        if (state.getStatus() != UiState.Status.SUCCESS || state.getData() == null) {
            return;
        }
        sports.clear();
        sports.addAll(state.getData());
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.play_all_sports));
        for (Sport sport : sports) {
            labels.add(sport.getName());
        }
        binding.sportFilter.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void renderResults(UiState<PlayRequestPage> state) {
        UiState.Status status = state.getStatus();
        PlayRequestPage page = state.getData();
        List<PlayRequest> items = page == null ? null : page.getItems();
        boolean empty = status == UiState.Status.SUCCESS && (items == null || items.isEmpty());
        binding.progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.emptyMessage.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.requestList.setVisibility(status == UiState.Status.SUCCESS && !empty ? View.VISIBLE : View.GONE);
        binding.loadMore.setVisibility(status == UiState.Status.SUCCESS && page != null && page.hasNextPage()
                ? View.VISIBLE : View.GONE);
        AppError error = state.getError();
        binding.errorMessage.setText(error == null ? null : ErrorMessages.resolve(requireContext(), error));
        if (status == UiState.Status.SUCCESS && page != null) {
            adapter.submitList(page.getItems());
        }
    }

    private void chooseFilterDate() {
        Calendar initial = selectedDate == null ? Calendar.getInstance() : selectedDate;
        new DatePickerDialog(
                requireContext(),
                (picker, year, month, dayOfMonth) -> {
                    selectedDate = Calendar.getInstance();
                    selectedDate.clear();
                    selectedDate.set(year, month, dayOfMonth);
                    binding.dateFilter.setText(DateFormat.getDateInstance(
                            DateFormat.MEDIUM,
                            ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0))
                            .format(selectedDate.getTime()));
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void applyFilters() {
        String start = null;
        String end = null;
        Double lat = null;
        Double lon = null;
        Double radius = null;
        if (selectedDate != null) {
            Calendar startOfDay = (Calendar) selectedDate.clone();
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);
            Calendar endOfDay = (Calendar) startOfDay.clone();
            endOfDay.add(Calendar.DAY_OF_MONTH, 1);
            endOfDay.add(Calendar.MINUTE, -1);
            start = UtcTimestamps.format(startOfDay.getTimeInMillis());
            end = UtcTimestamps.format(endOfDay.getTimeInMillis());
        }

        String latText = text(binding.latitudeFilter);
        String lonText = text(binding.longitudeFilter);
        String radiusText = text(binding.radiusFilter);
        boolean anyLocation = !latText.isEmpty() || !lonText.isEmpty() || !radiusText.isEmpty();
        if (anyLocation) {
            try {
                if (latText.isEmpty() || lonText.isEmpty() || radiusText.isEmpty()) {
                    throw new IllegalArgumentException();
                }
                lat = new BigDecimal(latText).doubleValue();
                lon = new BigDecimal(lonText).doubleValue();
                radius = new BigDecimal(radiusText).doubleValue();
                if (!Double.isFinite(lat) || lat < -90 || lat > 90
                        || !Double.isFinite(lon) || lon < -180 || lon > 180
                        || !Double.isFinite(radius) || radius <= 0 || radius > 100) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException exception) {
                binding.filterError.setText(R.string.play_filter_location_error);
                binding.filterError.setVisibility(View.VISIBLE);
                return;
            }
        }

        binding.filterError.setVisibility(View.GONE);
        String selectedSportId = binding.sportFilter.getSelectedItemPosition() == 0
                ? null
                : sports.get(binding.sportFilter.getSelectedItemPosition() - 1).getId();
        viewModel.setFilters(selectedSportId, start, end, lat, lon, radius);
    }

    private void clearFilters() {
        selectedDate = null;
        binding.dateFilter.setText(R.string.play_filter_date);
        binding.sportFilter.setSelection(0);
        binding.latitudeFilter.setText(null);
        binding.longitudeFilter.setText(null);
        binding.radiusFilter.setText(null);
        binding.filterError.setVisibility(View.GONE);
        viewModel.setFilters(null, null, null, null, null, null);
    }

    private static String text(android.widget.EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}

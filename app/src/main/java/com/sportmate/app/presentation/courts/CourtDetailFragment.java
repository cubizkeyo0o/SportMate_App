package com.sportmate.app.presentation.courts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ConfigurationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.sportmate.app.R;
import com.sportmate.app.databinding.FragmentCourtDetailBinding;
import com.sportmate.app.databinding.ItemCourtSportBinding;
import com.sportmate.app.databinding.ItemOperatingDayBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.MoneyFormats;
import com.sportmate.app.presentation.booking.BookCourtViewModel;
import com.sportmate.app.presentation.common.UiState;

import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A court's information, the sports it offers and its weekly operating hours. Hours are shown in
 * the court's local time exactly as configured; nothing is converted to the device time zone.
 */
@AndroidEntryPoint
public final class CourtDetailFragment extends Fragment {
    private FragmentCourtDetailBinding binding;
    private CourtDetailViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCourtDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(CourtDetailViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.sportsRetryButton.setOnClickListener(v -> viewModel.retrySports());
        binding.hoursRetryButton.setOnClickListener(v -> viewModel.retryOperatingHours());
        binding.bookButton.setOnClickListener(v -> openBooking());

        viewModel.getCourt().observe(getViewLifecycleOwner(), this::renderCourt);
        viewModel.getSports().observe(getViewLifecycleOwner(), this::renderSports);
        viewModel.getOperatingHours().observe(getViewLifecycleOwner(), this::renderHours);
        viewModel.getCourt().observe(getViewLifecycleOwner(), state -> renderBooking());
        viewModel.getSports().observe(getViewLifecycleOwner(), state -> renderBooking());
        viewModel.getOperatingHours().observe(getViewLifecycleOwner(), state -> renderBooking());
    }

    private void renderBooking() {
        BookingEligibility eligibility = BookingEligibility.of(
                viewModel.getCourt().getValue(), viewModel.getSports().getValue(),
                viewModel.getOperatingHours().getValue());
        binding.bookButton.setEnabled(eligibility.canBook);
        binding.bookUnavailableReason.setVisibility(eligibility.reason == 0 ? View.GONE : View.VISIBLE);
        if (eligibility.reason != 0) {
            binding.bookUnavailableReason.setText(eligibility.reason);
        }
    }

    private void openBooking() {
        Court court = viewModel.getCourt().getValue() == null ? null : viewModel.getCourt().getValue().getData();
        if (court == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putString(BookCourtViewModel.ARG_COURT_ID, court.getId());
        NavHostFragment.findNavController(this).navigate(R.id.action_court_detail_to_book_court, args);
    }

    private void renderCourt(UiState<Court> state) {
        UiState.Status status = state.getStatus();
        binding.progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.content.setVisibility(status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);

        AppError error = state.getError();
        if (error != null) {
            boolean notFound = error.getKind() == AppError.Kind.NOT_FOUND;
            // Retrying cannot bring back a court that is inactive or does not exist.
            binding.errorMessage.setText(notFound
                    ? getString(R.string.court_not_found)
                    : ErrorMessages.resolve(requireContext(), error));
            binding.retryButton.setVisibility(notFound ? View.GONE : View.VISIBLE);
        }

        Court court = state.getData();
        if (court == null) {
            return;
        }
        binding.toolbar.setTitle(court.getName());
        binding.courtName.setText(court.getName());
        binding.courtStatus.setText(CourtFormats.statusLabel(court.getStatus()));
        String description = court.getDescription();
        boolean hasDescription = description != null && !description.trim().isEmpty();
        binding.courtDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        binding.courtDescription.setText(hasDescription ? description : null);
        binding.courtAddress.setText(court.getAddress());
        binding.courtCity.setText(court.getCity());
        binding.courtCoordinates.setText(getString(R.string.court_coordinates_value,
                court.getLatitude().toPlainString(), court.getLongitude().toPlainString()));
        binding.hoursTimeZone.setText(getString(R.string.court_hours_time_zone, court.getTimeZoneId()));
    }

    private void renderSports(UiState<List<CourtSport>> state) {
        UiState.Status status = state.getStatus();
        List<CourtSport> sports = state.getData();
        boolean empty = status == UiState.Status.SUCCESS && (sports == null || sports.isEmpty());

        binding.sportsProgress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.sportsErrorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.sportsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.sportsContainer.setVisibility(status == UiState.Status.SUCCESS && !empty ? View.VISIBLE : View.GONE);

        AppError error = state.getError();
        binding.sportsErrorMessage.setText(error == null ? null : ErrorMessages.resolve(requireContext(), error));

        binding.sportsContainer.removeAllViews();
        if (status != UiState.Status.SUCCESS || empty) {
            return;
        }
        Locale locale = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        LayoutInflater inflater = getLayoutInflater();
        for (CourtSport sport : sports) {
            ItemCourtSportBinding row = ItemCourtSportBinding.inflate(inflater, binding.sportsContainer, false);
            row.sportName.setText(sport.getSport().getName());
            row.sportPrice.setText(getString(R.string.court_sport_price,
                    MoneyFormats.amount(sport.getPricePerHour(), sport.getCurrency(), locale), sport.getCurrency()));
            binding.sportsContainer.addView(row.getRoot());
        }
    }

    private void renderHours(UiState<List<OperatingHour>> state) {
        UiState.Status status = state.getStatus();
        List<OperatingHour> hours = state.getData();
        boolean empty = status == UiState.Status.SUCCESS && (hours == null || hours.isEmpty());

        binding.hoursProgress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.hoursErrorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.hoursEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.hoursContainer.setVisibility(status == UiState.Status.SUCCESS && !empty ? View.VISIBLE : View.GONE);
        binding.hoursTimeZone.setVisibility(status == UiState.Status.SUCCESS && !empty ? View.VISIBLE : View.GONE);

        AppError error = state.getError();
        binding.hoursErrorMessage.setText(error == null ? null : ErrorMessages.resolve(requireContext(), error));

        binding.hoursContainer.removeAllViews();
        if (status != UiState.Status.SUCCESS || empty) {
            return;
        }
        LayoutInflater inflater = getLayoutInflater();
        for (CourtFormats.DayHours day : CourtFormats.byDay(hours)) {
            ItemOperatingDayBinding row = ItemOperatingDayBinding.inflate(inflater, binding.hoursContainer, false);
            row.dayName.setText(CourtFormats.dayLabel(day.day));
            row.dayPeriods.setText(periodsText(day.periods));
            binding.hoursContainer.addView(row.getRoot());
        }
    }

    private String periodsText(List<OperatingHour> periods) {
        if (periods.isEmpty()) {
            return getString(R.string.court_hours_closed);
        }
        StringBuilder text = new StringBuilder();
        for (OperatingHour period : periods) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(getString(R.string.court_hours_period, period.getOpensAt(), period.getClosesAt()));
        }
        return text.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

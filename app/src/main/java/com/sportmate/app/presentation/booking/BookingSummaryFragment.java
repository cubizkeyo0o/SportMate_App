package com.sportmate.app.presentation.booking;

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
import com.sportmate.app.databinding.FragmentBookingSummaryBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.MoneyFormats;
import com.sportmate.app.presentation.common.UiState;

import java.text.DateFormat;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/** Reviews the booking and creates it as Pending. */
@AndroidEntryPoint
public final class BookingSummaryFragment extends Fragment {
    private FragmentBookingSummaryBinding binding;
    private BookingSummaryViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(BookingSummaryViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.confirmButton.setOnClickListener(v -> viewModel.confirm());
        binding.changeTimeButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        viewModel.getSummary().observe(getViewLifecycleOwner(), this::renderSummary);
        viewModel.getSubmitting().observe(getViewLifecycleOwner(), submitting -> renderSubmit());
        viewModel.getCreateError().observe(getViewLifecycleOwner(), error -> renderSubmit());
        viewModel.getCreated().observe(getViewLifecycleOwner(), this::openBooking);
    }

    private void renderSummary(UiState<BookingSummary> state) {
        UiState.Status status = state.getStatus();
        binding.progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.content.setVisibility(status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        AppError error = state.getError();
        if (error != null) {
            boolean notFound = error.getKind() == AppError.Kind.NOT_FOUND;
            binding.errorMessage.setText(notFound
                    ? getString(R.string.booking_summary_unavailable)
                    : ErrorMessages.resolve(requireContext(), error));
            binding.retryButton.setVisibility(notFound ? View.GONE : View.VISIBLE);
        }
        BookingSummary summary = state.getData();
        if (summary == null) {
            return;
        }
        Locale locale = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        binding.courtValue.setText(summary.courtName);
        binding.sportValue.setText(summary.sportName);
        binding.dateValue.setText(BookingFormats.date(summary.date, DateFormat.FULL, locale));
        binding.timeValue.setText(getString(R.string.court_hours_period, summary.startTime, summary.endTime));
        binding.durationValue.setText(getResources().getQuantityString(
                R.plurals.booking_duration_minutes, (int) summary.minutes, summary.minutes));
        binding.timeZoneValue.setText(summary.timeZoneId);
        binding.priceValue.setText(getString(R.string.court_sport_price,
                MoneyFormats.amount(summary.pricePerHour, summary.currency, locale), summary.currency));
        binding.estimateValue.setText(getString(R.string.booking_estimated_total,
                MoneyFormats.amount(summary.estimatedTotal, summary.currency, locale), summary.currency));
        renderSubmit();
    }

    private void renderSubmit() {
        boolean submitting = Boolean.TRUE.equals(viewModel.getSubmitting().getValue());
        binding.submitProgress.setVisibility(submitting ? View.VISIBLE : View.GONE);
        // Stays disabled once the booking exists, so it can never be created twice.
        binding.confirmButton.setEnabled(!submitting && !viewModel.isCreated());

        AppError error = viewModel.getCreateError().getValue();
        binding.createError.setVisibility(error == null ? View.GONE : View.VISIBLE);
        binding.createError.setText(error == null ? null : BookingMessages.forCreate(error).resolve(requireContext()));
        boolean timeProblem = error != null
                && (error.getKind() == AppError.Kind.CONFLICT || error.getKind() == AppError.Kind.VALIDATION);
        binding.changeTimeButton.setVisibility(timeProblem ? View.VISIBLE : View.GONE);
    }

    private void openBooking(Event<String> event) {
        String bookingId = event.getContentIfNotHandled();
        if (bookingId == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putString(BookingDetailViewModel.ARG_BOOKING_ID, bookingId);
        NavHostFragment.findNavController(this).navigate(R.id.action_summary_to_booking_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

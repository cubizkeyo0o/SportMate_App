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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.sportmate.app.R;
import com.sportmate.app.databinding.FragmentBookingDetailBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.MoneyFormats;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.common.UiText;

import java.text.DateFormat;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/** One booking: its details as the backend reports them, mock payment and cancellation. */
@AndroidEntryPoint
public final class BookingDetailFragment extends Fragment {
    private FragmentBookingDetailBinding binding;
    private BookingDetailViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(BookingDetailViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.payButton.setOnClickListener(v -> viewModel.pay());
        binding.cancelButton.setOnClickListener(v -> confirmCancel());

        viewModel.getBooking().observe(getViewLifecycleOwner(), this::renderBooking);
        viewModel.getCourtName().observe(getViewLifecycleOwner(), name ->
                binding.courtValue.setText(name == null ? getString(R.string.booking_court_loading) : name.resolve(requireContext())));
        viewModel.getBusy().observe(getViewLifecycleOwner(), busy -> renderActions());
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::showMessage);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    private void renderBooking(UiState<Booking> state) {
        UiState.Status status = state.getStatus();
        binding.progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.content.setVisibility(status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        AppError error = state.getError();
        if (error != null) {
            binding.errorMessage.setText(BookingMessages.forBooking(error).resolve(requireContext()));
            binding.retryButton.setVisibility(error.getKind() == AppError.Kind.NOT_FOUND ? View.GONE : View.VISIBLE);
        }
        Booking booking = state.getData();
        if (booking == null) {
            return;
        }
        Locale locale = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        binding.statusValue.setText(BookingFormats.statusLabel(booking.getStatus()));
        binding.sportValue.setText(booking.getSport().getName());
        binding.dateValue.setText(BookingFormats.date(booking.getLocalDate(), DateFormat.FULL, locale));
        binding.timeValue.setText(getString(R.string.court_hours_period,
                booking.getLocalStartTime(), booking.getLocalEndTime()));
        binding.timeZoneValue.setText(booking.getTimeZoneId());
        binding.priceValue.setText(getString(R.string.booking_total_value,
                MoneyFormats.amount(booking.getTotalPrice(), booking.getCurrency(), locale), booking.getCurrency()));
        binding.bookedAtValue.setText(BookingFormats.courtDateTime(
                booking.getCreatedAtUtcMillis(), booking.getTimeZoneId(), locale));
        binding.referenceValue.setText(booking.getId());

        Long holdExpires = booking.getHoldExpiresAtUtcMillis();
        binding.holdGroup.setVisibility(BookingDetailViewModel.canPay(booking) && holdExpires != null
                ? View.VISIBLE : View.GONE);
        if (holdExpires != null) {
            binding.holdValue.setText(getString(R.string.booking_hold_value,
                    BookingFormats.courtDateTime(holdExpires, booking.getTimeZoneId(), locale)));
        }

        Long cancelledAt = booking.getCancelledAtUtcMillis();
        binding.cancellationGroup.setVisibility(cancelledAt == null ? View.GONE : View.VISIBLE);
        if (cancelledAt != null) {
            binding.cancelledAtValue.setText(BookingFormats.courtDateTime(cancelledAt, booking.getTimeZoneId(), locale));
            String reason = booking.getCancellationReason();
            binding.cancellationReasonValue.setVisibility(reason == null ? View.GONE : View.VISIBLE);
            binding.cancellationReasonValue.setText(reason);
        }
        renderActions();
    }

    private void renderActions() {
        UiState<Booking> state = viewModel.getBooking().getValue();
        Booking booking = state == null ? null : state.getData();
        boolean busy = Boolean.TRUE.equals(viewModel.getBusy().getValue());
        binding.actionProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.payGroup.setVisibility(BookingDetailViewModel.canPay(booking) ? View.VISIBLE : View.GONE);
        binding.cancelButton.setVisibility(BookingDetailViewModel.canCancel(booking) ? View.VISIBLE : View.GONE);
        binding.payButton.setEnabled(!busy);
        binding.cancelButton.setEnabled(!busy);
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.booking_cancel_title)
                .setMessage(R.string.booking_cancel_message)
                .setNegativeButton(R.string.booking_cancel_keep, null)
                .setPositiveButton(R.string.booking_cancel_confirm, (dialog, which) -> viewModel.cancel())
                .show();
    }

    private void showMessage(Event<UiText> event) {
        UiText text = event.getContentIfNotHandled();
        if (text != null) {
            Snackbar.make(binding.getRoot(), text.resolve(requireContext()), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

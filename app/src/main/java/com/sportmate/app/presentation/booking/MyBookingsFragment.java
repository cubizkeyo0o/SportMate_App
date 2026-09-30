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
import com.sportmate.app.databinding.FragmentMyBookingsBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.UiState;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** The signed-in user's upcoming and past bookings. */
@AndroidEntryPoint
public final class MyBookingsFragment extends Fragment implements MyBookingsAdapter.Listener {
    private FragmentMyBookingsBinding binding;
    private MyBookingsViewModel viewModel;
    private MyBookingsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMyBookingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MyBookingsViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        adapter = new MyBookingsAdapter(this, ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0));
        binding.bookingList.setAdapter(adapter);
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        viewModel.getBookings().observe(getViewLifecycleOwner(), this::render);
        viewModel.getCourtNames().observe(getViewLifecycleOwner(), names -> adapter.setCourtNames(names));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Picks up payments or cancellations made on the detail screen.
        viewModel.refresh();
    }

    private void render(UiState<List<Booking>> state) {
        UiState.Status status = state.getStatus();
        List<Booking> bookings = state.getData();
        boolean empty = status == UiState.Status.SUCCESS && (bookings == null || bookings.isEmpty());

        binding.progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.emptyMessage.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.bookingList.setVisibility(status == UiState.Status.SUCCESS && !empty ? View.VISIBLE : View.GONE);

        AppError error = state.getError();
        binding.errorMessage.setText(error == null ? null : ErrorMessages.resolve(requireContext(), error));
        if (status == UiState.Status.SUCCESS && bookings != null) {
            adapter.submitList(BookingGroups.rows(bookings, System.currentTimeMillis()));
        }
    }

    @Override
    public void onBookingSelected(Booking booking) {
        Bundle args = new Bundle();
        args.putString(BookingDetailViewModel.ARG_BOOKING_ID, booking.getId());
        NavHostFragment.findNavController(this).navigate(R.id.action_my_bookings_to_booking_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }
}

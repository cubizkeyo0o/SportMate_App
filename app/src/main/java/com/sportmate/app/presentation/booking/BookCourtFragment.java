package com.sportmate.app.presentation.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ConfigurationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.sportmate.app.R;
import com.sportmate.app.databinding.FragmentBookCourtBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.Weekday;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.MoneyFormats;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.courts.CourtFormats;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Pick a sport, a date and start/end times. Times are the court's local wall-clock times; the
 * court's operating hours for the chosen day are shown to guide the choice.
 */
@AndroidEntryPoint
public final class BookCourtFragment extends Fragment {
    private static final String TAG_DATE = "booking_date_picker";
    private static final String TAG_START = "booking_start_picker";
    private static final String TAG_END = "booking_end_picker";

    private FragmentBookCourtBinding binding;
    private BookCourtViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentBookCourtBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(BookCourtViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.dateButton.setOnClickListener(v -> showDatePicker());
        binding.startButton.setOnClickListener(v -> showTimePicker(TAG_START, R.string.booking_start_time));
        binding.endButton.setOnClickListener(v -> showTimePicker(TAG_END, R.string.booking_end_time));
        binding.continueButton.setOnClickListener(v -> viewModel.continueToSummary());
        binding.sportGroup.setOnCheckedChangeListener((group, checkedId) -> {
            View checked = group.findViewById(checkedId);
            if (checked != null && checked.getTag() instanceof String) {
                viewModel.selectSport((String) checked.getTag());
            }
        });
        reattachPickers();

        viewModel.getOptions().observe(getViewLifecycleOwner(), this::renderOptions);
        viewModel.getSelection().observe(getViewLifecycleOwner(), this::renderSelection);
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            binding.selectionError.setVisibility(error == null ? View.GONE : View.VISIBLE);
            binding.selectionError.setText(error == null ? null : error.resolve(requireContext()));
        });
        viewModel.getShowSummary().observe(getViewLifecycleOwner(), this::openSummary);
    }

    private void renderOptions(UiState<BookCourtViewModel.Options> state) {
        UiState.Status status = state.getStatus();
        binding.progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.content.setVisibility(status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        AppError error = state.getError();
        if (error != null) {
            binding.errorMessage.setText(error.getKind() == AppError.Kind.NOT_FOUND
                    ? getString(R.string.court_not_found)
                    : ErrorMessages.resolve(requireContext(), error));
            binding.retryButton.setVisibility(error.getKind() == AppError.Kind.NOT_FOUND ? View.GONE : View.VISIBLE);
        }
        BookCourtViewModel.Options options = state.getData();
        if (options == null) {
            return;
        }
        binding.toolbar.setSubtitle(options.court.getName());
        binding.timeZoneNote.setText(getString(R.string.booking_time_zone_note, options.court.getTimeZoneId()));
        binding.sportGroup.removeAllViews();
        Locale locale = locale();
        for (CourtSport sport : options.sports) {
            RadioButton button = new RadioButton(requireContext());
            button.setId(View.generateViewId());
            button.setTag(sport.getId());
            button.setText(getString(R.string.booking_sport_option, sport.getSport().getName(),
                    MoneyFormats.amount(sport.getPricePerHour(), sport.getCurrency(), locale), sport.getCurrency()));
            binding.sportGroup.addView(button);
        }
        renderSelection(viewModel.getSelection().getValue());
    }

    private void renderSelection(@Nullable BookingSelection selection) {
        if (selection == null) {
            return;
        }
        // Check the selected sport without re-triggering the listener's selection.
        for (int i = 0; i < binding.sportGroup.getChildCount(); i++) {
            View child = binding.sportGroup.getChildAt(i);
            boolean selected = child.getTag() != null && child.getTag().equals(selection.courtSportId);
            if (selected && binding.sportGroup.getCheckedRadioButtonId() != child.getId()) {
                binding.sportGroup.check(child.getId());
            }
        }
        binding.dateButton.setText(selection.date == null
                ? getString(R.string.booking_choose_date)
                : BookingFormats.date(selection.date, DateFormat.FULL, locale()));
        binding.startButton.setText(selection.startTime == null
                ? getString(R.string.booking_start_time)
                : getString(R.string.booking_start_value, selection.startTime));
        binding.endButton.setText(selection.endTime == null
                ? getString(R.string.booking_end_time)
                : getString(R.string.booking_end_value, selection.endTime));
        binding.dayHours.setVisibility(selection.date == null ? View.GONE : View.VISIBLE);
        if (selection.date != null) {
            binding.dayHours.setText(dayHoursText(selection.date));
        }
    }

    private String dayHoursText(String date) {
        UiState<BookCourtViewModel.Options> state = viewModel.getOptions().getValue();
        if (state == null || state.getData() == null) {
            return "";
        }
        Weekday day = BookingTimes.weekdayOf(date);
        String dayName = getString(CourtFormats.dayLabel(day));
        List<OperatingHour> periods = BookingTimes.periodsOn(state.getData().hours, day);
        if (periods.isEmpty()) {
            return getString(R.string.booking_day_closed, dayName);
        }
        StringBuilder text = new StringBuilder();
        for (OperatingHour period : periods) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(getString(R.string.court_hours_period, period.getOpensAt(), period.getClosesAt()));
        }
        return getString(R.string.booking_day_hours, dayName, text.toString());
    }

    private void showDatePicker() {
        if (getChildFragmentManager().findFragmentByTag(TAG_DATE) != null) {
            return;
        }
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.booking_choose_date)
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.now())
                        .build())
                .build();
        listen(picker);
        picker.show(getChildFragmentManager(), TAG_DATE);
    }

    private void showTimePicker(String tag, int title) {
        if (getChildFragmentManager().findFragmentByTag(tag) != null) {
            return;
        }
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setTitleText(title)
                .build();
        listen(tag, picker);
        picker.show(getChildFragmentManager(), tag);
    }

    // Pickers survive recreation, but their listeners do not.
    @SuppressWarnings("unchecked")
    private void reattachPickers() {
        Fragment date = getChildFragmentManager().findFragmentByTag(TAG_DATE);
        if (date instanceof MaterialDatePicker) {
            listen((MaterialDatePicker<Long>) date);
        }
        for (String tag : new String[] {TAG_START, TAG_END}) {
            Fragment time = getChildFragmentManager().findFragmentByTag(tag);
            if (time instanceof MaterialTimePicker) {
                listen(tag, (MaterialTimePicker) time);
            }
        }
    }

    private void listen(MaterialDatePicker<Long> picker) {
        picker.addOnPositiveButtonClickListener(utcMidnight -> {
            // The picker reports the chosen day as UTC midnight; format it as a plain date.
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            viewModel.selectDate(format.format(new Date(utcMidnight)));
        });
    }

    private void listen(String tag, MaterialTimePicker picker) {
        picker.addOnPositiveButtonClickListener(v -> {
            String time = String.format(Locale.US, "%02d:%02d", picker.getHour(), picker.getMinute());
            if (TAG_START.equals(tag)) {
                viewModel.selectStartTime(time);
            } else {
                viewModel.selectEndTime(time);
            }
        });
    }

    private void openSummary(Event<BookingSelection> event) {
        BookingSelection selection = event.getContentIfNotHandled();
        if (selection == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putString(BookingSummaryViewModel.ARG_COURT_ID, viewModel.getCourtId());
        args.putString(BookingSummaryViewModel.ARG_COURT_SPORT_ID, selection.courtSportId);
        args.putString(BookingSummaryViewModel.ARG_DATE, selection.date);
        args.putString(BookingSummaryViewModel.ARG_START_TIME, selection.startTime);
        args.putString(BookingSummaryViewModel.ARG_END_TIME, selection.endTime);
        NavHostFragment.findNavController(this).navigate(R.id.action_book_court_to_summary, args);
    }

    private Locale locale() {
        return ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

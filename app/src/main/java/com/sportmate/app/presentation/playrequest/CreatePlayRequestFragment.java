package com.sportmate.app.presentation.playrequest;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.sportmate.app.R;
import com.sportmate.app.data.remote.mapper.DtoMapper;
import com.sportmate.app.databinding.FragmentCreatePlayRequestBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.UserSportProfile;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.UiState;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class CreatePlayRequestFragment extends Fragment {
    private FragmentCreatePlayRequestBinding binding;
    private CreatePlayRequestViewModel viewModel;
    private final List<UserSportProfile> profiles = new ArrayList<>();
    private Calendar startsAt;
    private Calendar endsAt;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCreatePlayRequestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(CreatePlayRequestViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.retryButton.setOnClickListener(v -> viewModel.retryProfiles());
        initializeTimeChoices();
        configureSkillOptions();
        binding.startButton.setOnClickListener(v -> chooseDateTime(true));
        binding.endButton.setOnClickListener(v -> chooseDateTime(false));
        binding.createButton.setOnClickListener(v -> submit());
        viewModel.getProfiles().observe(getViewLifecycleOwner(), this::renderProfiles);
        viewModel.getCreated().observe(getViewLifecycleOwner(), this::renderCreateState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void initializeTimeChoices() {
        startsAt = Calendar.getInstance();
        startsAt.add(Calendar.DAY_OF_MONTH, 1);
        startsAt.set(Calendar.HOUR_OF_DAY, 18);
        startsAt.set(Calendar.MINUTE, 0);
        startsAt.set(Calendar.SECOND, 0);
        startsAt.set(Calendar.MILLISECOND, 0);
        endsAt = (Calendar) startsAt.clone();
        endsAt.add(Calendar.HOUR_OF_DAY, 2);
        binding.startButton.setText(formatSelection(startsAt));
        binding.endButton.setText(formatSelection(endsAt));
    }

    private void configureSkillOptions() {
        List<String> minimum = new ArrayList<>();
        minimum.add(getString(R.string.play_choose_any_skill));
        List<String> maximum = new ArrayList<>();
        maximum.add(getString(R.string.play_choose_any_max_skill));
        minimum.addAll(skillLabels());
        maximum.addAll(skillLabels());
        binding.minSkill.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, minimum));
        binding.maxSkill.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, maximum));
        binding.minSkill.setSelection(0);
        binding.maxSkill.setSelection(0);
    }

    private List<String> skillLabels() {
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.skill_beginner));
        labels.add(getString(R.string.skill_elementary));
        labels.add(getString(R.string.skill_intermediate));
        labels.add(getString(R.string.skill_advanced));
        labels.add(getString(R.string.skill_expert));
        return labels;
    }

    private void renderProfiles(UiState<List<UserSportProfile>> state) {
        UiState.Status status = state.getStatus();
        binding.progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.content.setVisibility(status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        AppError error = state.getError();
        binding.errorMessage.setText(error == null ? null : ErrorMessages.resolve(requireContext(), error));
        if (status != UiState.Status.SUCCESS || state.getData() == null) {
            return;
        }

        profiles.clear();
        profiles.addAll(state.getData());
        if (profiles.isEmpty()) {
            binding.formError.setText(R.string.play_create_no_profile);
            binding.formError.setVisibility(View.VISIBLE);
            binding.createButton.setEnabled(false);
        } else {
            binding.formError.setVisibility(View.GONE);
            binding.createButton.setEnabled(true);
        }
        List<String> labels = new ArrayList<>();
        for (UserSportProfile profile : profiles) {
            labels.add(profile.getSport().getName() + " · "
                    + PlayRequestFormats.skillLabel(
                    requireContext(), profile.getSkillLevel(), getString(R.string.play_skill_any)));
        }
        binding.sport.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void renderCreateState(UiState<com.sportmate.app.domain.model.PlayRequest> state) {
        if (state.getStatus() == UiState.Status.LOADING) {
            binding.createButton.setEnabled(false);
            return;
        }
        binding.createButton.setEnabled(!profiles.isEmpty());
        if (state.getStatus() == UiState.Status.ERROR) {
            AppError error = state.getError();
            binding.formError.setText(error == null
                    ? getString(R.string.error_unknown)
                    : ErrorMessages.resolve(requireContext(), error));
            binding.formError.setVisibility(View.VISIBLE);
        } else if (state.getStatus() == UiState.Status.SUCCESS) {
            NavHostFragment.findNavController(this).popBackStack();
        }
    }

    private void chooseDateTime(boolean chooseStart) {
        Calendar selected = chooseStart ? startsAt : endsAt;
        new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, day) -> new TimePickerDialog(
                        requireContext(),
                        (timePicker, hour, minute) -> {
                            Calendar result = (Calendar) selected.clone();
                            result.set(year, month, day, hour, minute, 0);
                            result.set(Calendar.MILLISECOND, 0);
                            if (chooseStart) {
                                startsAt = result;
                                binding.startButton.setText(formatSelection(startsAt));
                            } else {
                                endsAt = result;
                                binding.endButton.setText(formatSelection(endsAt));
                            }
                        },
                        selected.get(Calendar.HOUR_OF_DAY),
                        selected.get(Calendar.MINUTE),
                        true).show(),
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void submit() {
        binding.formError.setVisibility(View.GONE);
        if (profiles.isEmpty() || binding.sport.getSelectedItemPosition() < 0) {
            showFormError(R.string.play_create_no_profile);
            return;
        }
        String title = text(binding.titleInput);
        String spotsText = text(binding.spotsInput);
        BigDecimal latitude = parseDecimal(text(binding.latitudeInput));
        BigDecimal longitude = parseDecimal(text(binding.longitudeInput));
        int spots;
        try {
            spots = Integer.parseInt(spotsText);
        } catch (NumberFormatException exception) {
            showFormError(R.string.play_create_validation);
            return;
        }

        if (title.isEmpty() || title.length() > 150
                || spots < 1 || spots > 255
                || !validCoordinate(latitude, 90) || !validCoordinate(longitude, 180)) {
            showFormError(R.string.play_create_validation);
            return;
        }
        long startMillis = startsAt.getTimeInMillis();
        long endMillis = endsAt.getTimeInMillis();
        if (startMillis <= System.currentTimeMillis()) {
            showFormError(R.string.play_start_in_future);
            return;
        }
        if (endMillis <= startMillis) {
            showFormError(R.string.play_end_after_start);
            return;
        }

        SkillLevel min = selectedSkill(binding.minSkill.getSelectedItemPosition());
        SkillLevel max = selectedSkill(binding.maxSkill.getSelectedItemPosition());
        if (min != null && max != null && min.ordinal() > max.ordinal()) {
            showFormError(R.string.play_create_validation);
            return;
        }

        Sport sport = profiles.get(binding.sport.getSelectedItemPosition()).getSport();
        viewModel.create(
                sport,
                title,
                nullableText(text(binding.descriptionInput)),
                startMillis,
                endMillis,
                spots,
                latitude.doubleValue(),
                longitude.doubleValue(),
                min == null ? null : DtoMapper.toApiValue(min),
                max == null ? null : DtoMapper.toApiValue(max),
                nullableText(text(binding.locationNameInput)));
    }

    @Nullable
    private static SkillLevel selectedSkill(int position) {
        return position <= 0 ? null : SkillLevel.values()[position - 1];
    }

    private static boolean validCoordinate(@Nullable BigDecimal value, int max) {
        return value != null
                && value.compareTo(BigDecimal.valueOf(-max)) >= 0
                && value.compareTo(BigDecimal.valueOf(max)) <= 0
                && value.scale() <= 6;
    }

    @Nullable
    private static BigDecimal parseDecimal(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void showFormError(int message) {
        binding.formError.setText(message);
        binding.formError.setVisibility(View.VISIBLE);
    }

    private String formatSelection(Calendar calendar) {
        DateFormat date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        return date.format(calendar.getTime());
    }

    @Nullable
    private static String nullableText(String value) {
        return value.isEmpty() ? null : value;
    }

    private static String text(android.widget.EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}

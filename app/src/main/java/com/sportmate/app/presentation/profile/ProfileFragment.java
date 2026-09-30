package com.sportmate.app.presentation.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.sportmate.app.R;
import com.sportmate.app.databinding.FragmentProfileBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.model.UserSportProfile;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.FormState;
import com.sportmate.app.presentation.common.FormViews;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.common.UiText;
import com.sportmate.app.presentation.common.UserInputRules;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** The signed-in user's account details and sport profiles. */
@AndroidEntryPoint
public final class ProfileFragment extends Fragment implements SportProfilesAdapter.Listener {
    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private SportProfilesViewModel sportsViewModel;
    private SportProfilesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        sportsViewModel = new ViewModelProvider(this).get(SportProfilesViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        // Account
        binding.accountRetryButton.setOnClickListener(v -> profileViewModel.retry());
        binding.saveButton.setOnClickListener(v -> {
            FormViews.hideKeyboard(binding.getRoot());
            profileViewModel.save(FormViews.text(binding.displayNameInput), FormViews.text(binding.phoneInput));
        });
        FormViews.onUserEdit(binding.displayNameInput,
                () -> profileViewModel.onFieldEdited(UserInputRules.FIELD_DISPLAY_NAME));
        FormViews.onUserEdit(binding.phoneInput, () -> profileViewModel.onFieldEdited(UserInputRules.FIELD_PHONE));
        profileViewModel.getProfile().observe(getViewLifecycleOwner(), this::renderProfile);
        profileViewModel.getForm().observe(getViewLifecycleOwner(), this::renderForm);
        profileViewModel.getFillForm().observe(getViewLifecycleOwner(), this::fillForm);
        profileViewModel.getMessage().observe(getViewLifecycleOwner(), this::showMessage);

        // Sports
        adapter = new SportProfilesAdapter(this);
        binding.sportList.setAdapter(adapter);
        binding.sportsRetryButton.setOnClickListener(v -> sportsViewModel.retry());
        binding.addSportButton.setOnClickListener(v -> showAddSportDialog());
        sportsViewModel.getProfiles().observe(getViewLifecycleOwner(), this::renderSportProfiles);
        sportsViewModel.getAddableSports().observe(getViewLifecycleOwner(), sports -> renderAddButton());
        sportsViewModel.getUpdating().observe(getViewLifecycleOwner(), updating -> {
            binding.sportsUpdatingProgress.setVisibility(Boolean.TRUE.equals(updating) ? View.VISIBLE : View.GONE);
            adapter.setActionsEnabled(!Boolean.TRUE.equals(updating));
            renderAddButton();
        });
        sportsViewModel.getMessage().observe(getViewLifecycleOwner(), this::showMessage);
    }

    private void renderProfile(UiState<User> state) {
        UiState.Status status = state.getStatus();
        binding.accountProgress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.accountError.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.accountRetryButton.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.accountContent.setVisibility(status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);

        AppError error = state.getError();
        binding.accountError.setText(error == null ? null : ErrorMessages.resolve(requireContext(), error));

        User user = state.getData();
        if (user != null) {
            binding.emailValue.setText(user.getEmail());
            binding.roleValue.setText(ProfileLabels.of(user.getRole()));
            binding.statusValue.setText(ProfileLabels.of(user.getStatus()));
        }
    }

    private void renderForm(FormState state) {
        boolean saving = state.isLoading();
        binding.saveProgress.setVisibility(saving ? View.VISIBLE : View.GONE);
        binding.saveButton.setEnabled(!saving);
        binding.displayNameLayout.setEnabled(!saving);
        binding.phoneLayout.setEnabled(!saving);
        FormViews.showFieldError(binding.displayNameLayout, state.getFieldError(UserInputRules.FIELD_DISPLAY_NAME));
        FormViews.showFieldError(binding.phoneLayout, state.getFieldError(UserInputRules.FIELD_PHONE));
        FormViews.showGeneralError(binding.saveError, state.getError());
    }

    // Only on load/save: text the user is typing survives configuration changes on its own.
    private void fillForm(Event<User> event) {
        User user = event.getContentIfNotHandled();
        if (user != null) {
            binding.displayNameInput.setText(user.getDisplayName());
            binding.phoneInput.setText(user.getPhone());
        }
    }

    private void renderSportProfiles(UiState<List<UserSportProfile>> state) {
        UiState.Status status = state.getStatus();
        List<UserSportProfile> profiles = state.getData();
        boolean empty = status == UiState.Status.SUCCESS && (profiles == null || profiles.isEmpty());

        binding.sportsProgress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.sportsError.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.sportsRetryButton.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.sportsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.sportList.setVisibility(status == UiState.Status.SUCCESS && !empty ? View.VISIBLE : View.GONE);

        AppError error = state.getError();
        binding.sportsError.setText(error == null ? null : ErrorMessages.resolve(requireContext(), error));
        if (status == UiState.Status.SUCCESS) {
            adapter.submitList(profiles);
        }
        renderAddButton();
    }

    private void renderAddButton() {
        UiState<List<UserSportProfile>> state = sportsViewModel.getProfiles().getValue();
        List<Sport> addable = sportsViewModel.getAddableSports().getValue();
        boolean loaded = state != null && state.getStatus() == UiState.Status.SUCCESS;
        boolean updating = Boolean.TRUE.equals(sportsViewModel.getUpdating().getValue());
        binding.addSportButton.setVisibility(loaded ? View.VISIBLE : View.GONE);
        binding.addSportButton.setEnabled(!updating && addable != null && !addable.isEmpty());
    }

    private void showMessage(Event<UiText> event) {
        UiText text = event.getContentIfNotHandled();
        if (text != null) {
            Snackbar.make(binding.getRoot(), text.resolve(requireContext()), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEdit(UserSportProfile profile) {
        showSkillLevelDialog(profile.getSport(), profile.getSkillLevel());
    }

    @Override
    public void onRemove(UserSportProfile profile) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sport_profile_remove_title)
                .setMessage(getString(R.string.sport_profile_remove_message, profile.getSport().getName()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_remove,
                        (dialog, which) -> sportsViewModel.remove(profile.getSport().getId()))
                .show();
    }

    private void showAddSportDialog() {
        List<Sport> addable = sportsViewModel.getAddableSports().getValue();
        if (addable == null || addable.isEmpty()) {
            return;
        }
        CharSequence[] names = new CharSequence[addable.size()];
        for (int i = 0; i < addable.size(); i++) {
            names[i] = addable.get(i).getName();
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sport_profile_choose_sport)
                .setItems(names, (dialog, which) -> showSkillLevelDialog(addable.get(which), null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSkillLevelDialog(Sport sport, @Nullable SkillLevel current) {
        SkillLevel[] levels = SkillLevel.values();
        CharSequence[] labels = new CharSequence[levels.length];
        int checked = -1;
        for (int i = 0; i < levels.length; i++) {
            labels[i] = getString(ProfileLabels.of(levels[i]));
            if (levels[i] == current) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.sport_profile_choose_level, sport.getName()))
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (levels[which] != current) {
                        sportsViewModel.setSkillLevel(sport.getId(), levels[which]);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }
}

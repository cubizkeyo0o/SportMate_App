package com.sportmate.app.presentation.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.sportmate.app.R;
import com.sportmate.app.databinding.FragmentRegisterBinding;
import com.sportmate.app.presentation.common.FormState;
import com.sportmate.app.presentation.common.FormViews;
import com.sportmate.app.presentation.common.UserInputRules;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        FormViews.onUserEdit(binding.emailInput, () -> viewModel.onFieldEdited(UserInputRules.FIELD_EMAIL));
        FormViews.onUserEdit(binding.passwordInput, () -> viewModel.onFieldEdited(UserInputRules.FIELD_PASSWORD));
        FormViews.onUserEdit(binding.displayNameInput, () -> viewModel.onFieldEdited(UserInputRules.FIELD_DISPLAY_NAME));
        FormViews.onUserEdit(binding.phoneInput, () -> viewModel.onFieldEdited(UserInputRules.FIELD_PHONE));
        binding.phoneInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        binding.registerButton.setOnClickListener(v -> submit());

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getRegistered().observe(getViewLifecycleOwner(), event -> {
            String email = event.getContentIfNotHandled();
            if (email == null) {
                return;
            }
            Bundle args = new Bundle();
            args.putString(LoginFragment.ARG_EMAIL, email);
            args.putBoolean(LoginFragment.ARG_REGISTERED, true);
            NavHostFragment.findNavController(this).navigate(R.id.action_register_to_login, args);
        });
    }

    private void submit() {
        FormViews.hideKeyboard(binding.getRoot());
        viewModel.register(
                FormViews.text(binding.emailInput),
                FormViews.text(binding.passwordInput),
                FormViews.text(binding.displayNameInput),
                FormViews.text(binding.phoneInput));
    }

    private void render(FormState state) {
        boolean loading = state.isLoading();
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.registerButton.setEnabled(!loading);
        binding.emailLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);
        binding.displayNameLayout.setEnabled(!loading);
        binding.phoneLayout.setEnabled(!loading);

        FormViews.showFieldError(binding.emailLayout, state.getFieldError(UserInputRules.FIELD_EMAIL));
        FormViews.showFieldError(binding.passwordLayout, state.getFieldError(UserInputRules.FIELD_PASSWORD));
        FormViews.showFieldError(binding.displayNameLayout, state.getFieldError(UserInputRules.FIELD_DISPLAY_NAME));
        FormViews.showFieldError(binding.phoneLayout, state.getFieldError(UserInputRules.FIELD_PHONE));
        FormViews.showGeneralError(binding.generalError, state.getError());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

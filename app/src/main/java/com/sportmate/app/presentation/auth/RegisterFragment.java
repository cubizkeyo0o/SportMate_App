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

        AuthForms.onUserEdit(binding.emailInput, () -> viewModel.onFieldEdited(AuthInputRules.FIELD_EMAIL));
        AuthForms.onUserEdit(binding.passwordInput, () -> viewModel.onFieldEdited(AuthInputRules.FIELD_PASSWORD));
        AuthForms.onUserEdit(binding.displayNameInput, () -> viewModel.onFieldEdited(AuthInputRules.FIELD_DISPLAY_NAME));
        AuthForms.onUserEdit(binding.phoneInput, () -> viewModel.onFieldEdited(AuthInputRules.FIELD_PHONE));
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
        AuthForms.hideKeyboard(binding.getRoot());
        viewModel.register(
                AuthForms.text(binding.emailInput),
                AuthForms.text(binding.passwordInput),
                AuthForms.text(binding.displayNameInput),
                AuthForms.text(binding.phoneInput));
    }

    private void render(AuthFormState state) {
        boolean loading = state.isLoading();
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.registerButton.setEnabled(!loading);
        binding.emailLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);
        binding.displayNameLayout.setEnabled(!loading);
        binding.phoneLayout.setEnabled(!loading);

        AuthForms.showFieldError(binding.emailLayout, state.getFieldError(AuthInputRules.FIELD_EMAIL));
        AuthForms.showFieldError(binding.passwordLayout, state.getFieldError(AuthInputRules.FIELD_PASSWORD));
        AuthForms.showFieldError(binding.displayNameLayout, state.getFieldError(AuthInputRules.FIELD_DISPLAY_NAME));
        AuthForms.showFieldError(binding.phoneLayout, state.getFieldError(AuthInputRules.FIELD_PHONE));
        AuthForms.showGeneralError(binding.generalError, state.getError());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

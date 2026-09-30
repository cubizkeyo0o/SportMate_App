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

import com.sportmate.app.databinding.FragmentLoginBinding;
import com.sportmate.app.presentation.common.FormState;
import com.sportmate.app.presentation.common.FormViews;
import com.sportmate.app.presentation.common.UserInputRules;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class LoginFragment extends Fragment {
    /** Optional: email to prefill. */
    public static final String ARG_EMAIL = "email";
    /** Optional: true when arriving from a successful registration. */
    public static final String ARG_REGISTERED = "registered";

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        Bundle args = getArguments();
        boolean registered = args != null && args.getBoolean(ARG_REGISTERED, false);
        binding.registeredMessage.setVisibility(registered ? View.VISIBLE : View.GONE);
        String email = args == null ? null : args.getString(ARG_EMAIL);
        if (savedInstanceState == null && email != null) {
            binding.emailInput.setText(email);
            binding.passwordInput.requestFocus();
        }

        FormViews.onUserEdit(binding.emailInput, () -> viewModel.onFieldEdited(UserInputRules.FIELD_EMAIL));
        FormViews.onUserEdit(binding.passwordInput, () -> viewModel.onFieldEdited(UserInputRules.FIELD_PASSWORD));
        binding.passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        binding.loginButton.setOnClickListener(v -> submit());

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    private void submit() {
        FormViews.hideKeyboard(binding.getRoot());
        viewModel.login(FormViews.text(binding.emailInput), FormViews.text(binding.passwordInput));
    }

    private void render(FormState state) {
        boolean loading = state.isLoading();
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!loading);
        binding.emailLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);

        FormViews.showFieldError(binding.emailLayout, state.getFieldError(UserInputRules.FIELD_EMAIL));
        FormViews.showFieldError(binding.passwordLayout, state.getFieldError(UserInputRules.FIELD_PASSWORD));
        FormViews.showGeneralError(binding.generalError, state.getError());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

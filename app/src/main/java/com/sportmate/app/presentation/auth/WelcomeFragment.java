package com.sportmate.app.presentation.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.sportmate.app.R;
import com.sportmate.app.databinding.FragmentWelcomeBinding;

/** Entry point of the auth flow. */
public final class WelcomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentWelcomeBinding binding = FragmentWelcomeBinding.inflate(inflater, container, false);
        binding.loginButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_welcome_to_login));
        binding.registerButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_welcome_to_register));
        return binding.getRoot();
    }
}

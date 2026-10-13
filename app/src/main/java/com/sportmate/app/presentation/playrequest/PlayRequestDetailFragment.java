package com.sportmate.app.presentation.playrequest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.os.ConfigurationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sportmate.app.R;
import com.sportmate.app.databinding.FragmentPlayRequestDetailBinding;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.ParticipantStatus;
import com.sportmate.app.domain.model.PlayRequest;
import com.sportmate.app.domain.model.PlayRequestParticipant;
import com.sportmate.app.domain.model.PlayRequestStatus;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.UiState;

import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class PlayRequestDetailFragment extends Fragment
        implements PlayRequestParticipantsAdapter.Listener {
    private FragmentPlayRequestDetailBinding binding;
    private PlayRequestDetailViewModel viewModel;
    private PlayRequestParticipantsAdapter participantsAdapter;
    private String requestId;
    private PlayRequest currentRequest;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayRequestDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        requestId = requireArguments().getString(PlayRequestDetailViewModel.ARG_REQUEST_ID);
        viewModel = new ViewModelProvider(this).get(PlayRequestDetailViewModel.class);
        Locale locale = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        participantsAdapter = new PlayRequestParticipantsAdapter(this, locale);
        binding.participantsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.participantsList.setAdapter(participantsAdapter);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.retryButton.setOnClickListener(v -> viewModel.load(requestId));
        binding.joinButton.setOnClickListener(v -> viewModel.performAction(PlayRequestDetailViewModel.Action.JOIN, null));
        binding.withdrawButton.setOnClickListener(v -> viewModel.performAction(PlayRequestDetailViewModel.Action.WITHDRAW, null));
        binding.cancelButton.setOnClickListener(v -> confirmCancel());
        binding.completeButton.setOnClickListener(v -> confirmComplete());
        viewModel.getRequest().observe(getViewLifecycleOwner(), this::renderRequest);
        viewModel.getParticipants().observe(getViewLifecycleOwner(), this::renderParticipants);
        viewModel.getAction().observe(getViewLifecycleOwner(), this::renderAction);
        viewModel.load(requestId);
    }

    @Override
    public void onRespond(PlayRequestParticipant participant, boolean accept) {
        viewModel.performAction(
                accept ? PlayRequestDetailViewModel.Action.ACCEPT : PlayRequestDetailViewModel.Action.REJECT,
                participant.getId());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        participantsAdapter = null;
        currentRequest = null;
    }

    private void renderRequest(UiState<PlayRequest> state) {
        UiState.Status status = state.getStatus();
        binding.progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.content.setVisibility(status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        AppError error = state.getError();
        binding.errorMessage.setText(error == null ? null : ErrorMessages.resolve(requireContext(), error));
        PlayRequest request = state.getData();
        if (status != UiState.Status.SUCCESS || request == null) {
            return;
        }

        currentRequest = request;
        Locale locale = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        binding.title.setText(request.getTitle());
        binding.sport.setText(request.getSportName());
        binding.status.setText(PlayRequestFormats.statusLabel(requireContext(), request.getStatus()));
        binding.when.setText(PlayRequestFormats.when(
                request.getStartsAtUtcMillis(), request.getEndsAtUtcMillis(), locale));
        String hostSkill = PlayRequestFormats.skillLabel(
                requireContext(), request.getHostSkillLevel(), getString(R.string.play_skill_any));
        binding.host.setText(getString(R.string.play_label_host, request.getHostDisplayName(), hostSkill));
        binding.spots.setText(getString(
                R.string.play_label_spots, request.getAcceptedCount(), request.getSpotsNeeded()));
        String minimum = PlayRequestFormats.skillLabel(
                requireContext(), request.getMinSkillLevel(), getString(R.string.play_skill_any));
        String maximum = PlayRequestFormats.skillLabel(
                requireContext(), request.getMaxSkillLevel(), getString(R.string.play_skill_any));
        binding.skillRange.setText(getString(R.string.play_label_skill_range, minimum, maximum));
        String location = request.getLocationName();
        binding.location.setText(location == null || location.trim().isEmpty()
                ? getString(R.string.play_label_no_location)
                : getString(R.string.play_label_location, location));
        binding.description.setText(request.getDescription());
        binding.description.setVisibility(
                request.getDescription() == null || request.getDescription().trim().isEmpty()
                        ? View.GONE : View.VISIBLE);

        boolean host = request.isHost();
        PlayRequestStatus requestStatus = request.getStatus();
        boolean activeRequest = requestStatus == PlayRequestStatus.OPEN || requestStatus == PlayRequestStatus.FULL;
        ParticipantStatus mine = request.getMyParticipantStatus();
        binding.participantsTitle.setVisibility(host ? View.VISIBLE : View.GONE);
        binding.participantsList.setVisibility(host ? View.VISIBLE : View.GONE);
        binding.completeButton.setVisibility(host && activeRequest ? View.VISIBLE : View.GONE);
        binding.cancelButton.setVisibility(host && activeRequest ? View.VISIBLE : View.GONE);
        binding.joinButton.setVisibility(View.GONE);
        binding.withdrawButton.setVisibility(View.GONE);
        binding.myStatus.setVisibility(View.GONE);

        if (!host && activeRequest) {
            if (mine == null || mine == ParticipantStatus.REJECTED || mine == ParticipantStatus.WITHDRAWN) {
                binding.joinButton.setVisibility(
                        requestStatus == PlayRequestStatus.OPEN ? View.VISIBLE : View.GONE);
                binding.joinButton.setText(mine == null
                        ? R.string.play_join_action : R.string.play_join_again_action);
                binding.myStatus.setVisibility(mine == null && requestStatus == PlayRequestStatus.FULL
                        ? View.VISIBLE : View.GONE);
                if (requestStatus == PlayRequestStatus.FULL) {
                    binding.myStatus.setText(R.string.play_host_full_note);
                }
            } else if (mine == ParticipantStatus.PENDING || mine == ParticipantStatus.ACCEPTED) {
                binding.myStatus.setVisibility(View.VISIBLE);
                binding.myStatus.setText(mine == ParticipantStatus.PENDING
                        ? R.string.play_join_pending_note : R.string.play_join_accepted_note);
                binding.withdrawButton.setVisibility(View.VISIBLE);
            }
        }
        participantsAdapter.setRequestStatus(requestStatus);
        participantsAdapter.submitList(request.getParticipants());
    }

    private void renderParticipants(UiState<List<PlayRequestParticipant>> state) {
        if (binding == null || currentRequest == null || !currentRequest.isHost()) {
            return;
        }
        if (state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
            participantsAdapter.submitList(state.getData());
        } else if (state.getStatus() == UiState.Status.ERROR) {
            AppError error = state.getError();
            binding.actionMessage.setText(error == null
                    ? getString(R.string.error_unknown)
                    : ErrorMessages.resolve(requireContext(), error));
            binding.actionMessage.setVisibility(View.VISIBLE);
        }
    }

    private void renderAction(UiState<PlayRequest> state) {
        boolean loading = state.getStatus() == UiState.Status.LOADING;
        binding.joinButton.setEnabled(!loading);
        binding.withdrawButton.setEnabled(!loading);
        binding.completeButton.setEnabled(!loading);
        binding.cancelButton.setEnabled(!loading);
        binding.actionMessage.setVisibility(View.GONE);
        if (state.getStatus() == UiState.Status.SUCCESS) {
            binding.actionMessage.setText(R.string.play_action_saved);
            binding.actionMessage.setVisibility(View.VISIBLE);
        } else if (state.getStatus() == UiState.Status.ERROR) {
            AppError error = state.getError();
            binding.actionMessage.setText(error == null
                    ? getString(R.string.error_unknown)
                    : ErrorMessages.resolve(requireContext(), error));
            binding.actionMessage.setVisibility(View.VISIBLE);
        }
    }

    private void confirmCancel() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.play_confirmation_cancel_title)
                .setMessage(R.string.play_confirmation_cancel_message)
                .setNegativeButton(R.string.play_action_cancel_keep, null)
                .setPositiveButton(R.string.play_action_confirm,
                        (dialog, which) -> viewModel.performAction(PlayRequestDetailViewModel.Action.CANCEL, null))
                .show();
    }

    private void confirmComplete() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.play_confirmation_complete_title)
                .setMessage(R.string.play_confirmation_complete_message)
                .setNegativeButton(R.string.play_action_cancel_keep, null)
                .setPositiveButton(R.string.play_action_confirm,
                        (dialog, which) -> viewModel.performAction(PlayRequestDetailViewModel.Action.COMPLETE, null))
                .show();
    }
}

package com.sportmate.app.presentation.playrequest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sportmate.app.R;
import com.sportmate.app.databinding.ItemPlayRequestParticipantBinding;
import com.sportmate.app.domain.model.ParticipantStatus;
import com.sportmate.app.domain.model.PlayRequestParticipant;
import com.sportmate.app.domain.model.PlayRequestStatus;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

final class PlayRequestParticipantsAdapter
        extends ListAdapter<PlayRequestParticipant, PlayRequestParticipantsAdapter.ViewHolder> {
    interface Listener {
        void onRespond(PlayRequestParticipant participant, boolean accept);
    }

    private static final DiffUtil.ItemCallback<PlayRequestParticipant> DIFF =
            new DiffUtil.ItemCallback<PlayRequestParticipant>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull PlayRequestParticipant oldItem,
                        @NonNull PlayRequestParticipant newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull PlayRequestParticipant oldItem,
                        @NonNull PlayRequestParticipant newItem) {
                    return oldItem.getStatus() == newItem.getStatus();
                }
            };

    private final Listener listener;
    private final Locale locale;
    private PlayRequestStatus requestStatus = PlayRequestStatus.UNKNOWN;

    PlayRequestParticipantsAdapter(Listener listener, Locale locale) {
        super(DIFF);
        this.listener = listener;
        this.locale = locale;
    }

    void setRequestStatus(PlayRequestStatus status) {
        requestStatus = status;
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemPlayRequestParticipantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayRequestParticipant participant = getItem(position);
        ItemPlayRequestParticipantBinding binding = holder.binding;
        String skill = PlayRequestFormats.skillLabel(
                binding.getRoot().getContext(), participant.getSkillLevel(),
                binding.getRoot().getContext().getString(R.string.play_skill_any));
        String requested = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
                .format(new Date(participant.getRequestedAtUtcMillis()));
        binding.name.setText(participant.getDisplayName());
        binding.details.setText(binding.getRoot().getContext().getString(
                R.string.play_participant_skill,
                skill,
                PlayRequestFormats.participantStatusLabel(binding.getRoot().getContext(), participant.getStatus()))
                + " · " + requested);
        boolean canRespond = participant.getStatus() == ParticipantStatus.PENDING
                && (requestStatus == PlayRequestStatus.OPEN || requestStatus == PlayRequestStatus.FULL);
        boolean canAccept = canRespond && requestStatus == PlayRequestStatus.OPEN;
        binding.accept.setVisibility(canRespond ? View.VISIBLE : View.GONE);
        binding.accept.setEnabled(canAccept);
        binding.reject.setVisibility(canRespond ? View.VISIBLE : View.GONE);
        binding.accept.setOnClickListener(v -> listener.onRespond(participant, true));
        binding.reject.setOnClickListener(v -> listener.onRespond(participant, false));
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ItemPlayRequestParticipantBinding binding;

        ViewHolder(ItemPlayRequestParticipantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

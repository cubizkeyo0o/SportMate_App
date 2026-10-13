package com.sportmate.app.presentation.playrequest;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sportmate.app.R;
import com.sportmate.app.databinding.ItemPlayRequestBinding;
import com.sportmate.app.domain.model.PlayRequest;

import java.util.Date;
import java.util.Locale;

final class PlayRequestsAdapter extends ListAdapter<PlayRequest, PlayRequestsAdapter.ViewHolder> {
    interface Listener {
        void onPlayRequestSelected(PlayRequest request);
    }

    private static final DiffUtil.ItemCallback<PlayRequest> DIFF = new DiffUtil.ItemCallback<PlayRequest>() {
        @Override
        public boolean areItemsTheSame(@NonNull PlayRequest oldItem, @NonNull PlayRequest newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull PlayRequest oldItem, @NonNull PlayRequest newItem) {
            return oldItem.getStatus() == newItem.getStatus()
                    && oldItem.getAcceptedCount() == newItem.getAcceptedCount()
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getStartsAtUtcMillis() == newItem.getStartsAtUtcMillis();
        }
    };

    private final Listener listener;
    private final Locale locale;

    PlayRequestsAdapter(Listener listener, Locale locale) {
        super(DIFF);
        this.listener = listener;
        this.locale = locale;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemPlayRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayRequest request = getItem(position);
        ItemPlayRequestBinding binding = holder.binding;
        String status = PlayRequestFormats.statusLabel(binding.getRoot().getContext(), request.getStatus());
        binding.title.setText(request.getTitle());
        binding.sport.setText(request.getSportName());
        binding.when.setText(PlayRequestFormats.when(
                request.getStartsAtUtcMillis(), request.getEndsAtUtcMillis(), locale));
        binding.host.setText(binding.getRoot().getContext().getString(
                R.string.play_card_host, request.getHostDisplayName()));
        binding.spots.setText(binding.getRoot().getContext().getString(
                R.string.play_status_and_count, status, request.getAcceptedCount(), request.getSpotsNeeded()));
        binding.status.setText(request.getMyParticipantStatus() == null
                ? null
                : PlayRequestFormats.participantStatusLabel(
                        binding.getRoot().getContext(), request.getMyParticipantStatus()));
        binding.getRoot().setOnClickListener(v -> listener.onPlayRequestSelected(request));
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ItemPlayRequestBinding binding;

        ViewHolder(ItemPlayRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

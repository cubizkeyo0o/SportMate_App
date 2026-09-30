package com.sportmate.app.presentation.courts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sportmate.app.databinding.ItemCourtBinding;
import com.sportmate.app.domain.model.Court;

import java.util.Objects;

/** Discoverable courts; tapping one opens its details. */
final class CourtsAdapter extends ListAdapter<Court, CourtsAdapter.ViewHolder> {
    interface Listener {
        void onCourtSelected(Court court);
    }

    private static final DiffUtil.ItemCallback<Court> DIFF = new DiffUtil.ItemCallback<Court>() {
        @Override
        public boolean areItemsTheSame(@NonNull Court oldItem, @NonNull Court newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Court oldItem, @NonNull Court newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getCity().equals(newItem.getCity())
                    && oldItem.getAddress().equals(newItem.getAddress())
                    && Objects.equals(oldItem.getDescription(), newItem.getDescription());
        }
    };

    private final Listener listener;

    CourtsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemCourtBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Court court = getItem(position);
        ItemCourtBinding binding = holder.binding;
        binding.courtName.setText(court.getName());
        binding.courtCity.setText(court.getCity());
        binding.courtAddress.setText(court.getAddress());
        String description = court.getDescription();
        boolean hasDescription = description != null && !description.trim().isEmpty();
        binding.courtDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        binding.courtDescription.setText(hasDescription ? description : null);
        binding.getRoot().setOnClickListener(v -> listener.onCourtSelected(court));
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCourtBinding binding;

        ViewHolder(ItemCourtBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

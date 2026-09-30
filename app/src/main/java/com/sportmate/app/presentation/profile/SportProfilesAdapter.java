package com.sportmate.app.presentation.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sportmate.app.databinding.ItemSportProfileBinding;
import com.sportmate.app.domain.model.UserSportProfile;

/** The user's sport profiles; tapping a row changes the skill level, the button removes it. */
final class SportProfilesAdapter extends ListAdapter<UserSportProfile, SportProfilesAdapter.ViewHolder> {
    interface Listener {
        void onEdit(UserSportProfile profile);

        void onRemove(UserSportProfile profile);
    }

    private static final DiffUtil.ItemCallback<UserSportProfile> DIFF = new DiffUtil.ItemCallback<UserSportProfile>() {
        @Override
        public boolean areItemsTheSame(@NonNull UserSportProfile oldItem, @NonNull UserSportProfile newItem) {
            return oldItem.getSport().getId().equals(newItem.getSport().getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserSportProfile oldItem, @NonNull UserSportProfile newItem) {
            return oldItem.getSkillLevel().equals(newItem.getSkillLevel())
                    && oldItem.isSportActive() == newItem.isSportActive()
                    && oldItem.getSport().getName().equals(newItem.getSport().getName());
        }
    };

    private final Listener listener;
    private boolean actionsEnabled = true;

    SportProfilesAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    /** Disables row actions while a change is in flight. */
    void setActionsEnabled(boolean enabled) {
        if (actionsEnabled != enabled) {
            actionsEnabled = enabled;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSportProfileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserSportProfile profile = getItem(position);
        ItemSportProfileBinding binding = holder.binding;
        binding.sportName.setText(profile.getSport().getName());
        binding.skillLevel.setText(ProfileLabels.of(profile.getSkillLevel()));
        binding.inactiveLabel.setVisibility(profile.isSportActive() ? View.GONE : View.VISIBLE);

        binding.getRoot().setEnabled(actionsEnabled);
        binding.removeButton.setEnabled(actionsEnabled);
        binding.getRoot().setOnClickListener(v -> listener.onEdit(profile));
        binding.removeButton.setOnClickListener(v -> listener.onRemove(profile));
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ItemSportProfileBinding binding;

        ViewHolder(ItemSportProfileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

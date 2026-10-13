package com.sportmate.app.presentation.playrequest;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sportmate.app.R;
import com.sportmate.app.domain.model.ParticipantStatus;
import com.sportmate.app.domain.model.PlayRequestStatus;
import com.sportmate.app.domain.model.SkillLevel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class PlayRequestFormats {
    private PlayRequestFormats() {
    }

    @NonNull
    static String when(long startsAtUtcMillis, long endsAtUtcMillis, @NonNull Locale locale) {
        DateFormat date = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        SimpleDateFormat time = new SimpleDateFormat("HH:mm", locale);
        return date.format(new Date(startsAtUtcMillis)) + ", "
                + time.format(new Date(startsAtUtcMillis)) + " – "
                + time.format(new Date(endsAtUtcMillis));
    }

    @NonNull
    static String statusLabel(@NonNull Context context, @NonNull PlayRequestStatus status) {
        switch (status) {
            case OPEN:
                return context.getString(R.string.play_status_open);
            case FULL:
                return context.getString(R.string.play_status_full);
            case CANCELLED:
                return context.getString(R.string.play_status_cancelled);
            case COMPLETED:
                return context.getString(R.string.play_status_completed);
            default:
                return context.getString(R.string.play_status_unknown);
        }
    }

    @NonNull
    static String participantStatusLabel(@NonNull Context context, @NonNull ParticipantStatus status) {
        switch (status) {
            case PENDING:
                return context.getString(R.string.play_participant_pending);
            case ACCEPTED:
                return context.getString(R.string.play_participant_accepted);
            case REJECTED:
                return context.getString(R.string.play_participant_rejected);
            case WITHDRAWN:
                return context.getString(R.string.play_participant_withdrawn);
            default:
                return context.getString(R.string.play_participant_unknown);
        }
    }

    @NonNull
    static String skillLabel(@NonNull Context context, @Nullable SkillLevel level, @NonNull String anyLevel) {
        if (level == null) {
            return anyLevel;
        }
        switch (level) {
            case BEGINNER:
                return context.getString(R.string.skill_beginner);
            case ELEMENTARY:
                return context.getString(R.string.skill_elementary);
            case INTERMEDIATE:
                return context.getString(R.string.skill_intermediate);
            case ADVANCED:
                return context.getString(R.string.skill_advanced);
            case EXPERT:
                return context.getString(R.string.skill_expert);
            default:
                return anyLevel;
        }
    }
}

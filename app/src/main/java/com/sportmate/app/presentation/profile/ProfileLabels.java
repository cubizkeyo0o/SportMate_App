package com.sportmate.app.presentation.profile;

import androidx.annotation.StringRes;

import com.sportmate.app.R;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.UserRole;
import com.sportmate.app.domain.model.UserStatus;

/** Display text for domain enums shown on the profile screen. */
final class ProfileLabels {
    private ProfileLabels() {
    }

    @StringRes
    static int of(SkillLevel level) {
        switch (level) {
            case BEGINNER:
                return R.string.skill_beginner;
            case ELEMENTARY:
                return R.string.skill_elementary;
            case INTERMEDIATE:
                return R.string.skill_intermediate;
            case ADVANCED:
                return R.string.skill_advanced;
            case EXPERT:
                return R.string.skill_expert;
            default:
                throw new IllegalArgumentException("unmapped skill level");
        }
    }

    @StringRes
    static int of(UserRole role) {
        switch (role) {
            case USER:
                return R.string.role_user;
            case COURT_OWNER:
                return R.string.role_court_owner;
            case ADMIN:
                return R.string.role_admin;
            default:
                return R.string.value_unknown;
        }
    }

    @StringRes
    static int of(UserStatus status) {
        switch (status) {
            case ACTIVE:
                return R.string.status_active;
            case SUSPENDED:
                return R.string.status_suspended;
            default:
                return R.string.value_unknown;
        }
    }
}

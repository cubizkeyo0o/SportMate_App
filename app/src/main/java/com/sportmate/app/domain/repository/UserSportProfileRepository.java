package com.sportmate.app.domain.repository;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.UserSportProfile;

import java.util.List;

/** The authenticated user's sport profiles, at most one per sport. */
public interface UserSportProfileRepository {
    /** Ordered by sport name. */
    Cancellable getSportProfiles(ResultCallback<List<UserSportProfile>> callback);

    /**
     * Creates the profile for the sport or changes its skill level, delivering the saved profile.
     * Only active sports can get a new profile.
     */
    Cancellable setSportProfile(String sportId, SkillLevel skillLevel, ResultCallback<UserSportProfile> callback);

    /** Removes the profile; succeeds also when there was none. */
    Cancellable removeSportProfile(String sportId, ResultCallback<Void> callback);
}

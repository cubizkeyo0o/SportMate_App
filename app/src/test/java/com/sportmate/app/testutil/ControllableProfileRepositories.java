package com.sportmate.app.testutil;

import androidx.annotation.Nullable;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.model.UserRole;
import com.sportmate.app.domain.model.UserSportProfile;
import com.sportmate.app.domain.model.UserStatus;
import com.sportmate.app.domain.repository.SportRepository;
import com.sportmate.app.domain.repository.UserRepository;
import com.sportmate.app.domain.repository.UserSportProfileRepository;

import java.util.List;

/**
 * Repositories that record calls and keep each callback so a test can complete it. Used for
 * ViewModel state tests; HTTP behaviour is covered against MockWebServer.
 */
public final class ControllableProfileRepositories {
    private ControllableProfileRepositories() {
    }

    public static final String USER_ID = "4f1c2d7e-8a8b-4d8e-9d52-2b1f0c1a7e11";

    public static User user(String displayName, @Nullable String phone) {
        return new User(USER_ID, "a@example.com", displayName, phone, UserStatus.ACTIVE, UserRole.USER);
    }

    public static Sport sport(String id, String name) {
        return new Sport(id, name, name.toLowerCase());
    }

    public static UserSportProfile profile(String sportId, String sportName, SkillLevel level) {
        return new UserSportProfile(sport(sportId, sportName), true, level);
    }

    public static final class Users implements UserRepository {
        public int getCalls;
        public int updateCalls;
        public int cancelled;
        public String lastDisplayName;
        public String lastPhone;
        public ResultCallback<User> getCallback;
        public ResultCallback<User> updateCallback;

        @Override
        public Cancellable getCurrentUser(ResultCallback<User> callback) {
            getCalls++;
            getCallback = callback;
            return () -> cancelled++;
        }

        @Override
        public Cancellable updateProfile(String displayName, @Nullable String phone, ResultCallback<User> callback) {
            updateCalls++;
            lastDisplayName = displayName;
            lastPhone = phone;
            updateCallback = callback;
            return () -> cancelled++;
        }
    }

    public static final class SportProfiles implements UserSportProfileRepository {
        public int listCalls;
        public int setCalls;
        public int removeCalls;
        public int cancelled;
        public String lastSportId;
        public SkillLevel lastSkillLevel;
        public ResultCallback<List<UserSportProfile>> listCallback;
        public ResultCallback<UserSportProfile> setCallback;
        public ResultCallback<Void> removeCallback;

        @Override
        public Cancellable getSportProfiles(ResultCallback<List<UserSportProfile>> callback) {
            listCalls++;
            listCallback = callback;
            return () -> cancelled++;
        }

        @Override
        public Cancellable setSportProfile(String sportId, SkillLevel skillLevel, ResultCallback<UserSportProfile> callback) {
            setCalls++;
            lastSportId = sportId;
            lastSkillLevel = skillLevel;
            setCallback = callback;
            return () -> cancelled++;
        }

        @Override
        public Cancellable removeSportProfile(String sportId, ResultCallback<Void> callback) {
            removeCalls++;
            lastSportId = sportId;
            removeCallback = callback;
            return () -> cancelled++;
        }
    }

    public static final class Sports implements SportRepository {
        public int calls;
        public int cancelled;
        public ResultCallback<List<Sport>> callback;

        @Override
        public Cancellable getSports(ResultCallback<List<Sport>> callback) {
            calls++;
            this.callback = callback;
            return () -> cancelled++;
        }
    }
}

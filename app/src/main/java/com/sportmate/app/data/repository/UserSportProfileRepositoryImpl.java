package com.sportmate.app.data.repository;

import com.sportmate.app.data.remote.ApiCallExecutor;
import com.sportmate.app.data.remote.api.UserApi;
import com.sportmate.app.data.remote.dto.SetSportProfileRequestDto;
import com.sportmate.app.data.remote.mapper.DtoMapper;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.UserSportProfile;
import com.sportmate.app.domain.repository.UserSportProfileRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class UserSportProfileRepositoryImpl implements UserSportProfileRepository {
    private final UserApi userApi;
    private final ApiCallExecutor executor;

    @Inject
    public UserSportProfileRepositoryImpl(UserApi userApi, ApiCallExecutor executor) {
        this.userApi = userApi;
        this.executor = executor;
    }

    @Override
    public Cancellable getSportProfiles(ResultCallback<List<UserSportProfile>> callback) {
        return executor.execute(userApi.getMySports(), DtoMapper::toUserSportProfiles, callback);
    }

    @Override
    public Cancellable setSportProfile(
            String sportId,
            SkillLevel skillLevel,
            ResultCallback<UserSportProfile> callback) {
        return executor.execute(
                userApi.setMySport(sportId, new SetSportProfileRequestDto(DtoMapper.toApiValue(skillLevel))),
                DtoMapper::toUserSportProfile,
                callback);
    }

    @Override
    public Cancellable removeSportProfile(String sportId, ResultCallback<Void> callback) {
        return executor.executeNoContent(userApi.removeMySport(sportId), callback);
    }
}

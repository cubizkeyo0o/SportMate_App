package com.sportmate.app.data.remote.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sportmate.app.data.remote.dto.SportResponseDto;
import com.sportmate.app.data.remote.dto.UserResponseDto;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.model.UserRole;
import com.sportmate.app.domain.model.UserStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO to domain model mapping. Missing required fields throw {@link IllegalStateException}, which
 * {@link com.sportmate.app.data.remote.ApiCallExecutor} reports as an unreadable response.
 */
public final class DtoMapper {
    private DtoMapper() {
    }

    @NonNull
    public static User toUser(@Nullable UserResponseDto dto) {
        if (dto == null) {
            throw new IllegalStateException("user is missing");
        }
        return new User(
                required(dto.id, "id"),
                required(dto.email, "email"),
                required(dto.displayName, "display_name"),
                dto.phone,
                toUserStatus(dto.status),
                toUserRole(dto.role));
    }

    @NonNull
    public static List<Sport> toSports(@NonNull List<SportResponseDto> dtos) {
        List<Sport> sports = new ArrayList<>(dtos.size());
        for (SportResponseDto dto : dtos) {
            sports.add(toSport(dto));
        }
        return Collections.unmodifiableList(sports);
    }

    @NonNull
    public static Sport toSport(@Nullable SportResponseDto dto) {
        if (dto == null) {
            throw new IllegalStateException("sport is missing");
        }
        return new Sport(
                required(dto.id, "id"),
                required(dto.name, "name"),
                required(dto.slug, "slug"));
    }

    // Role names are the backend's UserRole member names.
    @NonNull
    static UserRole toUserRole(@Nullable String role) {
        if ("User".equals(role)) {
            return UserRole.USER;
        }
        if ("CourtOwner".equals(role)) {
            return UserRole.COURT_OWNER;
        }
        if ("Admin".equals(role)) {
            return UserRole.ADMIN;
        }
        return UserRole.UNKNOWN;
    }

    // Enum values are serialized snake_case by the backend.
    @NonNull
    static UserStatus toUserStatus(@Nullable String status) {
        if ("active".equals(status)) {
            return UserStatus.ACTIVE;
        }
        if ("suspended".equals(status)) {
            return UserStatus.SUSPENDED;
        }
        return UserStatus.UNKNOWN;
    }

    private static String required(@Nullable String value, String field) {
        if (value == null) {
            throw new IllegalStateException(field + " is missing");
        }
        return value;
    }
}

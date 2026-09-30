package com.sportmate.app.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class User {
    private final String id;
    private final String email;
    private final String displayName;
    @Nullable
    private final String phone;
    private final UserStatus status;
    private final UserRole role;

    public User(
            @NonNull String id,
            @NonNull String email,
            @NonNull String displayName,
            @Nullable String phone,
            @NonNull UserStatus status,
            @NonNull UserRole role) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.phone = phone;
        this.status = status;
        this.role = role;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public String getPhone() {
        return phone;
    }

    @NonNull
    public UserStatus getStatus() {
        return status;
    }

    @NonNull
    public UserRole getRole() {
        return role;
    }
}

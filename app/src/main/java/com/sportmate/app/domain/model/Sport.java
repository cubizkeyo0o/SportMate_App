package com.sportmate.app.domain.model;

import androidx.annotation.NonNull;

public final class Sport {
    private final String id;
    private final String name;
    private final String slug;

    public Sport(@NonNull String id, @NonNull String name, @NonNull String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getSlug() {
        return slug;
    }
}

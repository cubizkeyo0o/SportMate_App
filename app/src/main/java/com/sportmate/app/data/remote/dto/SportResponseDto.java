package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Backend {@code SportResponse}. */
public final class SportResponseDto {
    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("slug")
    public String slug;
}

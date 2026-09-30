package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** {@code GET api/v1/health}. */
public final class HealthResponseDto {
    @SerializedName("status")
    public String status;
}

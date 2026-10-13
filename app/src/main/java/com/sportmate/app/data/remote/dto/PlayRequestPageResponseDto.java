package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class PlayRequestPageResponseDto {
    @SerializedName("items")
    public List<PlayRequestResponseDto> items;
    @SerializedName("page")
    public Integer page;
    @SerializedName("page_size")
    public Integer pageSize;
    @SerializedName("total_count")
    public Integer totalCount;
}

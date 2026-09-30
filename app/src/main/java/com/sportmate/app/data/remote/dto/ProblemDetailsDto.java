package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * RFC 7807 error body returned by the backend for 4xx/5xx responses. {@code errors} is only
 * present for 400 validation failures, keyed by snake_case request field name.
 */
public final class ProblemDetailsDto {
    @SerializedName("type")
    public String type;
    @SerializedName("title")
    public String title;
    @SerializedName("status")
    public Integer status;
    @SerializedName("detail")
    public String detail;
    @SerializedName("errors")
    public Map<String, List<String>> errors;
}

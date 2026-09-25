package com.football.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private ApiError error;
    private ApiMeta meta;

    public ApiResponse() {}

    public ApiResponse(T data, ApiMeta meta) {
        this.data = data;
        this.meta = meta;
    }

    public ApiResponse(ApiError error, ApiMeta meta) {
        this.error = error;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> success(T data, long startTimeMs, boolean cached) {
        long executionTimeMs = System.currentTimeMillis() - startTimeMs;
        return new ApiResponse<>(data, new ApiMeta(executionTimeMs, cached, "v1"));
    }

    public static <T> ApiResponse<T> error(String code, String message, long startTimeMs) {
        long executionTimeMs = System.currentTimeMillis() - startTimeMs;
        return new ApiResponse<>(new ApiError(code, message), new ApiMeta(executionTimeMs, false, "v1"));
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }

    public ApiMeta getMeta() {
        return meta;
    }

    public void setMeta(ApiMeta meta) {
        this.meta = meta;
    }
}

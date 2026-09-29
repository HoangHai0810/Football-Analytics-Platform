package com.football.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ApiMeta {
    @JsonProperty("execution_time_ms")
    private double executionTimeMs;

    private boolean cached;

    private String version;

    private String timestamp;

    public ApiMeta() {
        this.timestamp = Instant.now().toString();
    }

    public ApiMeta(double executionTimeMs, boolean cached, String version) {
        this.executionTimeMs = executionTimeMs;
        this.cached = cached;
        this.version = version;
        this.timestamp = Instant.now().toString();
    }

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(double executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

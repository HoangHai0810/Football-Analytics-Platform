package com.football.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class SystemStatusDto {
    private String status;

    @JsonProperty("app_version")
    private String appVersion;

    @JsonProperty("clickhouse_status")
    private String clickhouseStatus;

    @JsonProperty("data_engineering_status")
    private String dataEngineeringStatus;

    @JsonProperty("lakehouse_writer_active")
    private boolean lakehouseWriterActive;

    @JsonProperty("active_source")
    private String activeSource; // "CLICKHOUSE_LIVE" | "HIGH_FIDELITY_SEED_STORE"

    @JsonProperty("entity_counts")
    private Map<String, Long> entityCounts;

    public SystemStatusDto() {}

    public SystemStatusDto(String status, String appVersion, String clickhouseStatus,
                           String dataEngineeringStatus, boolean lakehouseWriterActive,
                           String activeSource, Map<String, Long> entityCounts) {
        this.status = status;
        this.appVersion = appVersion;
        this.clickhouseStatus = clickhouseStatus;
        this.dataEngineeringStatus = dataEngineeringStatus;
        this.lakehouseWriterActive = lakehouseWriterActive;
        this.activeSource = activeSource;
        this.entityCounts = entityCounts;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getClickhouseStatus() { return clickhouseStatus; }
    public void setClickhouseStatus(String clickhouseStatus) { this.clickhouseStatus = clickhouseStatus; }

    public String getDataEngineeringStatus() { return dataEngineeringStatus; }
    public void setDataEngineeringStatus(String dataEngineeringStatus) { this.dataEngineeringStatus = dataEngineeringStatus; }

    public boolean isLakehouseWriterActive() { return lakehouseWriterActive; }
    public void setLakehouseWriterActive(boolean lakehouseWriterActive) { this.lakehouseWriterActive = lakehouseWriterActive; }

    public String getActiveSource() { return activeSource; }
    public void setActiveSource(String activeSource) { this.activeSource = activeSource; }

    public Map<String, Long> getEntityCounts() { return entityCounts; }
    public void setEntityCounts(Map<String, Long> entityCounts) { this.entityCounts = entityCounts; }
}

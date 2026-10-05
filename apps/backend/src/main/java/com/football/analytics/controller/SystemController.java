package com.football.analytics.controller;

import com.football.analytics.dto.ApiResponse;
import com.football.analytics.dto.SystemStatusDto;
import com.football.analytics.repository.PostgresRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class SystemController {
    private final PostgresRepository PostgresRepository;

    public SystemController(PostgresRepository PostgresRepository) {
        this.PostgresRepository = PostgresRepository;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "app", "football-analytics-api-springboot", "version", "1.0.0");
    }

    @GetMapping("/api/v1/system/health")
    public Map<String, Object> apiHealth() {
        return Map.of(
            "status", "ok",
            "app", "football-analytics-api",
            "version", "1.0.0",
            "timestamp", java.time.Instant.now().toString()
        );
    }

    @GetMapping("/api/v1/system/status")
    public ApiResponse<SystemStatusDto> getSystemStatus() {
        return buildStatus();
    }

    @GetMapping("/api/v1/system/info")
    public ApiResponse<SystemStatusDto> getSystemInfo() {
        return buildStatus();
    }

    private ApiResponse<SystemStatusDto> buildStatus() {
        long start = System.currentTimeMillis();
        boolean chOnline = false;
        try {
            chOnline = PostgresRepository.testConnection();
        } catch (Exception ignored) {}

        String chStatus = chOnline
            ? "ONLINE (Connected to ClickHouse OLAP)"
            : "OFFLINE (ClickHouse unreachable â€” no mock fallback)";
        String deStatus = chOnline
            ? "ACTIVE (serving real ingested data)"
            : "BLOCKED (configure CLICKHOUSE_* env vars on Render)";
        String activeSource = chOnline ? "CLICKHOUSE_LIVE" : "NONE";

        Map<String, Long> entityCounts = Collections.emptyMap();
        if (chOnline) {
            try {
                entityCounts = PostgresRepository.getEntityCounts();
            } catch (Exception ignored) {}
        }
        if (entityCounts == null || entityCounts.isEmpty()) {
            entityCounts = Map.of(
                "competitions", 0L,
                "teams", 0L,
                "players", 0L,
                "matches", 0L,
                "shot_events", 0L
            );
        }

        SystemStatusDto statusDto = new SystemStatusDto(
            chOnline ? "HEALTHY" : "DEGRADED",
            "1.0.0 (Spring Boot 3.3.4 + ClickHouse JDBC)",
            chStatus,
            deStatus,
            chOnline,
            activeSource,
            entityCounts
        );

        return ApiResponse.success(statusDto, start, false);
    }
}
